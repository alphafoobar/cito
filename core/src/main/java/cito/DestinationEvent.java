package cito;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [18 Jul 2016]
 */
public class DestinationEvent {
	private final Type type;
	private final String destination;

	public DestinationEvent(Type type, String destination) {
		this.type = type;
		this.destination = destination;
	}

	public Type getType() {
		return type;
	}

	public String getDestination() {
		return destination;
	}

	public boolean isTopic() {
		return destination.startsWith("/topic/");
	}

	// --- Inner Classes ---

	/**
	 * 
	 * @author Daniel Siviter
	 * @since v1.0 [19 Jul 2016]
	 */
	public enum Type {
		ADDED,
		REMOVED
	}
}
