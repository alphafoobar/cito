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
package cito.server;


import java.net.URI;

import javax.inject.Inject;
import javax.servlet.ServletException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.wildfly.swarm.Swarm;
import org.wildfly.swarm.arquillian.CreateSwarm;

import cito.Client;

/**
 * 
 * @author Daniel Siviter
 * @since v1.0 [15 Jul 2016]
 */
@RunWith(Arquillian.class)
@RunAsClient 
public abstract class AbstractIT {
	@Inject
	protected Logger log;
	////	private final int port;
	//
	@ArquillianResource
	private URI baseURL;
	private Client client;
	//
	//	protected AbstractTest() {
	//		this.log = LoggerFactory.getLogger(getClass());
	////		this.port = findPort();
	////		this.log.info("Creating test http listener on port ''{}''.", port);
	//	}

	@Deployment(testable = false)
	public static Archive<WebArchive> createDeployment() {
		return ShrinkWrap
				.create(WebArchive.class)
				.addPackages(true, "javax.ws.rs")
				.addPackages(true, "javax.jms")
				.addPackages(true, "org.apache.activemq")
				.addPackages(true, "io.netty")
				.addPackages(true, "org.apache.deltaspike")
				.addPackages(true, "civvi")
				.addPackages(true, "io.undertow.websockets")
				//				.addClasses(WebSocketServer.class, FrameEncoding.class, LogProvider.class, WebSocketSessionRegistry.class)
				//	                .addClasses(TestSuiteEnvironment.class, Alpha.class, Bravo.class, ComponentInterceptorBinding.class,
				//	                        ComponentInterceptor.class).addClasses(InjectionSupportTestCase.constructTestsHelperClasses)
				.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
				.addAsManifestResource(new StringAsset("io.undertow.websockets.jsr.UndertowContainerProvider"),
						"services/javax.websocket.ContainerProvider")
				.addAsManifestResource(new StringAsset("civvi.messaging.MessagingExtension"),
						"services/javax.enterprise.inject.spi.Extension")
				/*	.addAsManifestResource(createPermissionsXmlAsset(
						// Needed for the TestSuiteEnvironment.getServerAddress() and TestSuiteEnvironment.getHttpPort()
						new PropertyPermission("management.address", "read"),
						new PropertyPermission("node0", "read"),
						new PropertyPermission("jboss.http.port", "read"),
						// Needed for the serverContainer.connectToServer()
						new SocketPermission("*:" + TestSuiteEnvironment.getHttpPort(), "connect,resolve")),
						"permissions.xml")*/;
	}

	@Before
	public void setUp() throws ServletException {
		setUpClient();
	}

	private void setUpClient() {
		this.client = new Client(URI.create(this.baseURL.toString() + "websocket"));
	}

	protected Client getClient() {
		return this.client;
	}

	@CreateSwarm
	public static Swarm newContainer() throws Exception {
		Swarm swarm = new Swarm();
		// ... configure Swarm ...
		return swarm;
	}

	//	/**
	//	 * Override to supply own port.
	//	 * 
	//	 * @return the port to run the test on. By default this will let the OS find a free port.
	//	 */
	//	private int findPort() {
	//		try (final ServerSocket socket = new ServerSocket(0)) {
	//			return socket.getLocalPort();
	//		} catch (IOException e) {
	//			throw new RuntimeException("I should never happen!", e);
	//		}
	//	}
	//
	//	@After
	//	public void tearDown() throws IOException {
	//		this.client.close();
	//		//		this.undertow.stop();
	//	}
}
