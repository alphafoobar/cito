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
package cito.stomp;

import static cito.stomp.Command.CONNECT;
import static cito.stomp.Command.CONNECTED;
import static cito.stomp.Command.DISCONNECT;
import static cito.stomp.Command.RECEIPT;
import static cito.stomp.Command.SEND;
import static cito.stomp.Headers.ACCEPT_VERSION;
import static cito.stomp.Headers.CONTENT_TYPE;
import static cito.stomp.Headers.DESTINATION;
import static cito.stomp.Headers.HOST;
import static cito.stomp.Headers.ID;
import static cito.stomp.Headers.MESSAGE_ID;
import static cito.stomp.Headers.RECEIPT_ID;
import static cito.stomp.Headers.SERVER;
import static cito.stomp.Headers.SESSION;
import static cito.stomp.Headers.SUBSCRIPTION;
import static cito.stomp.Headers.TRANSACTION;
import static cito.stomp.Headers.VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

import cito.collections.LinkedCaseInsensitiveMap;
import cito.collections.UnmodifiableMultivaluedMap;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

/**
 * Defines a STOMP frame
 *
 * @author Daniel Siviter
 * @since v1.0 [12 Jul 2016]
 */
@Immutable
public class Frame {
	private static final AtomicLong MESSAGE_ID_COUNTER = new AtomicLong();

	public static final char NULL = '\u0000';
	static final char LINE_FEED = '\n';
	public static final Frame HEART_BEAT = new Frame(Command.HEARTBEAT, new MultivaluedHashMap<>(0), null);

	private final Command command;
	private final MultivaluedMap<String, String> headers;
	private final ByteBuffer body;

	/**
	 *
	 * @param command
	 * @param headers
	 */
	Frame(@Nonnull Command command, @Nonnull MultivaluedMap<String, String> headers) {
		this(command, headers, null);
	}

	/**
	 *
	 * @param command
	 * @param headers
	 * @param body
	 */
	Frame(@Nonnull Command command, @Nonnull MultivaluedMap<String, String> headers, ByteBuffer body) {
		this.command = requireNonNull(command);
		this.headers = new UnmodifiableMultivaluedMap<>(requireNonNull(headers));
		this.body = body != null ? body.asReadOnlyBuffer() : null;
	}

	/**
	 *
	 * @return
	 */
	public boolean isHeartBeat() {
		return this.command == Command.HEARTBEAT;
	}

	/**
	 *
	 * @return
	 */
	public Command getCommand() {
		return command;
	}

	/**
	 *
	 * @return
	 */
	public MultivaluedMap<String, String> getHeaders() {
		return headers;
	}

	/**
	 *
	 * @return
	 */
	public ByteBuffer getBody() {
		return body;
	}

	/**
	 *
	 * @param key
	 * @return
	 */
	public boolean containsHeader(String key) {
		return getHeaders(key) != null;
	}

	/**
	 *
	 * @param key
	 * @return
	 */
	public List<String> getHeaders(String key) {
		return getHeaders().get(key);
	}

	/**
	 *
	 * @param key
	 * @return
	 */
	public String getFirstHeader(String key) {
		return getHeaders().getFirst(key);
	}

	public String destination() {
		return getFirstHeader(DESTINATION);
	}

	/**
	 *
	 * @return
	 */
	public int contentLength() {
		final String contentLength = getFirstHeader(Headers.CONTENT_LENGTH);
		return contentLength != null ? Integer.parseInt(contentLength) : -1;
	}

	/**
	 *
	 * @return
	 */
	public MediaType contentType() {
		final String contentType = getFirstHeader(Headers.CONTENT_TYPE);
		return contentType != null ? MediaType.valueOf(contentType) : null;
	}

	/**
	 *
	 * @return
	 */
	public int receipt() {
		return Integer.parseInt(getFirstHeader(Headers.RECEIPT));
	}

	/**
	 *
	 * @return
	 */
	public int receiptId() {
		return Integer.parseInt(getFirstHeader(Headers.RECEIPT_ID));
	}

	/**
	 *
	 * @return
	 */
	public String subscription() {
		if (this.command == Command.MESSAGE) { // why is MESSAGE so special?!
			return getFirstHeader(Headers.SUBSCRIPTION);
		}
		return getFirstHeader(ID);
	}

	/**
	 *
	 * @return
	 */
	public HeartBeat heartBeat() {
		final String heartBeat = getFirstHeader(Headers.HEART_BEAT);
		return heartBeat != null ? new HeartBeat(heartBeat) : null;
	}

