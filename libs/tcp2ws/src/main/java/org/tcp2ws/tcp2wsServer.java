package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocket;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class tcp2wsServer {

    protected int mPort;
    protected boolean mStopping = false;
    protected String mServer = "";
    protected static boolean tls = false;
    protected static String userAgent = "tcp2ws/1.0.0";
    protected static String connHash = "";
    protected static String host = "";

    static Map<String, String> HostMaps = new HashMap<>();
    protected static Map<Integer, String> mServerList = new HashMap<>();
    static final Map<String, HashSet<WebSocket>> inactiveWs = new HashMap<>();

    public tcp2wsServer setCdnDomain(String config) {
        if (config.contains("#")) {
            mServer = config.split("#")[0];
            host = config.split("#")[1];
        } else {
            mServer = config;
            host = "";
        }

        mServerList.put(1, "pluto." + mServer);
        mServerList.put(2, "venus." + mServer);
        mServerList.put(3, "aurora." + mServer);
        mServerList.put(4, "vesta." + mServer);
        mServerList.put(5, "flora." + mServer);
        mServerList.put(17, "test_pluto." + mServer);
        mServerList.put(18, "test_venus." + mServer);
        mServerList.put(19, "test_aurora." + mServer);

        try {
            //DC1 US
            HostMaps.put("149.154.175.5", "pluto." + mServer);
            HostMaps.put("149.154.175.50", "pluto." + mServer);
            HostMaps.put("149.154.175.51", "pluto." + mServer);
            HostMaps.put("149.154.175.52", "pluto." + mServer);
            HostMaps.put("149.154.175.53", "pluto." + mServer);
            HostMaps.put("149.154.175.55", "pluto." + mServer);
            HostMaps.put("149.154.175.57", "pluto." + mServer);
            HostMaps.put("149.154.175.59", "pluto." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000a").getHostAddress(), "pluto." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000d").getHostAddress(), "pluto." + mServer);
            //DC2 NLD
            HostMaps.put("95.161.76.100", "venus." + mServer);
            HostMaps.put("111.62.91.", "venus." + mServer);
            HostMaps.put("149.154.161.144", "venus." + mServer);
            HostMaps.put("149.154.167.2", "venus." + mServer);
            HostMaps.put("149.154.167.5", "venus." + mServer);
            HostMaps.put("149.154.167.6", "venus." + mServer);
            HostMaps.put("149.154.167.7", "venus." + mServer);
            HostMaps.put("149.154.167.15", "venus." + mServer);
            HostMaps.put("149.154.167.41", "venus." + mServer);
            HostMaps.put("149.154.167.50", "venus." + mServer);
            HostMaps.put("149.154.167.51", "venus." + mServer);
            HostMaps.put("149.154.167.222", "venus." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000a").getHostAddress(), "venus." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000b").getHostAddress(), "venus." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000d").getHostAddress(), "venus." + mServer);
            //DC3 US
            HostMaps.put("149.154.175.100", "aurora." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000a").getHostAddress(), "aurora." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000d").getHostAddress(), "aurora." + mServer);
            //DC4 NLD
            HostMaps.put("5.28.195.", "vesta." + mServer);
            HostMaps.put("91.108.4.", "vesta." + mServer);
            HostMaps.put("149.154.164.", "vesta." + mServer);
            HostMaps.put("149.154.165.", "vesta." + mServer);
            HostMaps.put("149.154.166.", "vesta." + mServer);
            HostMaps.put("149.154.167.8", "vesta." + mServer);
            HostMaps.put("149.154.167.9", "vesta." + mServer);
            HostMaps.put("149.154.167.91", "vesta." + mServer);
            HostMaps.put("149.154.167.92", "vesta." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000a").getHostAddress(), "vesta." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000d").getHostAddress(), "vesta." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f004:0000:0000:0000:000b").getHostAddress(), "vesta." + mServer);
            //DC5 SGP
            HostMaps.put("91.108.56.", "flora." + mServer);
            HostMaps.put("149.154.171.5", "flora." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000a").getHostAddress(), "flora." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23f:f005:0000:0000:0000:000d").getHostAddress(), "flora." + mServer);
            //TEST
            HostMaps.put("149.154.175.10", "test_pluto." + mServer);
            HostMaps.put("149.154.175.40", "test_pluto." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23d:f001:0000:0000:0000:000e").getHostAddress(), "test_pluto." + mServer);
            HostMaps.put("149.154.167.40", "test_venus." + mServer);
            HostMaps.put(InetAddress.getByName("2001:67c:4e8:f002:0000:0000:0000:000e").getHostAddress(), "test_venus." + mServer);
            HostMaps.put("149.154.175.117", "test_aurora." + mServer);
            HostMaps.put(InetAddress.getByName("2001:b28:f23d:f003:0000:0000:0000:000e").getHostAddress(), "test_aurora." + mServer);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        inactiveWs.put(tcp2wsServer.mServerList.get(1), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(2), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(3), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(4), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(5), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(17), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(18), new HashSet<>());
        inactiveWs.put(tcp2wsServer.mServerList.get(19), new HashSet<>());

        return this;
    }

    public tcp2wsServer setUserAgent(String userAgent) {
        tcp2wsServer.userAgent = userAgent;
        return this;
    }

    public tcp2wsServer setConnHash(String connHash) {
        tcp2wsServer.connHash = connHash;
        return this;
    }

    public static void main(String[] args) {

    }

    public tcp2wsServer setTls(boolean tls) {
        tcp2wsServer.tls = tls;
        return this;
    }

    public synchronized void start(int listenPort) {
        if (HostMaps.isEmpty()) {
            throw new RuntimeException("cdn domain not set");
        }
        mStopping = false;
        mPort = listenPort;
        new Thread(new ServerProcess()).start();
    }

    public synchronized void stop() {
        mStopping = true;
    }

    private class ServerProcess implements Runnable {

        @Override
        public void run() {
            try {
                handleClients(mPort);
            } catch (IOException e) {
                Thread.currentThread().interrupt();
            }
        }

        protected void handleClients(int port) throws IOException {
            final ServerSocket listenSocket = new ServerSocket(port);
            listenSocket.setSoTimeout(SocksConstants.LISTEN_TIMEOUT);
            tcp2wsServer.this.mPort = listenSocket.getLocalPort();

            while (true) {
                synchronized (tcp2wsServer.this) {
                    if (mStopping) {
                        break;
                    }
                }
                handleNextClient(listenSocket);
            }

            try {
                listenSocket.close();
            } catch (IOException e) {
                // ignore
            }
        }

        private void handleNextClient(ServerSocket listenSocket) {
            try {
                final Socket clientSocket = listenSocket.accept();
                clientSocket.setSoTimeout(SocksConstants.DEFAULT_SERVER_TIMEOUT);
                new Thread(new ProxyHandler(clientSocket)).start();
            } catch (InterruptedIOException e) {
                //	This exception is thrown when accept timeout is expired
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
