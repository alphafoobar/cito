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
package cito.broker.artemis;

import static java.util.Collections.emptySet;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.InjectionPoint;

import org.apache.deltaspike.core.api.literal.AnyLiteral;
import org.apache.deltaspike.core.api.literal.DefaultLiteral;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [2 Feb 2017]
 */
public class BrokerConfigBean implements Bean<BrokerConfig> {

	@Override
	public Class<?> getBeanClass() {
		return DefaultBrokerConfig.class;
	}

	@Override
	public Set<InjectionPoint> getInjectionPoints() {
		return emptySet();
	}

	@Override
	public boolean isNullable() {
		return false;
	}

	@Override
	public BrokerConfig create(CreationalContext<BrokerConfig> creationalContext) {
		return new DefaultBrokerConfig();
	}

	@Override
	public void destroy(BrokerConfig configuration, CreationalContext<BrokerConfig> creationalContext) { }

	@Override
	public Set<Type> getTypes() {
		Set<Type> types = new HashSet<>();
		types.add(DefaultBrokerConfig.class);
		types.add(BrokerConfig.class);
		return types;
	}

	@Override
	public Set<Annotation> getQualifiers() {
		Set<Annotation> qualifiers = new HashSet<>();
		qualifiers.add(new AnyLiteral());
		qualifiers.add(new DefaultLiteral());
		return qualifiers;
	}

	@Override
	public Class<? extends Annotation> getScope() {
		return ApplicationScoped.class;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public Set<Class<? extends Annotation>> getStereotypes() {
		return emptySet();
	}

	@Override
	public boolean isAlternative() {
		return false;
	}
}