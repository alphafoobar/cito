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

import static cito.sockjs.EventSourceHandler.EVENTSOURCE;
import static cito.sockjs.GreetingHandler.GREETING;
import static cito.sockjs.HtmlFileHandler.HTMLFILE;
import static cito.sockjs.IFrameHandler.IFRAME;
import static cito.sockjs.InfoHandler.INFO;
import static cito.sockjs.JsonPHandler.JSONP;
import static cito.sockjs.JsonPSendHandler.JSONP_SEND;
import static cito.sockjs.XhrHandler.XHR;
import static cito.sockjs.XhrSendHandler.XHR_SEND;
import static cito.sockjs.XhrStreamingHandler.XHR_STREAMING;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The link between SockJS and the servlet container. See {@link Config} for usage.
 *
 * @author Daniel Siviter
 * @since v1.0 [4 Jan 2017]
 */
@SuppressFBWarnings(
	value = "NM_SAME_SIMPLE_NAME_AS_INTERFACE",
	justification = "TODO: Think of a better name."
)
public class Servlet implements javax.servlet.Servlet {
	private final Map<String, AbstractHandler> handlers = new HashMap<>();
	private final Map<String, ServletSession> sessions = new ConcurrentHashMap<>();
	// XXX Should I use ManagedScheduledExecutorService?
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

	private final Logger log = LoggerFactory.getLogger(Servlet.class);
	private final Config config;

	private ServletConfig servletConfig;
	private boolean webSocketSupported;

	protected Servlet(Config config) {
		this.config = config;
	}

	@Override
	public void init(ServletConfig servletConfig) throws ServletException {
		this.servletConfig = servletConfig;
		this.handlers.put(GREETING, new GreetingHandler(this).init());
		this.handlers.put(IFRAME, new IFrameHandler(this).init());
		this.handlers.put(INFO, new InfoHandler(this).init());
		this.handlers.put(XHR, new XhrHandler(this).init());
		this.handlers.put(XHR_SEND, new XhrSendHandler(this).init());
		this.handlers.put(XHR_STREAMING, new XhrStreamingHandler(this).init());
		this.handlers.put(EVENTSOURCE, new EventSourceHandler(this).init());
		this.handlers.put(HTMLFILE, new HtmlFileHandler(this).init());
		this.handlers.put(JSONP, new JsonPHandler(this).init());
		this.handlers.put(JSONP_SEND, new JsonPSendHandler(this).init());

		this.scheduler.scheduleWithFixedDelay(this::cleanupSessions, 5, 5, TimeUnit.SECONDS);
	}

	@Override
	public ServletConfig getServletConfig() {
		return this.servletConfig;
	}

	@Override
	public String getServletInfo() {
		return "Citō SockJS";
	}

	/**
	 * @return the config
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * @return the webSocketSupported
	 */
	public boolean isWebSocketSupported() {
		return webSocketSupported;
	}

	/**
	 *
	 * @param supported
	 */
	void setWebSocketSupported(boolean supported) {
		this.webSocketSupported = supported;
	}

	@Override
	public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		if (!(req instanceof HttpServletRequest && res instanceof HttpServletResponse)) {
			throw new ServletException("non-HTTP request or response");
		}
		service((HttpServletRequest) req, (HttpServletResponse) res);
	}

	/**
	 *
	 * @param req
	 * @param res
	 * @throws ServletException
	 * @throws IOException
	 */
	private void service(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
		this.log.info("SockJS request received. [name={},path={},method={}]", this.config.name(), req.getRequestURI(), req.getMethod());

		String type = resolveType(req);

		final AbstractHandler handler = this.handlers.get(type);
		if (handler == null) {
			this.log.warn("Invalid path sent to SockJS! [name={},path={},method={}]", this.config.name(), req.getRequestURI(), req.getMethod());
			res.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		final AsyncContext asyncContext = req.startAsync();
		asyncContext.setTimeout(TimeUnit.MINUTES.toMillis(1));
		asyncContext.start(() -> onRequest(handler, asyncContext));
	}

	private String resolveType(HttpServletRequest req) {
		if (req.getPathTranslated() == null) {
			return GREETING;
		}

		final String[] segments = req.getPathInfo().substring(1).split("/"); // strip leading '/'
		if (segments.length == 1) {
			String type = segments[0].toLowerCase(Locale.ENGLISH);
			if (type.startsWith(IFRAME)) {
				return IFRAME; // special case, avoids a regex or similar
			}
			return type;
		} else if (segments.length == 3) {
			return segments[2];
		}
		return null;
	}

	/**
	 *
	 * @param req
	 * @return
	 * @throws ServletException
	 */
	protected ServletSession getSession(HttpServletRequest req) throws ServletException {
		final String sessionId = Util.session(this.config, req);
		return this.sessions.get(sessionId);
	}

	/**
	 *
	 * @param req
	 * @return
	 * @throws ServletException
	 */
	protected ServletSession createSession(HttpServletRequest req) throws ServletException {
		final ServletSession session = new ServletSession(this, req);
		if (this.sessions.putIfAbsent(session.getId(), session) != null) {
			throw new ServletException("Session already exists! [" + Util.session(this.config, req) + "]");
		}
		session.getEndpoint().onOpen(session, this.config);
		return session;
	}

	/**
	 *
	 * @param session
	 */
	public void unregister(ServletSession session) {
		final String id = session.getId();
		// if super old, remove straight away
		if (session.activeTime().isBefore(LocalDateTime.now().minus(5, ChronoUnit.SECONDS))) {
			this.log.debug("Removing session straight away. [{}]", id);
			this.sessions.remove(id);
			return;
		}

		this.scheduler.schedule(() -> {
			this.log.debug("Removing session after delay. [{}]", id);
			this.sessions.remove(id);
		}, 5, TimeUnit.SECONDS);
	}

	/**
	 *
	 * @param handler
	 * @param asyncCtx
	 */
	private void onRequest(AbstractHandler handler, AsyncContext asyncCtx) {
		final HttpAsyncContext async = new HttpAsyncContext(asyncCtx);
		try {
			handler.service(async);
		} catch (ServletException | IOException | RuntimeException e) {
			onError(e, async);
		}
	}

	/**
	 *
	 * @param t
	 * @param async
	 */
	private void onError(Throwable t, HttpAsyncContext async) {
		this.log.warn("Error while servicing request!", t);
		async.getResponse().setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		async.complete();
	}

	/**
	 *
	 */
	private void cleanupSessions() {
		this.log.info("Cleaning up inactive sessions. [count={}]", this.sessions.size());
		this.sessions.forEach((k, v) -> {
			final String id = v.getId();
			if (v.isOpen() && v.activeTime().isBefore(LocalDateTime.now().plus(5, ChronoUnit.SECONDS))) {
				try {
					v.close();
				} catch (IOException e) {
					this.log.warn("Error closing session! [" + id + "]", e);
				}
			}
		});
	}

	@Override
	public void destroy() { }
}
