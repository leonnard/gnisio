package net.gnisio.server.impl;

import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.util.internal.ConcurrentHashMap;

import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientsStorage;

public class MemoryClientsStorage implements ClientsStorage {
	private final ConcurrentMap<String, ClientConnection> clientsMap = new ConcurrentHashMap<String, ClientConnection>();

	@Override
	public ClientConnection getClient(String clientId) {
		return clientsMap.get(clientId);
	}

	@Override
	public void addClient(ClientConnection client) {
		clientsMap.put(client.getId(), client);
	}

	@Override
	public void removeClient(ClientConnection client) {
		clientsMap.remove(client.getId());
	}

}
