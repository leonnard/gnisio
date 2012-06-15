package net.gnisio.server.clients;


/**
 * Storage of connected clients
 * 
 * @author c58
 */
public interface ClientsStorage {

	ClientConnection getClient(String clientId);

	void addClient(ClientConnection client);

	void removeClient(ClientConnection client);

}
