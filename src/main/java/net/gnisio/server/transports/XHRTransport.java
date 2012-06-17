package net.gnisio.server.transports;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.SocketIOManager;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientConnection.State;
import net.gnisio.server.clients.ClientsStorage;
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
	protected static final Logger LOG = LoggerFactory
			.getLogger(XHRTransport.class);

	@Override
	public void processRequest(ClientsStorage clientsStore, String clientId,
			HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx,
			AbstractRemoteService remoteService)
			throws ClientConnectionNotExists, ClientConnectionMismatch {

		// Process request-response
		if (req.getMethod() == HttpMethod.POST)
			processPostRequest(clientsStore, clientId, req, resp, ctx,
					remoteService);

		// Process server-push
		else if (req.getMethod() == HttpMethod.GET)
			processGetRequest(clientsStore, clientId, req, resp, ctx,
					remoteService);

		// Other requests not supported
		else {
			LOG.warn("Client send unsupported request: " + req.getMethod()
					+ ". Close connection");
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
	private void processPostRequest(ClientsStorage clientsStore,
			String clientId, HttpRequest req, HttpResponse resp,
			ChannelHandlerContext ctx, AbstractRemoteService remoteService)
			throws ClientConnectionNotExists, ClientConnectionMismatch {

		LOG.debug("Process XHR request-response (POST)");

		try {
			// Try to get client connection
			XHRClient client = doGetClientConnection(clientId, clientsStore,
					remoteService);

			// Set in remote service
			remoteService.setClientConnection(client);

			// Decode received socket.io payload
			List<SocketIOFrame> receivedFrames = SocketIOFrame
					.decodePayload( decodePostData(req.getContent().toString(
							Charsets.UTF_8)));

			// Process each frame and add to frames list
			List<SocketIOFrame> resultFrames = new ArrayList<SocketIOFrame>();

			for (SocketIOFrame frame : receivedFrames) {
				SocketIOFrame rf = processSocketIOFrame(frame, client,
						clientsStore, remoteService);

				if (rf != null)
					resultFrames.add(rf);
			}

			// Send all frames by client object
			if (resultFrames.size() > 0)
				client.sendFrames(resultFrames);

			// Send empty response
			SocketIOManager.sendHttpResponse(ctx, req, resp);

		} finally {
			// Unset client from remote service
			remoteService.clearClientConnection();
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
	protected XHRClient doGetClientConnection(String clientId,
			ClientsStorage clientsStore, AbstractRemoteService remoteService)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		return getClientConnection(clientId, clientsStore, remoteService,
				XHRClient.class);
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
	private void processGetRequest(ClientsStorage clientsStore,
			String clientId, HttpRequest req, HttpResponse resp,
			ChannelHandlerContext ctx, AbstractRemoteService remoteService)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		LOG.debug("Process XHR server-push (GET)");

		try {
			// Try to get client connection
			XHRClient client = doGetClientConnection(clientId, clientsStore,
					remoteService);
			
			remoteService.setClientConnection(client);

			// Prepare connection (needed for htmlfile transport)
			doPrepareConnection(ctx, client, resp);

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
				client.setCtx(ctx);
				client.setRequestFields(req, resp);
				client.setState(ClientConnection.State.CONNECTED);
			}

			// Write buffer if exists
			if (buff != null)
				client.sendFrames(buff);

		} finally {
			// Unset client from remote service
			remoteService.clearClientConnection();
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
	public void processWebSocketFrame(ClientsStorage clientsStorage,
			ClientConnection currentClient, WebSocketFrame msg,
			ChannelHandlerContext ctx, AbstractRemoteService remoteService)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		// do nothing...
	}

}
