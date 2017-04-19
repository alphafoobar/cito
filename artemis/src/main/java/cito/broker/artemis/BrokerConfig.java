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

import static org.apache.activemq.artemis.core.remoting.impl.invm.TransportConstants.SERVER_ID_PROP_NAME;
import static org.apache.activemq.artemis.jms.client.ActiveMQDestination.createTopicAddressFromName;

import java.util.Collections;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;

import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyAcceptorFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.NettyConnectorFactory;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [2 Feb 2017]
 */
@ApplicationScoped
public class BrokerConfig {
	public static final String IN_VM_CONNECTOR = InVMConnectorFactory.class.getName();
	public static final String REMOTE_CONNECTOR = NettyConnectorFactory.class.getName();
	public static final String IN_VM_ACCEPTOR = InVMAcceptorFactory.class.getName();
	public static final String REMOTE_ACCEPTOR = NettyAcceptorFactory.class.getName();



	/**
	 * @return if present, sends a username for the connection
	 */
	public String getUsername() {
		return null;
	}

	/**
	 * @return the password for the connection.  If username is set, password must be set
	 */
	public String getPassword() {
		return null;
	}

	/**
	 * Either url should be set, or host, port, connector factory should be set.
	 *
	 * @return if set, will be used in the server locator to look up the server instead of the hostname/port combination
	 */
	public String getUrl() {
		return null;
	}

	/**
	 * @return The hostname to connect to
	 */
	public String getHost() {
		return null;
	}

	/**
	 * @return the port number to connect to
	 */
	public Integer getPort() {
		return null;
	}

	/**
	 * @return the connector factory to use for connections.
	 */
	public String getConnectorFactory() {
		return IN_VM_CONNECTOR;
	}

	/**
	 * @return Whether or not to start the embedded broker
	 */
	public boolean startEmbeddedBroker() {
		return true;
	}
	/**
	 * @return whether or not this is an HA connection
	 */
	public boolean isHa() {
		return false;
	}

	/**
	 * @return whether or not the authentication parameters should be used
	 */
	public boolean hasAuthentication() {
		return false;
	}

	/**
	 * @return the configuration that will be used in the broker.
	 */
	public Configuration getConfiguration() {
		final Map<String, Object> params = Collections.singletonMap(SERVER_ID_PROP_NAME, "1");
		final Configuration config = new ConfigurationImpl()
				.setSecurityEnabled(false)
				.setPersistenceEnabled(false);
		// convert to use JMS style topic for management
		config.setManagementAddress(createTopicAddressFromName(config.getManagementAddress().toString()));
		config.setManagementNotificationAddress(createTopicAddressFromName(config.getManagementNotificationAddress().toString()));
		// default In VM Acceptor for all your ConnectionFactory needs
		config.addAcceptorConfiguration(new TransportConfiguration(IN_VM_ACCEPTOR, params, "InVMAcceptor"));
		return config;
	}
}

