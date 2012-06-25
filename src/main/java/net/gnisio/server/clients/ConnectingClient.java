package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;

/**
 * This is empty implementation of ClientConnection used for reserving uuid in
 * clients storage.
 * 
 * @author c58
 */
public class ConnectingClient extends AbstractClient {

	public ConnectingClient(String id, String session, ServerContext servContext) {
		super(id, session, servContext);
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
	}

}
