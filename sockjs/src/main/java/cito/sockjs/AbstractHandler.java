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

import static cito.sockjs.Util.servletContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.StringJoiner;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.MessageHandler;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.ArrayUtils;

import cito.sockjs.ServletSession.MessageHandlerWrapper;

/**
 * @author Daniel Siviter
 * @since v1.0 [12 Feb 2017]
 */
public abstract class AbstractHandler implements Serializable {
	private static final long serialVersionUID = 3863326798644998080L;

	protected static final Charset UTF_8 = StandardCharsets.UTF_8;

	protected static final byte[] OPEN_FRAME = "o".getBytes(UTF_8);
	protected static final byte[] HEARDTBEAT_FRAME = "h".getBytes(UTF_8);
	protected static final byte[] ARRAY_FRAME = "a".getBytes(UTF_8);
//	protected static final byte[] CLOSE_FRAME = "c".getBytes(UTF_8);

	protected static final String CORS_ORIGIN = "Access-Control-Allow-Origin";
	protected static final String CORS_CREDENTIALS = "Access-Control-Allow-Credentials";
	protected static final String CORS_REQUEST_HEADERS = "Access-Control-Request-Headers";
	protected static final String CORS_ALLOW_HEADERS = "Access-Control-Allow-Headers";

	protected final Servlet servlet;
	protected final String mediaType;
	protected final String[] methods;

	/**
	 * 
	 * @param servlet
	 * @param mediaType
	 * @param methods
	 */
	public AbstractHandler(Servlet servlet, String mediaType, String... methods) {
		this.servlet = servlet;
		this.mediaType = mediaType;
		this.methods = methods;
	}

	/**
	 * 
	 * @return
	 * @throws ServletException
	 */
	public AbstractHandler init() throws ServletException { 
		return this;
	}

	/**
	 * 
	 * @param asyncCtx
	 * @throws ServletException
	 * @throws IOException
	 */
	public void service(HttpAsyncContext async) throws ServletException, IOException {
		final String method = async.getRequest().getMethod();
		if ("OPTIONS".equals(method)) {
			options(async, ArrayUtils.add(this.methods, "OPTIONS"));
			return;
		}
		if (!ArrayUtils.contains(this.methods, method)) {
			sendErrorNonBlock(async, HttpServletResponse.SC_METHOD_NOT_ALLOWED);
			return;
		}

		async.getResponse().setContentType(this.mediaType);
		setCors(async.getRequest(), async.getResponse());
		setCacheControl(async);

		handle(async);
	}

	/**
	 * 
	 * @param async
	 * @throws ServletException
	 * @throws IOException
	 */
	protected abstract void handle(HttpAsyncContext async)
			throws ServletException, IOException;

	/**
	 * 
	 * @param session
	 * @param text
	 * @throws IOException 
	 */
	@SuppressWarnings("unchecked")
	protected void send(ServletSession session, String text) throws IOException {
		Reader reader = null;

		for (MessageHandlerWrapper h : session.getMessageHandlerWrappers()) {
			if (h.handler() instanceof MessageHandler.Whole) {
				if (h.clazz == String.class) {
					((MessageHandler.Whole<String>) h.handler()).onMessage(text);
					continue;
				}

				if (h.clazz == Reader.class) {
					if (reader == null) {
						reader = new StringReader(text);
					}
					((MessageHandler.Whole<Reader>) h.handler()).onMessage(reader);
					reader.mark(0);
					reader.reset();
					continue;
				}

				if (h.clazz == ByteBuffer.class || h.clazz == InputStream.class) {
					this.servlet.log("Binary types not supported! [" + h.getClass() + "]");
					continue;
				}

				this.servlet.log("Decoder types not supported yet! [" + h.getClass() + "]");
			} else if (h.handler() instanceof MessageHandler.Partial) {
				this.servlet.log("Partial types not supported yet! [" + h.getClass() + "]");
			}
		}
		if (reader != null) {
			reader.close();
		}
	}

	/**
	 * 
	 * @param req
	 * @param res
	 * @param methods
	 */
	protected void options(HttpServletRequest req, HttpServletResponse res, String... methods) {
		res.setStatus(HttpServletResponse.SC_NO_CONTENT);
		res.setHeader("Cache-Control", "public, max-age=31536000"); // one year
		res.setDateHeader("Expires", ZonedDateTime.now(ZoneOffset.UTC.normalized()).plusYears(1).toEpochSecond());
		res.setIntHeader("Access-Control-Max-Age", 1000001);
		final StringJoiner joiner = new StringJoiner(", ");
		Arrays.asList(methods).forEach(joiner::add);
		res.setHeader("Access-Control-Allow-Methods", joiner.toString());
		setCors(req, res);
	}

	/**
	 * 
	 * @param asyncCtx
	 * @param methods
	 */
	protected void options(HttpAsyncContext asyncCtx, String... methods) {
		options(asyncCtx.getRequest(),  asyncCtx.getResponse(), methods);
		asyncCtx.complete();
	}

	/**
	 * 
	 * @param asyncCtx
	 * @param statusCode
	 * @throws IOException
	 */
	protected void sendErrorNonBlock(HttpAsyncContext asyncCtx, int statusCode) throws IOException {
		asyncCtx.getResponse().setStatus(statusCode);
		asyncCtx.complete();
	}

	/**
	 * 
	 * @param async
	 * @param statusCode
	 * @param message
	 */
	protected void sendErrorNonBlock(HttpAsyncContext async, int statusCode, String message) {
		try {
			final HttpServletResponse res = async.getResponse();
			res.setStatus(statusCode);
			final ServletOutputStream os = res.getOutputStream();
			os.setWriteListener(new WriteListener() {
				@Override
				public void onWritePossible() throws IOException {
					os.print(message);
					async.complete();
				}

				@Override
				public void onError(Throwable t) {
					servletContext(async).log("Unable to write error!", t);
					async.complete();
				}
			});
		} catch (IOException e) {
			servletContext(async).log("Unable to write error!", e);
			async.complete();
		}
	}


	/// --- Static Methods ---

	/**
	 * 
	 * @param req
	 * @param res
	 */
	protected static void setCors(HttpServletRequest req, HttpServletResponse res) {
		final String origin = req.getHeader("Origin");
		res.setHeader(CORS_ORIGIN, origin == null ? "*" : origin);
		res.setHeader(CORS_CREDENTIALS, Boolean.TRUE.toString());

		final String headers = req.getHeader(CORS_REQUEST_HEADERS);
		if (headers != null && !headers.isEmpty()) {
			res.setHeader(CORS_ALLOW_HEADERS, headers);
		}
	}

	/**
	 * 
	 * @param res
	 */
	protected static void setCacheControl(HttpServletResponse res) {
		res.setHeader(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate, max-age=0");
	}

	/**
	 * 
	 * @param asyncCtx
	 */
	protected static void setCacheControl(HttpAsyncContext asyncCtx) {
		setCacheControl(asyncCtx.getResponse());
	}

	/**
	 * 
	 * @param code
	 * @param message
	 * @return
	 */
	protected static CharBuffer closeFrame(int code, String message) {
		return CharBuffer.wrap("c[" + code + ",\"" + message + "\"]");
	}
}
