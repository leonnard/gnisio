package net.gnisio.server.transports;

import java.util.ArrayList;
import java.util.List;

import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.SocketIOManager;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientConnection.State;
import net.gnisio.server.clients.XHRClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.thirdparty.guava.common.base.Charsets;

public class XHRTransport extends AbstractTransport {
	protected static final Logger LOG = LoggerFactory.getLogger(XHRTransport.class);

	@Override
	public void processRequest(HttpRequest req, HttpResponse resp, String clientId, Packet packet,
			ServerContext servContext) throws ClientConnectionNotExists,
			ClientConnectionMismatch {

		// Process request-response
		if (req.getMethod() == HttpMethod.POST)
			processPostRequest(clientId, req, resp, packet, servContext);

		// Process server-push
		else if (req.getMethod() == HttpMethod.GET)
			processGetRequest(clientId, req, resp, packet, servContext);

		// Other requests not supported
		else {
			LOG.warn("Client send unsupported request: " + req.getMethod() + ". Close connection");
		}
	}

	/**
	 * POST request in XHR transport is only for client to server data pipe, not
	 * vise-versa. In this fact, this method process each frame and send it by
	 * ClientConnection.
	 * 
	 * In XHRClient method sendFrames doing two different things: 1. Send data
	 * to client if connection alive 2. Put data to buffer otherwise
	 * 
	 * @param clientsStore
	 * @param req
	 * @param resp
	 * @param ctx
	 * @param sessionId
	 * @param remoteService
	 * @throws ClientConnectionNotExists
	 * @throws ClientConnectionMismatch
	 */
	private void processPostRequest(String clientId, HttpRequest req, HttpResponse resp,
			Packet packet, ServerContext servContext) throws ClientConnectionNotExists,
			ClientConnectionMismatch {

		LOG.debug("Process XHR request-response (POST)");

		try {
			// Try to get client connection
			XHRClient client = doGetClientConnection(clientId, servContext);
			packet.getContext().setClientConnection( client );

			// Set in remote service
			servContext.getRemoteService().setClientConnection(client);

			// Decode received socket.io payload
			List<SocketIOFrame> receivedFrames = SocketIOFrame.decodePayload(decodePostData(req.getContent().toString(
					Charsets.UTF_8)));

			// Process each frame and add to frames list
			List<SocketIOFrame> resultFrames = new ArrayList<SocketIOFrame>();

			for (SocketIOFrame frame : receivedFrames) {
				SocketIOFrame rf = processSocketIOFrame(frame, null, null);

				if (rf != null)
					resultFrames.add(rf);
			}

			// Send all frames by client object
			if (resultFrames.size() > 0)
				client.sendFrames(resultFrames);

			// Send empty response
			SocketIOManager.sendHttpResponse(packet.getCtx(), req, resp);

		} finally {
			// Unset client from remote service
			servContext.getRemoteService().clearClientConnection();
		}
	}

	/**
	 * Needed for decoding form POST data in JSONP transport
	 * 
	 * @param data
	 * @return
	 */
	protected String decodePostData(String data) {
		return data;
	}

	/**
	 * Override this method for getting some extends of XHRClient
	 */
	protected XHRClient doGetClientConnection(String clientId, ServerContext servContext) throws ClientConnectionNotExists, ClientConnectionMismatch {
		return getClientConnection(clientId, XHRClient.class, null);
	}

	/**
	 * GET request is only for server-client data transfering in XHR-polling.
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
	private void processGetRequest(String clientId, HttpRequest req, HttpResponse resp,
			Packet packet, ServerContext servContext) throws ClientConnectionNotExists,
			ClientConnectionMismatch {
		LOG.debug("Process XHR server-push (GET)");

		try {
			// Try to get client connection
			XHRClient client = doGetClientConnection(clientId, servContext);
			packet.getContext().setClientConnection( client );

			servContext.getRemoteService().setClientConnection(client);

			// Prepare connection (needed for htmlfile transport)
			doPrepareConnection(packet.getCtx(), client, resp);

			List<SocketIOFrame> buff = null;
			synchronized (client) {
				// If client disconnected -> send connected frame and heartbeat
				if (client.getState() == State.DISCONNECTED) {
					client.startHeartbeatTask();
					client.stopCleanupTimers();
					client.sendFrame(SocketIOFrame.makeConnect());
				}

				// If buffer is not empty - put it to response
				if (!client.isBufferEmpty()) {
					LOG.debug("Client buffer is NOT empty. Flush buffer");
					buff = client.flushBuffer();
				}

				// Set all things for connected client
				client.setCtx(packet.getCtx());
				client.setRequestFields(req, resp);
				client.setState(ClientConnection.State.CONNECTED);
			}

			// Write buffer if exists
			if (buff != null)
				client.sendFrames(buff);

		} finally {
			// Unset client from remote service
			servContext.getRemoteService().clearClientConnection();
		}
	}

	/**
	 * This method invoked after getting client in GET processing. No frames had
	 * sent before this invokation
	 * 
	 * @param ctx
	 * @param client
	 */
	protected void doPrepareConnection(ChannelHandlerContext ctx, XHRClient client, HttpResponse resp) {
		// do nothing...
	}

	@Override
	public String getName() {
		return "xhr-polling";
	}

	@Override
	public void processWebSocketFrame(WebSocketFrame msg, Packet packet,
			ServerContext sercContext)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		// do nothing...
	}

}
