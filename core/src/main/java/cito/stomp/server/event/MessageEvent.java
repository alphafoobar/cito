package cito.stomp.server.event;

import cito.stomp.Frame;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [19 Jul 2016]
 */
public class MessageEvent {
	private final String sessionId;
	private final Frame frame;

	public MessageEvent(Frame frame) {
		this(null, frame);
	}

	public MessageEvent(String sessionId, Frame frame) {
		this.sessionId = sessionId;
		this.frame = frame;
	}

	/**
	 * @return the originating session identifier. If this is a internally generated message (i.e. application code)
	 * then this will be {@code null}.
	 */
	public String sessionId() {
		return sessionId;
	}

	/**
	 * @return the STOMP frame.
	 */
	public Frame frame() {
		return frame;
	}


}
