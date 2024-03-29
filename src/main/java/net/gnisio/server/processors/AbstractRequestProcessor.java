package net.gnisio.server.processors;

import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.SessionsStorage;
import net.gnisio.server.SessionsStorage.Session;
import net.gnisio.server.clients.ClientsStorage;

public abstract class AbstractRequestProcessor implements RequestProcessor {
	protected SessionsStorage sessionsStorage;
	protected ClientsStorage clientsStorage;

	/**
	 * This method invoked while adding it to the processors collection
	 * 
	 * @param sessionsStorage
	 * @param clientsStorage
	 */
	public void init(SessionsStorage sessionsStorage, ClientsStorage clientsStorage) {
		this.sessionsStorage = sessionsStorage;
		this.clientsStorage = clientsStorage;
	}
	
	public Session getSession(Packet packet) {
		return sessionsStorage.getSession(packet.getContext().getSessionId());
	}
}
