package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.SocketIOFrame;

/**
 * This is empty implementation of ClientConnection used for reserving uuid in
 * clients storage.
 * 
 * @author c58
 */
public class ConnectingClient extends AbstractClient {

	public ConnectingClient(String id, String sessionId, ClientsStorage clientsStorage,
			AbstractRemoteService remoteService) {
		super(id, sessionId, clientsStorage, remoteService);
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
	}

}
