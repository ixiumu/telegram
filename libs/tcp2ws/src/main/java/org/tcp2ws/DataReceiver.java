package org.tcp2ws;

import com.neovisionaries.ws.client.WebSocket;

public interface DataReceiver {
    void receiveFromServer(byte[] data);
    void handleServerClose(WebSocket websocket, boolean closedByServer);
}
