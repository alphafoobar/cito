package cito.server.security;

import static cito.ReflectionUtil.getAnnotationValue;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import cito.server.SecurityContext;
import cito.server.security.Builder.Limitation;
import cito.stomp.Frame;

/**
 * Registry should always allow {@code null} destinations.
 * 
 * @author Daniel Siviter
 * @since v1.0 [30 Aug 2016]
 */
@ApplicationScoped
public class SecurityRegistry {
	private final Set<Limitation> limitations = new LinkedHashSet<>();

	@Inject @Any
	private Instance<SecurityCustomiser> configurers;

	/**
	 * 
	 * @param limitation
	 */
	public synchronized void register(Limitation limitation) {
		this.limitations.add(limitation);
	}

	/**
	 * 
	 * @param frame
	 * @return
	 */
	public synchronized List<Limitation> getMatching(Frame frame) {
		return this.limitations.stream().filter(e -> e.matches(frame)).collect(Collectors.toList());
	}

	/**
	 * 
	 * @param frame
	 * @param ctx
	 * @return
	 */
	public boolean isPermitted(Frame frame, SecurityContext ctx) {
		for (Limitation limitation : getMatching(frame)) {
			if (!limitation.isPermitted(ctx)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 
	 * @return
	 */
	public Builder builder() {
		return builder(this);
	}

	/**
	 * 
	 * @param configurer
	 */
	public void configure(SecurityCustomiser configurer) {
		configurer.configure(this);
	}

	/**
	 * 
	 */
	@PostConstruct
	public void init() {
		final Set<SecurityCustomiser> configurers = new TreeSet<>(Comparator.comparing(SecurityRegistry::getPriority));
		this.configurers.forEach(configurers::add);
		configurers.forEach(c -> { 
			configure(c);
			this.configurers.destroy(c);
		});
	}



	// --- Static Methods ---

	/**
	 * 
	 * @return
	 */
	public static Builder builder(SecurityRegistry registry) {
		return new Builder(registry);
	}

	/**
	 * Returns the {@link Priority#value()} if available or 5000 if not.
	 * 
	 * @param config
	 * @return
	 */
	private static int getPriority(SecurityCustomiser config) {
		return getAnnotationValue(config, Priority.class, 5000);
	}
}