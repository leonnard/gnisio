package net.gnisio.server.transports;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientConnection.State;
import net.gnisio.server.clients.ConnectingClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractTransport implements Transport {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractTransport.class);

	@SuppressWarnings("unchecked")
	@Override
	public <T extends ClientConnection> T getClientConnection(String clientId, Class<T> clazz, ServerContext servContext)
			throws ClientConnectionNotExists, ClientConnectionMismatch {

		ClientConnection client = servContext.getClientsStorage().getClient(clientId);

		// If null - not reserved by handshake
		if (client == null)
			throw new ClientConnectionNotExists(clientId);

		// If only reserved or used before
		if (clazz.isInstance(client)) {
			return (T) client;
		} else if (client instanceof ConnectingClient) {
			try {
				// Create new instance of client
				T newClient = clazz.getDeclaredConstructor(String.class, String.class, ServerContext.class)
						.newInstance(clientId, client.getSessionId(), servContext);

				// Set timers
				client.stopCleanupTimers();
				newClient.resetCleanupTimers();

				// Add new client to starage
				servContext.getClientsStorage().addClient(newClient);
				return newClient;
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Can't create instance of " + clazz.toString());
			}
		} else
			throw new ClientConnectionMismatch(client.getClass(), clazz);
	}

	@Override
	public SocketIOFrame processSocketIOFrame(SocketIOFrame frame, ClientConnection client, ServerContext servContext) {
		SocketIOFrame resultFrame = null;

		switch (frame.getFrameType()) {
		case HEARTBEAT:
			LOG.debug("Process HEARTBEAT frame");
			client.stopCleanupTimers();
			client.startHeartbeatTask();

			if (client.getState() == State.DISCONNECTED)
				client.setState(State.CONNECTED);

			break;

		case MESSAGE:
			LOG.debug("Process MESSAGE frame");

			// Process data by remote service
			String result = servContext.getRemoteService().processPayload(frame.getData());

			// On success make a MESSAGE frame
			if (result != null)
				resultFrame = SocketIOFrame.makeMessage(result);
			break;

		case DISCONNECT:
			LOG.debug("Process DISCONNECT frame");

			// Remove client from storage
			servContext.getClientsStorage().removeClient(client);

			// Stop all tasks and close connection
			client.stopCleanupTimers();
			client.stopHeartbeatTask();
			client.setState(State.DISCONNECTED);
			client.getCtx().getChannel().close().addListener(ChannelFutureListener.CLOSE);
			break;
		}

		return resultFrame;
	}
}
