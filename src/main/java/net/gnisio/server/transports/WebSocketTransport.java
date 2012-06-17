package net.gnisio.server.transports;

import java.util.List;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientConnection.State;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.clients.WebSocketClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketTransport extends AbstractTransport {
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketTransport.class);

	@Override
	public void processRequest(ClientsStorage clientsStore, String clientId, HttpRequest req, HttpResponse resp,
			ChannelHandlerContext ctx, AbstractRemoteService remoteService) throws ClientConnectionNotExists,
			ClientConnectionMismatch {

		// Only GET request supported
		if (req.getMethod() == HttpMethod.GET) {
			LOG.debug("Initiate websocket connection by GET request");

			try {
				// Try to get client connection
				WebSocketClient client = getClientConnection(clientId, clientsStore, remoteService,
						WebSocketClient.class);

				remoteService.setClientConnection(client);

				// Handshaker
				WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
						getWebSocketLocation(req), null, false);
				WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
				if (handshaker == null) {
					wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
				} else {
					handshaker.handshake(ctx.getChannel(), req).addListener(
							WebSocketServerHandshaker.HANDSHAKE_LISTENER);
				}

				// Set connection channel and handshaker in client connection
				// object
				List<SocketIOFrame> buff = null;
				synchronized (client) {
					// Set handshaker in client
					client.setHandshaker(handshaker);
					client.setCtx(ctx);
					client.setState(State.CONNECTED);

					// Flush buffer if it's not empty
					if (!client.isBufferEmpty())
						buff = client.flushBuffer();
				}

				// If buffer is not empty
				if (buff != null)
					client.sendFrames(buff);

				// Initiate heartbeat
				client.stopCleanupTimers();
				client.startHeartbeatTask();
				client.sendFrame(SocketIOFrame.makeConnect());

			} finally {
				remoteService.clearClientConnection();
			}

		} else
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void processWebSocketFrame(ClientsStorage clientsStorage, ClientConnection client, WebSocketFrame frame,
			ChannelHandlerContext ctx, AbstractRemoteService remoteService) throws ClientConnectionNotExists,
			ClientConnectionMismatch {

		try {
			// Set current client in thread-local
			remoteService.setClientConnection(client);

			// Check for closing frame
			if (frame instanceof CloseWebSocketFrame) {
				client.setState(State.DISCONNECTED);
				((WebSocketClient) client).getHandshaker().close(ctx.getChannel(), (CloseWebSocketFrame) frame);
				return;
			} else if (frame instanceof PingWebSocketFrame) {
				ctx.getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
				return;
			} else if (!(frame instanceof TextWebSocketFrame)) {
				throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
						.getName()));
			}

			// Get request data
			String request = ((TextWebSocketFrame) frame).getText();

			// Decode socket.io frame
			SocketIOFrame socketioFrame = SocketIOFrame.decodePacket(request);

			// Process frame and get result
			socketioFrame = processSocketIOFrame(socketioFrame, client, clientsStorage, remoteService);

			// Send result
			if (socketioFrame != null)
				client.sendFrame(socketioFrame);
		} finally {
			remoteService.clearClientConnection();
		}
	}

	private String getWebSocketLocation(HttpRequest req) {
		return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + req.getUri();
	}

	@Override
	public String getName() {
		return "websocket";
	}

	public void handleDisconnect(ClientsStorage clientsStore, ClientConnection client,
			AbstractRemoteService remoteService) throws ClientConnectionNotExists, ClientConnectionMismatch {

		try {
			// Set current client in thread-local
			remoteService.setClientConnection(client);

			// Stop tasks and setconnection state
			client.stopCleanupTimers();
			client.stopHeartbeatTask();
			client.setState(State.DISCONNECTED);

			// Remove from clients store
			clientsStore.removeClient(client);
		} finally {
			remoteService.clearClientConnection();
		}
	}

}
