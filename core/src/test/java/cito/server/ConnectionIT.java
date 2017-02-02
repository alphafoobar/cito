package cito.server;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;

import org.junit.Test;

import cito.Client;

public class ConnectionIT extends AbstractIT {
	@Test
	public void connect() throws DeploymentException, IOException, EncodeException, InterruptedException, ExecutionException, TimeoutException {
		final Client client = getClient();
		client.connect(10, TimeUnit.SECONDS);
		assertEquals(Client.State.CONNECTED, client.getState());
	}

}