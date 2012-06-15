package net.gnisio.server.processors;

import net.gnisio.server.SessionsStorage;
import net.gnisio.server.clients.ClientsStorage;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

public abstract class RequestProcessor {
	protected SessionsStorage sessionsStorage;
	protected ClientsStorage clientsStorage;

	/**
	 * This method invoked while adding it to the processors collection
	 * @param sessionsStorage
	 * @param clientsStorage
	 */
	public void init(SessionsStorage sessionsStorage, ClientsStorage clientsStorage) {
		this.sessionsStorage = sessionsStorage;
		this.clientsStorage = clientsStorage;
	}
	
	/**
	 * This method invoked when some HTTP request received 
	 * @param req
	 * @param resp
	 * @param ctx 
	 * @throws Throwable 
	 */
	public abstract void processRequest(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx) throws Exception;
}
