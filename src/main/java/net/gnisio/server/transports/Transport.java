package net.gnisio.server.transports;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.clients.WebSocketClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

public interface Transport {
	/**
	 * Process any HTTP request
	 * 
	 * @param clientsStore
	 * @param req
	 * @param resp
	 * @param ctx
	 * @param sessionId
	 * @param remoteService
	 * @throws ClientConnectionMismatch 
	 * @throws ClientConnectionNotExists 
	 */
	void processRequest(ClientsStorage clientsStore, String clientId, HttpRequest req,
			HttpResponse resp, ChannelHandlerContext ctx, 
			AbstractRemoteService remoteService) throws ClientConnectionNotExists, ClientConnectionMismatch;

	/**
	 * Process WebSocket frame
	 * @param clientsStorage TODO
	 * @param msg
	 * @param ctx
	 * @param remoteService
	 * @param currentSessionId
	 * @param currentClientId 
	 * @throws ClientConnectionMismatch 
	 * @throws ClientConnectionNotExists 
	 */
	void processWebSocketFrame(ClientsStorage clientsStorage, WebSocketClient client, WebSocketFrame msg,
			ChannelHandlerContext ctx, AbstractRemoteService remoteService) throws ClientConnectionNotExists, ClientConnectionMismatch;


	/**
	 * This method get client connection from connections storage. If client
	 * with given id not exists it throws {@link ClientConnectionNotExists}
	 * exception. If it get client from client storage and class of this client
	 * is not clazz or ConnectingClient it throws
	 * {@link ClientConnectionMismatch} exception
	 * 
	 * @param storage
	 * @param clazz
	 * @return
	 */
	<T extends ClientConnection> T getClientConnection(String clientId,
			ClientsStorage storage, AbstractRemoteService remoteService, Class<T> clazz)
			throws ClientConnectionNotExists, ClientConnectionMismatch;
	
	/**
	 * This method process Socket.IO frame and return answer frame
	 * 
	 * @param frame
	 * @param client TODO
	 * @return
	 */
	SocketIOFrame processSocketIOFrame(SocketIOFrame frame, ClientConnection client, ClientsStorage storage, AbstractRemoteService remoteService);

	String getName();

}