package net.gnisio.server;

import net.gnisio.server.clients.ClientConnection;
import net.gnisio.shared.PushEventType;

public interface EventsPusher {
	
	/**
	 * Push the event to some clients
	 * @param event
	 * @param result
	 * @param node
	 */
	<T> void pushSystemEvent(PushEventType event, T result, String node);

	/**
	 * Subscribe some client to events
	 * @param client
	 * @param nodes
	 */
	void addSubscriber(ClientConnection client, String... nodes);
}
