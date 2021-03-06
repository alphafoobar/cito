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
package cito.event;

import static java.util.Objects.requireNonNull;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [18 Jul 2016]
 */
@Immutable
public class DestinationChanged {
	private final Type type;
	private final String destination;

	public DestinationChanged(@Nonnull Type type, @Nonnull String destination) {
		this.type = requireNonNull(type);
		this.destination = requireNonNull(destination);
	}

	public Type getType() {
		return type;
	}

	public String getDestination() {
		return destination;
	}

	public boolean isTopic() {
		return destination.startsWith("topic/");
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
