package net.gnisio.server.transports;

import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

public interface Transport {
	/**
	 * Process any HTTP request
	 * 
	 * @param req
	 * @param resp
	 * @param clientId TODO
	 * @param packet 
	 * @param servContext TODO
	 * @param sessionId
	 * @throws ClientConnectionMismatch
	 * @throws ClientConnectionNotExists
	 */
	void processRequest(HttpRequest req, HttpResponse resp, String clientId, Packet packet,
			ServerContext servContext) throws ClientConnectionNotExists,
			ClientConnectionMismatch;

	/**
	 * Process WebSocket frame
	 * @param msg
	 * @param packet TODO
	 * @param sercContext TODO
	 * @param currentSessionId
	 * @param currentClientId
	 * 
	 * @throws ClientConnectionMismatch
	 * @throws ClientConnectionNotExists
	 */
	void processWebSocketFrame(WebSocketFrame msg, Packet packet, ServerContext sercContext) throws ClientConnectionNotExists,
			ClientConnectionMismatch;

	/**
	 * This method get client connection from connections storage. If client
	 * with given id not exists it throws {@link ClientConnectionNotExists}
	 * exception. If it get client from client storage and class of this client
	 * is not clazz or ConnectingClient it throws
	 * {@link ClientConnectionMismatch} exception
	 * @param clazz
	 * @param servContext TODO
	 * 
	 * @return
	 */
	<T extends ClientConnection> T getClientConnection(String clientId, Class<T> clazz,
			ServerContext servContext) throws ClientConnectionNotExists,
			ClientConnectionMismatch;

	/**
	 * This method process Socket.IO frame and return answer frame
	 * 
	 * @param frame
	 * @param client TODO
	 * @param servContext TODO
	 * @return
	 */
	SocketIOFrame processSocketIOFrame(SocketIOFrame frame, ClientConnection client, ServerContext servContext);

	String getName();

}