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
package cito.sockjs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.Pipe;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;

import org.apache.commons.lang3.StringEscapeUtils;

import cito.sockjs.nio.WriteStream;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [3 Jan 2017]
 */
public class XhrHandler extends AbstractSessionHandler {
	private static final long serialVersionUID = -527374807374550532L;
	private static final byte[] SEPARATOR = "\n".getBytes(StandardCharsets.UTF_8);
	private static final String CONTENT_TYPE_VALUE = "application/javascript; charset=UTF-8";

	/**
	 * 
	 * @param ctx
	 */
	public XhrHandler(Servlet servlet) {
		super(servlet, CONTENT_TYPE_VALUE, true, "POST");
	}

	@Override
	protected void handle(HttpAsyncContext async, ServletSession session, boolean initial)
	throws ServletException, IOException
	{
		if (initial) {
			final ServletOutputStream out = async.getResponse().getOutputStream();
			out.write(OPEN_FRAME);
			out.write(SEPARATOR);
			async.complete();
			return;
		}

		final Pipe pipe = Pipe.open();
		final WritableByteChannel dest = pipe.sink();
		try (JsonGenerator generator = Json.createGenerator(Channels.newOutputStream(dest))) {
			dest.write(ByteBuffer.wrap(ARRAY_FRAME));
			generator.writeStartArray();
			try (Sender sender = new XhrSender(session, generator)) {
				session.setSender(sender);
			}
			generator.writeEnd();
			generator.flush();
			dest.write(ByteBuffer.wrap(SEPARATOR));
		}

		async.getResponse().getOutputStream().setWriteListener(new WriteStream(async, pipe.source()));
	}


	// --- Inner Classes ---

	/**
	 * 
	 * @author Daniel Siviter
	 * @since v1.0 [18 Feb 2017]
	 */
	private class XhrSender implements Sender {
		private final ServletSession session;
		private final JsonGenerator generator;

		public XhrSender(ServletSession session, JsonGenerator generator) {
			this.session = session;
			this.generator = generator;
		}

		@Override
		public void send(String frame, boolean last) throws IOException {
			this.generator.write(StringEscapeUtils.escapeJson(frame));
		}

		@Override
		public void close() throws IOException {
			this.session.setSender(null);
		}
	}
}
