package cito.server.ws;

import javax.websocket.CloseReason;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import cito.server.AbstractServer;
import cito.server.ws.FrameEncoding;
import cito.server.ws.WebSocketConfigurator;
import cito.stomp.Frame;

/**
 * Defines a basic WebSocket endpoint.
 * 
 * @author Daniel Siviter
 * @since v1.0 [15 Jul 2016]
 */
@ServerEndpoint(
		value = "/websocket",
		subprotocols = { "v10.stomp", "v11.stomp", "v12.stomp" },
		encoders = FrameEncoding.class,
		decoders = FrameEncoding.class,
		configurator = WebSocketConfigurator.class
		)
public class WebSocketServer extends AbstractServer {
	@OnOpen
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		super.onOpen(session, config);
	}

	@OnMessage
	@Override
	public void message(Session session, Frame frame) {
		super.message(session, frame);
	}

	@OnClose
	@Override
	public void onClose(Session session, CloseReason reason) {
		super.onClose(session, reason);
	}

	@OnError
	@Override
	public void onError(Session session, Throwable t) {
		super.onError(session, t);
	}
}
