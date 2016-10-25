package cito.stomp.jms;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import javax.enterprise.event.Event;
import javax.enterprise.inject.spi.BeanManager;

import org.apache.logging.log4j.Logger;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import cito.stomp.Command;
import cito.stomp.Frame;
import cito.stomp.Headers;
import cito.stomp.server.event.MessageEvent;

/**
 * Unit test for {@link Relay}.
 * 
 * @author Daniel Siviter
 * @since v1.0 [25 Jul 2016]
 */
@RunWith(MockitoJUnitRunner.class)
public class RelayTest {
	@Mock
	private Logger log;
	@Mock
	private BeanManager beanManager;
	@Mock
	private Event<MessageEvent> messageEvent;

	@InjectMocks
	private Relay relay;

	@Test
	public void message_CONNECT() {
		final MessageEvent msg = new MessageEvent("sessionId", Frame.connect("host", "1.1").build());
		this.relay.message(msg);

	}

	@Test
	public void message_STOMP() {
		final MessageEvent msg = new MessageEvent("sessionId", Frame.builder(Command.STOMP).header(Headers.HOST, "host").header(Headers.ACCEPT_VERSION, "1.1").build());
		this.relay.message(msg);
	}

	@Test
	public void message_DISCONNECT() {
		final MessageEvent msg = new MessageEvent("sessionId", Frame.disconnect().build());
		this.relay.message(msg);
	}

	@Test
	public void close_session() {
		final javax.websocket.Session session = mock(javax.websocket.Session.class);
		this.relay.close(session);

		verifyNoMoreInteractions(session);
	}

	@Test
	public void send() {
		final MessageEvent msg = mock(MessageEvent.class);
		this.relay.send(msg);

		verifyNoMoreInteractions(msg);
	}

	@After
	public void after() {
		verifyNoMoreInteractions(this.log, this.beanManager, this.messageEvent);
	}
}
