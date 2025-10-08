package ttt_backend;

import java.util.logging.Level;
import java.util.logging.Logger;

import io.vertx.core.http.ServerWebSocket;

public class WebSocketEventListener implements EventListenerInterface {

    static Logger logger = Logger.getLogger("WebSocketEventListener");
    private final ServerWebSocket webSocket;

    public WebSocketEventListener(ServerWebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public void onEvent(String eventData) {
        logger.log(Level.INFO, "Sending event to WebSocket: " + eventData);
        webSocket.writeTextMessage(eventData);
    }
}
