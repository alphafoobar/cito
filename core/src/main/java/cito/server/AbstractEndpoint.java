/*
 * Copyright 2016-2017 Daniel Siviter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cito.server;

import static cito.server.Extension.webSocketContext;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import org.slf4j.Logger;

import cito.QuietClosable;
import cito.annotation.FromClient;
import cito.annotation.Qualifiers;
import cito.event.ClientMessageProducer;
import cito.event.Message;
import cito.scope.WebSocketContext;
import cito.server.ws.WebSocketConfigurator;
import cito.stomp.Frame;
import cito.stomp.jms.Relay;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [15 Jul 2016]
 */
public abstract class AbstractEndpoint extends Endpoint {
	@Inject
	protected Logger log;
	@Inject
	private BeanManager beanManager;
	@Inject
	private SessionRegistry registry;
	@Inject
	private Relay relay;
	@Inject @FromClient
	private Event<Message> messageEvent;
	@Inject
	private Event<Session> sessionEvent;
	@Inject
	private Event<Throwable> errorEvent;

	@OnOpen
	@Override
	public void onOpen(Session session, EndpointConfig config) {
		final String httpSessionId = session.getRequestParameterMap().get("httpSessionId").get(0);
		this.log.info("WebSocket connection opened. [id={},httpSessionId={},principle={}]",
				session.getId(),
				httpSessionId,
				session.getUserPrincipal());
		final SecurityContext securityCtx = WebSocketConfigurator.removeSecurityContext(config.getUserProperties(), httpSessionId);
		SecurityContextProducer.set(session, securityCtx);
		try (QuietClosable c = webSocketContext(this.beanManager).activate(session)) {
			this.registry.register(session);
			this.sessionEvent.select(Qualifiers.onOpen()).fire(session);
		}
	}

	/**
	 * 
	 * @param session
	 * @param frame
	 */
	@OnMessage
	public void message(Session session, Frame frame) {
		final String sessionId = session.getId();
		this.log.debug("Received message from client. [id={},principle={},command={}]", sessionId, session.getUserPrincipal(), frame.getCommand());
		try (QuietClosable c = webSocketContext(this.beanManager).activate(session)) {
			final Message event = new Message(sessionId, frame);
			try (QuietClosable closable = ClientMessageProducer.set(event)) {
				this.relay.fromClient(event); // due to no @Observe @Priority we need to ensure the relay gets this first
				this.messageEvent.fire(event);
			}
		}
	}

	@OnError
	@Override
	public void onError(Session session, Throwable cause) {
		this.log.warn("WebSocket error. [id={},principle={}]", session.getId(), session.getUserPrincipal(), cause);
		try (QuietClosable c = webSocketContext(this.beanManager).activate(session)) {
			this.registry.unregister(session);
			this.errorEvent.select(Qualifiers.onError()).fire(cause);
		}
	}

	@OnClose
	@Override
	public void onClose(Session session, CloseReason reason) {
		this.log.info("WebSocket connection closed. [id={},principle={},code={},reason={}]", session.getId(), session.getUserPrincipal(), reason.getCloseCode(), reason.getReasonPhrase());
		final WebSocketContext ctx = webSocketContext(this.beanManager);
		try (QuietClosable c = ctx.activate(session)) {
			this.registry.unregister(session);
			this.sessionEvent.select(Qualifiers.onClose()).fire(session);
		}
		ctx.dispose(session);
	}
}
