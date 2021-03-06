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

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;

import cito.annotation.Qualifiers;
import cito.event.Message;
import cito.scope.WebSocketContext;
import cito.stomp.Command;
import cito.stomp.Frame;
import cito.stomp.jms.Relay;

/**
 * Unit tests for {@link AbstractEndpoint}.
 * 
 * @author Daniel Siviter
 * @since v1.0 [15 Apr 2017]
 */
@RunWith(MockitoJUnitRunner.class)
public class AbstractEndpointTest {
	@Mock
	protected Logger log;
	@Mock
	private BeanManager beanManager;
	@Mock
	private SessionRegistry registry;
	@Mock
	private Relay relay;
	@Mock
	private Event<Message> messageEvent;
	@Mock
	private Event<Session> sessionEvent;
	@Mock
	private Event<Throwable> errorEvent;
	@Mock
	private Extension extension;
	@Mock
	private WebSocketContext webSocketContext;

	@InjectMocks
	private AbstractEndpoint endpoint = new AbstractEndpoint() { };

	@Before
	public void before() {
		when(this.beanManager.getExtension(Extension.class)).thenReturn(this.extension);
		when(this.extension.webSocketContext()).thenReturn(this.webSocketContext);
	}

	@Test
	public void onOpen() {
		final Session session = mock(Session.class);
		when(session.getId()).thenReturn("sessionId");
		final Map<String, List<String>> paramMap = singletonMap("httpSessionId", singletonList("httpSessionId"));
		when(session.getRequestParameterMap()).thenReturn(paramMap);
		final EndpointConfig config = mock(EndpointConfig.class);
		when(this.sessionEvent.select(Qualifiers.onOpen())).thenReturn(this.sessionEvent);
		when(config.getUserProperties()).thenReturn(new HashMap<>());

		this.endpoint.onOpen(session, config);

		verify(this.registry).register(session);
		verify(session).getRequestParameterMap();
		verify(session).getId();
		verify(session).getUserPrincipal();
		verify(this.log).info("WebSocket connection opened. [id={},httpSessionId={},principle={}]",
				"sessionId",
				"httpSessionId",
				null);
		verify(config).getUserProperties();
		verify(session).getUserProperties();
		verify(this.registry).register(session);
		verify(this.sessionEvent).select(Qualifiers.onOpen());
		verify(this.sessionEvent).fire(session);
		verifyNoMoreInteractions(session, config);
	}

	@Test
	public void message() {
		final Session session = mock(Session.class);
		when(session.getId()).thenReturn("sessionId");
		final Frame frame = mock(Frame.class);
		when(frame.getCommand()).thenReturn(Command.MESSAGE);

		this.endpoint.message(session, frame);

		verify(session).getId();
		verify(session).getUserPrincipal();
		verify(this.log).debug("Received message from client. [id={},principle={},command={}]", "sessionId", null, Command.MESSAGE);
		verify(frame).getCommand();
		verify(this.relay).fromClient(any());
		verify(this.messageEvent).fire(any());
		verifyNoMoreInteractions(session, frame);
	}

	@Test
	public void onError() {
		final Session session = mock(Session.class);
		when(session.getId()).thenReturn("sessionId");
		final Throwable cause = new Throwable();
		when(this.errorEvent.select(Qualifiers.onError())).thenReturn(this.errorEvent);

		this.endpoint.onError(session, cause);

		verify(session).getId();
		verify(session).getUserPrincipal();
		verify(this.log).warn("WebSocket error. [id={},principle={}]", "sessionId", null, cause);
		verify(this.registry).unregister(session);
		verify(this.errorEvent).select(Qualifiers.onError());
		verify(this.errorEvent).fire(cause);
		verifyNoMoreInteractions(session);
	}

	@Test
	public void onClose() {
		final Session session = mock(Session.class);
		when(session.getId()).thenReturn("sessionId");
		final CloseReason reason = new CloseReason(CloseCodes.GOING_AWAY, "oooh");
		when(this.sessionEvent.select(Qualifiers.onClose())).thenReturn(this.sessionEvent);

		this.endpoint.onClose(session, reason);

		verify(session).getId();
		verify(session).getUserPrincipal();
		verify(this.log).info("WebSocket connection closed. [id={},principle={},code={},reason={}]", "sessionId", null, reason.getCloseCode(), reason.getReasonPhrase());
		verify(this.beanManager).getExtension(Extension.class);
		verify(this.registry).unregister(session);
		verify(this.sessionEvent).select(Qualifiers.onClose());
		verify(this.sessionEvent).fire(session);
		verifyNoMoreInteractions(session);
	}

	@After
	public void after() {
		verify(this.extension).webSocketContext();
		verify(this.beanManager).getExtension(Extension.class);
		verifyZeroInteractions(
				this.log,
				this.beanManager,
				this.registry,
				this.relay,
				this.messageEvent,
				this.sessionEvent,
				this.errorEvent,
				this.extension);
	}
}
