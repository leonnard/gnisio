package net.gnisio.server.processors;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.SessionsStorage;
import net.gnisio.server.clients.ClientsStorage;

public interface RequestProcessor {
	
	void init(SessionsStorage sessionsStorage, ClientsStorage clientsStorage);
	
	/**
	 * This method invoked when some HTTP request received
	 * 
	 * @param req
	 * @param resp
	 * @param packet
	 * @throws Throwable
	 */
	void processRequest(HttpRequest req, HttpResponse resp, Packet packet) throws Exception;
}
