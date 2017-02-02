package cito.server;

import static cito.annotation.Qualifiers.fromServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;

import javax.enterprise.event.Event;
import javax.websocket.Session;
import javax.ws.rs.core.MediaType;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;

import cito.event.MessageEvent;
import cito.ext.Serialiser;
import cito.server.SessionRegistry;
import cito.server.Support;

/**
 * Unit test for {@link Support}.
 * 
 * @author Daniel Siviter
 * @since v1.0 [25 Jul 2016]
 */
@RunWith(MockitoJUnitRunner.class)
public class SupportTest {
	@Mock
	private Logger log;
	@Mock
	private Event<MessageEvent> msgEvent;
	@Mock
	private SessionRegistry registry;
	@Mock
	private Serialiser serialiser;
	@Mock
	private Principal principal;

	@InjectMocks
	private Concrete support;

	@Before
	public void before() {
		when(msgEvent.select(fromServer())).thenReturn(this.msgEvent);
	}

	@Test
	public void broadcast_destination_payload() throws IOException {
		this.support.broadcast("destination", new Object(), Collections.emptyMap());

		final ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
		verify(this.msgEvent).fire(eventCaptor.capture());
		final MessageEvent msgEvent = eventCaptor.getValue();
		assertNull(msgEvent.sessionId());
		assertNull(msgEvent.frame().session());
		assertEquals("destination", msgEvent.frame().destination());
		assertEquals("application/json", msgEvent.frame().contentType().toString());

		verify(this.log).debug("Broadcasting... [destination={}]", "destination");
		verify(this.serialiser).writeTo(any(), any(Class.class), eq(MediaType.APPLICATION_JSON_TYPE), any(OutputStream.class));
	}

	@Test
	public void broadcast_destination_mediaType_payload() throws IOException {
		this.support.broadcast("destination", MediaType.TEXT_PLAIN_TYPE, new Object(), Collections.emptyMap());

		final ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
		verify(this.msgEvent).fire(eventCaptor.capture());
		final MessageEvent msgEvent = eventCaptor.getValue();
		assertNull(msgEvent.sessionId());
		assertNull(msgEvent.frame().session());
		assertEquals("destination", msgEvent.frame().destination());
		assertEquals("text/plain", msgEvent.frame().contentType().toString());


		verify(this.log).debug("Broadcasting... [destination={}]", "destination");
		verify(this.serialiser).writeTo(any(), any(Class.class), eq(MediaType.TEXT_PLAIN_TYPE), any(OutputStream.class));
	}

	@Test
	public void broadcastTo_destination_payload() throws IOException {
		final Session session0 = mock(Session.class);
		when(session0.getId()).thenReturn("session0");
		final Session session1 = mock(Session.class);
		when(session1.getId()).thenReturn("session1");
		when(this.registry.getSessions(principal)).thenReturn(new LinkedHashSet<>(Arrays.asList(session0, session1)));

		this.support.broadcastTo(principal, "destination", new Object(), Collections.emptyMap());

		final ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
		verify(this.msgEvent, times(2)).fire(eventCaptor.capture());

		final MessageEvent msgEvent0 = eventCaptor.getAllValues().get(0);
		assertEquals("session0", msgEvent0.frame().session());
		assertEquals("destination", msgEvent0.frame().destination());
		assertEquals("application/json", msgEvent0.frame().contentType().toString());
		final MessageEvent msgEvent1 = eventCaptor.getAllValues().get(1);
		assertEquals("session1", msgEvent1.frame().session());
		assertEquals("destination", msgEvent1.frame().destination());
		assertEquals("application/json", msgEvent1.frame().contentType().toString());

		verify(this.log).debug("Sending... [sessionId={},destination={}]", "session0", "destination");
		verify(this.log).debug("Sending... [sessionId={},destination={}]", "session1", "destination");
		verify(this.registry).getSessions(principal);
		verify(this.serialiser, times(2)).writeTo(any(), any(Class.class), eq(MediaType.APPLICATION_JSON_TYPE), any(OutputStream.class));
		verify(session0).getId();
		verify(session1).getId();
		verifyNoMoreInteractions(session0, session1);
	}

