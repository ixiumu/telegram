package org.tcp2ws;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;

public class Socks4Nat64ProxyHandler implements Runnable {
    private final Socket clientSocket;
    private final byte[] nat64Prefix;
    private final Set<Runnable> externalActiveSet;

    // SOCKS Protocol Constants
    private static final byte SOCKS4_VERSION = 0x04;
    private static final byte SOCKS5_VERSION = 0x05;
    private static final byte SOCKS_CMD_CONNECT = 0x01;
    private static final byte SOCKS5_AUTH_NO_AUTH = 0x00;
    private static final byte SOCKS5_ATYP_IPV4 = 0x01;
    private static final byte SOCKS_REPLY_SUCCESS = 0x5a; // SOCKS4 Success
    private static final byte SOCKS_REPLY_FAILURE = 0x5b; // SOCKS4 Failure

    // SOCKS5 Status Codes
    private static final byte SOCKS5_STATUS_SUCCESS = 0x00;
    private static final byte SOCKS5_STATUS_CMD_UNSUPPORTED = 0x07;
    private static final byte SOCKS5_STATUS_ATYP_UNSUPPORTED = 0x08;
    private static final byte SOCKS5_STATUS_REJECTED = 0x05;


    public Socks4Nat64ProxyHandler(Socket clientSocket, String nat64PrefixString, Set<Runnable> activeSet) {
        this.clientSocket = clientSocket;

        this.externalActiveSet = activeSet;
        this.externalActiveSet.add(this);

        try {
            this.nat64Prefix = parseNat64Prefix(nat64PrefixString);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private byte[] parseNat64Prefix(String prefixString) throws UnknownHostException {
        // Attempt to parse IPv6 address
        InetAddress addr = InetAddress.getByName(prefixString.trim());
        if (!(addr instanceof Inet6Address)) {
            throw new UnknownHostException("Prefix is not a valid IPv6 address: " + prefixString);
        }
        byte[] addressBytes = addr.getAddress();
        if (addressBytes.length != 16) {
            throw new UnknownHostException("IPv6 address is not 16 bytes: " + prefixString);
        }

        // NAT64 translation typically uses /96 prefix (12 fixed bytes, 4 embedded IPv4 bytes)
        // Extract first 12 bytes as NAT64 prefix
        byte[] prefix = new byte[12];
        System.arraycopy(addressBytes, 0, prefix, 0, 12);

        System.out.println("NAT64 Prefix set to: " + prefixString + " (96-bit)");
        return prefix;
    }

    @Override
    public void run() {
        byte[] ipv4Bytes = new byte[4];
        byte[] portBytes = new byte[2];

        try (InputStream clientIn = clientSocket.getInputStream();
            OutputStream clientOut = clientSocket.getOutputStream()) {

            // 1. SOCKS Protocol Negotiation Preparation
            clientSocket.setSoTimeout(5000); // 5 seconds timeout waiting for client to send SOCKS request

            // Read the first byte (SOCKS version)
            int versionByte = clientIn.read();
            if (versionByte == -1) {
                System.err.println("Proxy task error: Client closed socket immediately after connect (zero bytes read).");
                return;
            }
            byte version = (byte)versionByte;

            if (version == SOCKS4_VERSION) {
                // SOCKS4 handling logic
                handleSocks4Connect(clientIn, clientOut, version, ipv4Bytes, portBytes);

            } else if (version == SOCKS5_VERSION) {
                // SOCKS5 handling logic
                handleSocks5Connect(clientIn, clientOut, ipv4Bytes, portBytes);

            } else {
                System.err.println("Proxy task error: Invalid SOCKS protocol header received (Version: " + String.format("0x%02X", version) + ").");
                return;
            }

        } catch (SocketTimeoutException e) {
            // SOCKS Handshake Timeout
            System.err.println("Proxy task error: SOCKS Handshake Read Timeout (5s). Client was too slow or closed.");
        } catch (IOException e) {
            // SOCKS Handshake or Read Error
            System.err.println("Proxy task error (Handshake/Read): " + e.getMessage());
        } finally {
            // targetSocket cleanup happens inside connectAndRelay or on failure
            if (externalActiveSet != null) {
                externalActiveSet.remove(this);
            }
        }
    }

    // --- SOCKS Protocol Handlers ---

    private void handleSocks4Connect(InputStream clientIn, OutputStream clientOut, byte version, byte[] ipv4Bytes, byte[] portBytes) throws IOException {
        // Read the remaining 7 bytes of the SOCKS4 header
        byte[] buffer = new byte[7];
        int bytesRead = clientIn.read(buffer);

        if (bytesRead != 7) {
            System.err.println("Proxy task error: SOCKS4 header truncated.");
            return;
        }

        // SOCKS4 CONNECT Command (0x01)
        if (buffer[0] != SOCKS_CMD_CONNECT) {
            sendSocks4Response(clientOut, SOCKS_REPLY_FAILURE, new byte[4], new byte[2]);
            System.err.println("Proxy task error: Unsupported SOCKS4 command received: " + buffer[0]);
            return;
        }

        // Read DSTPORT (Network Byte Order / Big Endian)
        System.arraycopy(buffer, 1, portBytes, 0, 2);

        // Read DSTIP (4-byte IPv4)
        System.arraycopy(buffer, 3, ipv4Bytes, 0, 4);

        // Read USERID (until 0x00)
        while (clientIn.read() != 0x00) { /* simply read until null terminator or EOF */ }

        // Connect to target and relay data
        connectAndRelay(clientIn, clientOut, ipv4Bytes, portBytes, version);
    }

    private void handleSocks5Connect(InputStream clientIn, OutputStream clientOut, byte[] ipv4Bytes, byte[] portBytes) throws IOException {

        // 1. SOCKS5 Method Negotiation (Version, NMethods)
        int nmethods = clientIn.read();
        if (nmethods <= 0) {
            System.err.println("Proxy task error: SOCKS5 NMETHODS invalid or socket closed.");
            return;
        }

        // 2. Read Methods (Methods[NMethods])
        byte[] methods = new byte[nmethods];
        if (clientIn.read(methods) != nmethods) {
            System.err.println("Proxy task error: SOCKS5 method list truncated.");
            return;
        }

        // 3. Send SOCKS5 Method Response (Version, Selected Method)
        // Only support NO AUTHENTICATION REQUIRED (0x00)
        boolean noAuthSupported = false;
        for (byte method : methods) {
            if (method == SOCKS5_AUTH_NO_AUTH) {
                noAuthSupported = true;
                break;
            }
        }
        if (!noAuthSupported) {
            // Deny all methods
            sendSocks5MethodResponse(clientOut, SOCKS5_VERSION, (byte) 0xFF);
            System.err.println("Proxy task error: SOCKS5 No Auth method not offered.");
            return;
        }
        sendSocks5MethodResponse(clientOut, SOCKS5_VERSION, SOCKS5_AUTH_NO_AUTH);

        // 4. Read SOCKS5 Request (VER, CMD, RSV, ATYP)
        byte[] requestHeader = new byte[4];
        if (clientIn.read(requestHeader) != 4) {
            System.err.println("Proxy task error: SOCKS5 request header truncated.");
            return;
        }

        byte cmd = requestHeader[1];
        byte atyp = requestHeader[3];

        // 5. Check CMD (Must be CONNECT)
        if (cmd != SOCKS_CMD_CONNECT) {
            sendSocks5ConnectResponse(clientOut, SOCKS5_STATUS_CMD_UNSUPPORTED, SOCKS5_ATYP_IPV4, new byte[4], new byte[2]);
            System.err.println("Proxy task error: Unsupported SOCKS5 command: " + cmd);
            return;
        }

        // 6. Check ATYP (Must be IPv4)
        if (atyp != SOCKS5_ATYP_IPV4) {
            // If it is a domain name or IPv6, send an error response
            sendSocks5ConnectResponse(clientOut, SOCKS5_STATUS_ATYP_UNSUPPORTED, SOCKS5_ATYP_IPV4, new byte[4], new byte[2]);
            System.err.println("Proxy task error: Unsupported SOCKS5 ATYP: " + atyp + ". Only IPv4 supported for NAT64.");
            return;
        }

        // 7. Read Address (IPv4: 4 bytes)
        if (clientIn.read(ipv4Bytes) != 4) {
            System.err.println("Proxy task error: SOCKS5 IPv4 address truncated.");
            return;
        }

        // 8. Read Port (2 bytes)
        if (clientIn.read(portBytes) != 2) {
            System.err.println("Proxy task error: SOCKS5 port truncated.");
            return;
        }

        // Connect to target and relay data
        connectAndRelay(clientIn, clientOut, ipv4Bytes, portBytes, SOCKS5_VERSION);
    }

    // --- Core Connection and Relay Logic ---

    private void connectAndRelay(InputStream clientIn, OutputStream clientOut, byte[] ipv4Bytes, byte[] portBytes, byte socksVersion) throws IOException {
        Socket targetSocket = null;

        // 1. IPv4 to IPv6 Address Translation (NAT64)
        byte[] nat64AddressBytes = new byte[16];
        System.arraycopy(nat64Prefix, 0, nat64AddressBytes, 0, 12);
        System.arraycopy(ipv4Bytes, 0, nat64AddressBytes, 12, 4);
        InetAddress nat64Address = InetAddress.getByAddress(null, nat64AddressBytes);

        int port = ByteBuffer.wrap(portBytes).order(ByteOrder.BIG_ENDIAN).getShort() & 0xFFFF;

        System.out.printf("Client request: %d.%d.%d.%d:%d -> NAT64: %s:%d\n",
            ipv4Bytes[0] & 0xFF, ipv4Bytes[1] & 0xFF, ipv4Bytes[2] & 0xFF, ipv4Bytes[3] & 0xFF, port,
            nat64Address.getHostAddress(), port);

        // 2. Connect to Target Server (NAT64 Address)
        try {
            long startTime = System.currentTimeMillis();

            final Socket finalTargetSocket = new Socket(nat64Address, port);
            targetSocket = finalTargetSocket; // Assign to external variable for cleanup in finally block

            long connectTime = System.currentTimeMillis() - startTime;

            // 3. Send SOCKS Success Response
            if (socksVersion == SOCKS4_VERSION) {
                sendSocks4Response(clientOut, SOCKS_REPLY_SUCCESS, ipv4Bytes, portBytes);
            } else { // SOCKS5
                sendSocks5ConnectResponse(clientOut, SOCKS5_STATUS_SUCCESS, SOCKS5_ATYP_IPV4, ipv4Bytes, portBytes);
            }

            System.out.println("DEBUG: Target connection successful. Time taken: " + connectTime + "ms. Starting relay...");

            // 4. Data Relay
            final OutputStream targetOut = finalTargetSocket.getOutputStream();
            final InputStream targetIn = finalTargetSocket.getInputStream();

            // **IMPORTANT: After successful connection, remove clientSocket read timeout to enter the relay loop**
            clientSocket.setSoTimeout(0);

            // Target -> Client (Runs in a separate thread)
            Thread targetToClientThread = new Thread(() -> transferData(targetIn, clientOut, finalTargetSocket, clientSocket));
            targetToClientThread.start();

            // Client -> Target Server (Runs in the main thread)
            transferData(clientIn, targetOut, clientSocket, finalTargetSocket);

            // When the main thread's transferData finishes, force shutdown the client Socket's output
            // to interrupt the other thread's read() blocking on the target socket side.
            try {
                if (!finalTargetSocket.isClosed()) {
                    clientSocket.shutdownOutput();
                }
            } catch (IOException e) { /* ignore */ }

            targetToClientThread.join(100);

            System.out.println("DEBUG: Relay finished for " + nat64Address.getHostAddress() + ". Total duration: " + (System.currentTimeMillis() - startTime) + "ms.");


        } catch (IOException e) {
            // Target connection failed, send SOCKS rejection response
            if (socksVersion == SOCKS4_VERSION) {
                sendSocks4Response(clientOut, SOCKS_REPLY_FAILURE, ipv4Bytes, portBytes);
            } else { // SOCKS5
                sendSocks5ConnectResponse(clientOut, SOCKS5_STATUS_REJECTED, SOCKS5_ATYP_IPV4, ipv4Bytes, portBytes);
            }

            System.err.println("Target connection failed: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Relay interrupted.");
        } finally {
            // Ensure target Socket is closed
            try {
                if (targetSocket != null && !targetSocket.isClosed()) {
                    targetSocket.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }


    // --- SOCKS Response Builders ---

    public void close() {
        try {
            if (clientSocket != null && !clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error while closing client socket externally: " + e.getMessage());
        }
    }

    private void sendSocks4Response(OutputStream out, byte status, byte[] ip, byte[] port) throws IOException {
        ByteBuffer response = ByteBuffer.allocate(8);
        response.put((byte) 0x00);
        response.put(status);
        response.put(port);
        response.put(ip);
        out.write(response.array());
    }

    private void sendSocks5MethodResponse(OutputStream out, byte version, byte method) throws IOException {
        out.write(new byte[]{version, method});
    }

    private void sendSocks5ConnectResponse(OutputStream out, byte status, byte atyp, byte[] bindIp, byte[] bindPort) throws IOException {
        // VER | REP | RSV | ATYP | BND.ADDR | BND.PORT
        ByteBuffer response = ByteBuffer.allocate(4 + 4 + 2); // 10 bytes: Header(4) + IPv4(4) + Port(2)
        response.put(SOCKS5_VERSION);
        response.put(status);
        response.put((byte) 0x00); // RSV
        response.put(atyp);

        // BND.ADDR and BND.PORT are set to 0.0.0.0:0 for simplicity here,
        // using the requested address is technically not required for success.
        response.put(bindIp);
        response.put(bindPort);

        out.write(response.array());
    }

    // --- Data Transfer Logic ---

    // Receive Socket references for bidirectional shutdown coordination
    private void transferData(InputStream in, OutputStream out, Socket sourceSocket, Socket destSocket) {
        byte[] data = new byte[4096];
        int bytesRead;
        try {
            while ((bytesRead = in.read(data)) != -1) {
                out.write(data, 0, bytesRead);
                out.flush();
            }
            // Received EOF (-1), close the source Socket's write end to notify the peer.
            if (!destSocket.isClosed()) {
                destSocket.shutdownOutput();
            }
        } catch (IOException e) {
            // Connection error or interruption; we must force close the entire Socket to terminate the other thread
            try {
                if (!sourceSocket.isClosed()) {
                    sourceSocket.close();
                }
            } catch (IOException ignored) {}
            try {
                if (!destSocket.isClosed()) {
                    destSocket.close();
                }
            } catch (IOException ignored) {}
        }
    }
}