	/**
	 *
	 * @return
	 */
	public String transaction() {
		return getFirstHeader(TRANSACTION);
	}

	/**
	 *
	 * @return
	 */
	public String session() {
		return getFirstHeader(SESSION);
	}

	/**
	 *
	 * @param writer
	 * @throws IOException
	 */
	public void to(@Nonnull Writer writer) throws IOException {
		if (isHeartBeat()) {
			writer.append(LINE_FEED);
			return;
		}

		writer.append(getCommand().name()).append(LINE_FEED);
		for (Entry<String, List<String>> e : getHeaders().entrySet()) {
			for (String value : e.getValue()) {
				writer.append(e.getKey()).append(':').append(value).append(LINE_FEED);
			}
		}

		writer.append(LINE_FEED);

		if (getBody() != null) {
			writer.append(UTF_8.decode(getBody()));
		}

		writer.append(NULL);
	}

	@Override
	public String toString() {
		try (StringWriter writer = new StringWriter()) {
			to(writer);
			return writer.toString();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}


	// --- Static Methods ---


	/**
	 * Create a {@code Frame} from a {@link String}.
	 *
	 * @param in
	 * @return
	 */
	public static Frame from(@Nonnull String in) {
		try (StringReader reader = new StringReader(in)) {
			return from(reader);
		} catch (IOException e) {
			throw new IllegalArgumentException("String not parsable!", e);
		}
	}

	/**
	 * Create a {@code Frame} from a {@link Reader}.
	 * </p>
	 * <strong>Note:</strong> the caller takes responsibility for closing the {@link Reader}.
	 *
	 * @param in
	 * @return
	 * @throws IOException
	 */
	public static Frame from(@Nonnull Reader in) throws IOException {
		final BufferedReader reader = new BufferedReader(in);

		final String firstLine = reader.readLine();

		if (firstLine.isEmpty()) {
			return HEART_BEAT;
		}

		final Command command = Command.valueOf(firstLine);

		final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(new LinkedCaseInsensitiveMap<>());

		String headerLine;
		while (!(headerLine = reader.readLine()).isEmpty() && !Character.toString(NULL).equals(headerLine)) {
			final String[] tokens = headerLine.split(":");
			List<String> values = headers.get(tokens[0]);
			if (values == null) {
				headers.put(tokens[0], values = new ArrayList<>());
			}
			values.add(tokens[1]);
		}

		final StringBuilder buf = new StringBuilder();
		final char[] arr = new char[8 * 1024];
		int numCharsRead;
		while ((numCharsRead = reader.read(arr, 0, arr.length)) != -1) {
			buf.append(arr, 0, numCharsRead);
		}
		buf.setLength(buf.lastIndexOf(Character.toString(NULL)));
		final ByteBuffer byteBuf = buf.length() == 0 ? null : UTF_8.encode(buf.toString());
		return new Frame(command, headers, byteBuf);
	}

	/**
	 *
	 * @param host
	 * @param acceptVersion
	 * @return
	 */
	public static Builder connect(@Nonnull String host, @Nonnull String... acceptVersion) {
		return builder(CONNECT).header(HOST, host).header(ACCEPT_VERSION, acceptVersion);
	}

	/**
	 *
	 * @return
	 */
	public static Builder disconnect() {
		return builder(DISCONNECT);
	}

	/**
	 *
	 * @return
	 */
	public static Builder error() {
		return builder(Command.ERROR);
	}

	/**
	 *
	 * @param destination
	 * @param subscriptionId
	 * @param messageId
	 * @param contentType
	 * @param body
	 * @return
	 */
	public static Builder message(
			@Nonnull String destination,
			@Nonnull String subscriptionId,
			@Nonnull String messageId,
			MediaType contentType,
			@Nonnull String body)
	{
		return builder(Command.MESSAGE)
				.destination(destination)
				.subscription(subscriptionId)
				.messageId(messageId)
				.body(contentType, body);
	}

	/**
	 *
	 * @param destination
	 * @param contentType
	 * @param body
	 * @return
	 */
	public static Builder send(@Nonnull String destination, MediaType contentType, @Nonnull ByteBuffer body) {
		return builder(SEND).destination(destination).body(contentType, body);
	}

	/**
	 *
	 * @param destination
	 * @param contentType
	 * @param body
	 * @return
	 */
	public static Builder send(@Nonnull String destination, MediaType contentType, @Nonnull String body) {
		return builder(SEND).destination(destination).body(contentType, body);
	}

	/**
	 *
	 * @param version
	 * @param session
	 * @param server
	 * @param heartBeat
	 * @return
	 */
	public static Builder connnected(@Nonnull String version, @Nonnull String session, @Nonnull String server) {
		final Builder builder = builder(CONNECTED).header(VERSION, version);
		builder.header(SESSION, requireNonNull(session));
		builder.header(SERVER, requireNonNull(server));
		return builder;
	}

	/**
	 *
	 * @param id
	 * @param destination
	 * @return
	 */
	public static Builder subscribe(@Nonnull String id, @Nonnull String destination) {
		return builder(Command.SUBSCRIBE).subscription(id).destination(destination);
	}

	/**
	 *
	 * @param receiptId
	 * @return
	 */
	public static Builder receipt(@Nonnull String receiptId) {
		return builder(RECEIPT).header(RECEIPT_ID, receiptId);
	}

	/**
	 *
	 * @param command
	 * @return
	 */
	public static Builder builder(@Nonnull Command command) {
		return new Builder(command);
	}

	/**
	 *
	 * @param frame
	 * @return
	 */
	public static Builder builder(@Nonnull Builder builder) {
		return new Builder(builder);
	}

	/**
	 *
	 * @param frame
	 * @return
	 */
	public static Builder builder(@Nonnull Frame frame) {
		return new Builder(frame);
	}


	// --- Inner Classes ---

	/**
	 * A {@link Frame} builder.
	 *
	 * @author Daniel Siviter
	 * @since v1.0 [15 Jul 2016]
	 */
	public static class Builder {
		private final Command command;
		private final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(new LinkedCaseInsensitiveMap<>());
		private ByteBuffer body;

		/**
		 * Create a {@link Frame} builder from the given {@link Builder}.
		 *
		 * @param builder
		 */
		private Builder(@Nonnull Builder builder) {
			this(builder.command);

			for (Entry<String, List<String>> e : builder.headers.entrySet()) {
				headers.put(e.getKey(),  new ArrayList<>(e.getValue()));
			}
			this.body = builder.body;
		}

		/**
		 * Create a {@link Frame} builder from the given {@link Frame}.
		 *
		 * @param frame
		 */
		private Builder(@Nonnull Frame frame) {
			this(frame.getCommand());

			for (Entry<String, List<String>> e : frame.getHeaders().entrySet()) {
				headers.put(e.getKey(),  new ArrayList<>(e.getValue()));
			}
			this.body = frame.getBody();
		}

		/**
		 * Create a {@link Frame} for the {@link Command}.
		 *
		 * @param command
		 */
		private Builder(@Nonnull Command command) {
			this.command = command;
		}

		/**
		 *
		 * @param key
		 * @param values
		 * @return
		 */
		public Builder header(@Nonnull String key, @Nonnull String... values) {
			if (values == null || values.length == 0)
				throw new IllegalArgumentException("'values' cannot be null or empty!");

			final StringJoiner joiner = new StringJoiner(",");
			for (String v : values) {
				joiner.add(v);
			}

			this.headers.putSingle(key, joiner.toString());
			return this;
		}

		/**
		 *
		 * @param headers
		 * @return
		 */
		public Builder headers(Map<String, String> headers) {
			for (Entry<String, String> e : headers.entrySet()) {
				header(e.getKey(), e.getValue());
			}
			return this;
		}

		/**
		 *
		 * @param destination
		 * @return
		 */
		public Builder destination(@Nonnull String destination) {
			if (!this.command.destination()) {
				throw new IllegalArgumentException(this.command + " does not accept a destination!");
			}
			header(DESTINATION, destination);
			return this;
		}

		/**
		 *
		 * @param messageId
		 * @return
		 */
		public Builder messageId(@Nonnull String messageId) {
			header(MESSAGE_ID, messageId);
			return this;
		}

		/**
		 * Custom Header: send the message to
		 *
		 * @param session
		 * @return
		 */
		public Builder session(@Nonnull String session) {
			header(Headers.SESSION, session);
			return this;
		}

		/**
		 *
		 * @param body
		 * @return
		 * @throws IllegalArgumentException if the command type does not accept a body or {@code body} is {@code null}.
		 */
		public Builder body(MediaType contentType, @Nonnull String body) {
			return body(contentType, UTF_8.encode(requireNonNull(body)));
		}

		/**
		 *
		 * @param contentType
		 * @param body
		 * @return
		 * @throws IllegalArgumentException if the command type does not accept a body or {@code body} is {@code null}.
		 */
		public Builder body(MediaType contentType, @Nonnull ByteBuffer body) {
			if (!this.command.body()) {
				throw new IllegalArgumentException(this.command + " does not accept a body!");
			}
			this.body = requireNonNull(body);
			return contentType == null ? this : header(CONTENT_TYPE, contentType.toString());
		}

		/**
		 *
		 * @param id
		 * @return
		 */
		public Builder subscription(@Nonnull String id) {
			if (!this.command.subscriptionId()) {
				throw new IllegalArgumentException(this.command + " does not accept a subscription!");
			}

			if (this.command == Command.MESSAGE) { // why is MESSAGE so special?!
				header(Headers.SUBSCRIPTION, requireNonNull(id));
			} else {
				header(ID, requireNonNull(id));
			}

			return this;
		}

		/**
		 *
		 * @param outgoing
		 * @param incoming
		 * @return
		 */
		public Builder heartbeat(@Nonnull int outgoing, @Nonnull int incoming) {
			return header(Headers.HEART_BEAT, Integer.toString(outgoing), Integer.toString(incoming));
		}

		/**
		 *
		 * @param versions
		 * @return
		 */
		public Builder version(String... versions) {
			return header(VERSION, versions);
		}

		/**
		 *
		 * @param receiptId
		 * @return
		 */
		public Builder receipt(int receiptId) {
			return header(Headers.RECEIPT, Integer.toString(receiptId));
		}

		/**
		 *
		 * @param receiptId
		 * @return
		 */
		public Builder receiptId(int receiptId) {
			return header(Headers.RECEIPT_ID, Integer.toString(receiptId));
		}

		/**
		 * Derives values from other headers if needed.
		 */
		private void derive() {
			if (!this.headers.containsKey(MESSAGE_ID) && this.command == Command.MESSAGE) {
				String messageId = Long.toString(MESSAGE_ID_COUNTER.getAndIncrement());
				if (this.headers.containsKey(SESSION))
					messageId = this.headers.getFirst(SESSION).concat("-").concat(messageId);
				messageId(messageId);
			}
		}

		/**
		 * Verifies the minimum headers are present.
		 */
		private void verify() {
			switch (this.command) {
			case ACK:
			case NACK:
				assertExists(ID);
				break;
			case BEGIN:
			case COMMIT:
			case ABORT:
				assertExists(TRANSACTION);
				break;
			case CONNECT:
			case STOMP:
				assertExists(ACCEPT_VERSION);
				assertExists(HOST);
				break;
			case CONNECTED:
				assertExists(VERSION);
				break;
			case DISCONNECT:
			case ERROR:
			case HEARTBEAT:
				break;
			case MESSAGE:
				assertExists(DESTINATION);
				assertExists(MESSAGE_ID);
				assertExists(SUBSCRIPTION);
				break;
			case RECEIPT:
				assertExists(RECEIPT_ID);
				break;
			case SEND:
				assertExists(DESTINATION);
				break;
			case SUBSCRIBE:
				assertExists(DESTINATION);
				assertExists(ID);
				break;
			case UNSUBSCRIBE:
				assertExists(ID);
				break;
			}
		}

		/**
		 *
		 * @param key
		 */
		private void assertExists(String key) {
			if (!this.headers.containsKey(key))
				throw new AssertionError("Not set! [" + key + "]");
		}

		/**
		 * @return a newly created {@link Frame}.
		 */
		public Frame build() {
			derive();
			verify();

			final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>(new LinkedCaseInsensitiveMap<>());
			for (Entry<String, List<String>> e : this.headers.entrySet()) {
				headers.put(e.getKey(),  new ArrayList<>(e.getValue()));
			}
			return new Frame(this.command, headers, this.body);
		}
	}

	/**
	 *
	 * @author Daniel Siviter
	 * @since v1.0 [25 Jul 2016]
	 */
	public static class HeartBeat {
		public final long x, y;

		private HeartBeat(String heartBeat) {
			if (heartBeat == null) {
				this.x = 0;
				this.y = 0;
				return;
			}
			final String[] tokens = heartBeat.split(",");
			if (tokens.length != 2)
				throw new IllegalStateException("Invalid number of heart beat elements!");
			this.x = Long.parseLong(tokens[0]);
			this.y = Long.parseLong(tokens[1]);
		}
	}
}