	@Test
	public void broadcastTo_destination_mediaType_payload() throws IOException {
		final Session session0 = mock(Session.class);
		when(session0.getId()).thenReturn("session0");
		final Session session1 = mock(Session.class);
		when(session1.getId()).thenReturn("session1");
		when(this.registry.getSessions(principal)).thenReturn(new LinkedHashSet<>(Arrays.asList(session0, session1)));

		this.support.broadcastTo(principal, "destination", MediaType.TEXT_PLAIN_TYPE, new Object(), Collections.emptyMap());

		final ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
		verify(this.msgEvent, times(2)).fire(eventCaptor.capture());

		final MessageEvent msgEvent0 = eventCaptor.getAllValues().get(0);
		assertEquals("session0", msgEvent0.frame().session());
		assertEquals("destination", msgEvent0.frame().destination());
		assertEquals("text/plain", msgEvent0.frame().contentType().toString());
		final MessageEvent msgEvent1 = eventCaptor.getAllValues().get(1);
		assertEquals("session1", msgEvent1.frame().session());
		assertEquals("destination", msgEvent1.frame().destination());
		assertEquals("text/plain", msgEvent1.frame().contentType().toString());

		verify(this.log).debug("Sending... [sessionId={},destination={}]", "session0", "destination");
		verify(this.log).debug("Sending... [sessionId={},destination={}]", "session1", "destination");
			verify(this.registry).getSessions(principal);
		verify(this.serialiser, times(2)).writeTo(any(), any(Class.class), eq(MediaType.TEXT_PLAIN_TYPE), any(OutputStream.class));
		verify(session0).getId();
		verify(session1).getId();
		verifyNoMoreInteractions(session0, session1);
	}

	@Test
	public void sendTo_destination_payload() throws IOException {
		this.support.sendTo("sessionId", "destination", new Object(), Collections.emptyMap());

		final ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
		verify(this.msgEvent).fire(eventCaptor.capture());
		final MessageEvent msgEvent = eventCaptor.getValue();
		assertNull(msgEvent.sessionId());
		assertEquals("sessionId", msgEvent.frame().session());
		assertEquals("destination", msgEvent.frame().destination());
		assertEquals("application/json", msgEvent.frame().contentType().toString());

		verify(this.log).debug("Sending... [sessionId={},destination={}]", "sessionId", "destination");
		verify(this.serialiser).writeTo(any(), any(Class.class), eq(MediaType.APPLICATION_JSON_TYPE), any(OutputStream.class));
	}

	@Test
	public void sendTo_destination_mediaType_payload() throws IOException {
		this.support.sendTo("sessionId", "destination", MediaType.TEXT_PLAIN_TYPE, new Object(), Collections.emptyMap());

		final ArgumentCaptor<MessageEvent> eventCaptor = ArgumentCaptor.forClass(MessageEvent.class);
		verify(this.msgEvent).fire(eventCaptor.capture());
		final MessageEvent msgEvent = eventCaptor.getValue();
		assertNull(msgEvent.sessionId());
		assertEquals("sessionId", msgEvent.frame().session());
		assertEquals("destination", msgEvent.frame().destination());
		assertEquals("text/plain", msgEvent.frame().contentType().toString());

		verify(this.log).debug("Sending... [sessionId={},destination={}]", "sessionId", "destination");
		verify(this.serialiser).writeTo(any(), any(Class.class), eq(MediaType.TEXT_PLAIN_TYPE), any(OutputStream.class));
	}

	@After
	public void after() {
		verify(this.msgEvent, atLeastOnce()).select(fromServer());
		verifyNoMoreInteractions(this.log, this.msgEvent, this.registry, this.serialiser, this.principal);
	}


	// --- Inner Classes ---

	/**
	 * 
	 * @author Daniel Siviter
	 * @since v1.0 [31 Oct 2016]
	 */
	private static class Concrete extends Support { }
}