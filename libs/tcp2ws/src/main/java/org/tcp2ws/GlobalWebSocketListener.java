package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFrame;

public class GlobalWebSocketListener extends WebSocketAdapter {

    public static final GlobalWebSocketListener INSTANCE = new GlobalWebSocketListener();

    private GlobalWebSocketListener() {
    }

    @Override
    public void onBinaryMessage(WebSocket websocket, byte[] binary) {
        DataReceiver receiver = tcp2wsServer.activeWsHandlers.get(websocket);
        if (receiver != null) {
            receiver.receiveFromServer(binary);
        }
    }

    @Override
    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) {
        DataReceiver receiver = tcp2wsServer.activeWsHandlers.remove(websocket);

        if (receiver != null) {
            receiver.handleServerClose(websocket, closedByServer);
        }
    }
}
