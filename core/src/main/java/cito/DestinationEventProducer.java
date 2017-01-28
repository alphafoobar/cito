package cito;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [25 Jan 2017]
 */
@ApplicationScoped
public class DestinationEventProducer {
	private static final ThreadLocal<DestinationEvent> HOLDER = new ThreadLocal<>();

	@Produces @Dependent
	public static DestinationEvent get() {
		return HOLDER.get();
	}

	/**
	 * 
	 * @param e
	 */
	public static QuietClosable set(DestinationEvent e) {
		final DestinationEvent old = get();
		if (old != null) {
			throw new IllegalStateException("Already set!");
		}
		HOLDER.set(e);
		return new QuietClosable() {
			@Override
			public void close() {
				HOLDER.remove();
			}
		};
	}
}