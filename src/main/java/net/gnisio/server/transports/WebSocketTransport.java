package net.gnisio.server.transports;

import java.util.List;

import net.gnisio.server.PacketsProcessor.ConnectionContext;
import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientConnection.State;
import net.gnisio.server.clients.WebSocketClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.channel.ChannelFutureListener;
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
	public void processRequest(HttpRequest req, HttpResponse resp, String clientId, Packet packet,
			ServerContext servContext) throws ClientConnectionNotExists, ClientConnectionMismatch {

		// Only GET request supported
		if (req.getMethod() == HttpMethod.GET) {
			LOG.debug("Initiate websocket connection by GET request");

			try {
				// Try to get client connection
				WebSocketClient client = getClientConnection(clientId, WebSocketClient.class, servContext);
				packet.getContext().setClientConnection( client );
				servContext.getRemoteService().setClientConnection(client);

				// Handshaker
				WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
						getWebSocketLocation(req, servContext.isSSL()), null, false);
				WebSocketServerHandshaker handshaker = wsFactory.newHandshaker(req);
				
				if (handshaker == null) 
					wsFactory.sendUnsupportedWebSocketVersionResponse(packet.getCtx().getChannel());
				
				else 
					handshaker.handshake(packet.getCtx().getChannel(), req).addListener(
							WebSocketServerHandshaker.HANDSHAKE_LISTENER);

				// Set connection channel and handshaker in client connection
				// object
				List<SocketIOFrame> buff = null;
				synchronized (client) {
					// Set handshaker in client
					client.setHandshaker(handshaker);
					client.setCtx(packet.getCtx());
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
				servContext.getRemoteService().clearClientConnection();
			}

		} else
			packet.getCtx().getChannel().close().addListener(ChannelFutureListener.CLOSE);
	}

	@Override
	public void processWebSocketFrame(WebSocketFrame frame, Packet packet, ServerContext servContext)
			throws ClientConnectionNotExists, ClientConnectionMismatch {

		try {
			// Set current client in thread-local
			ClientConnection client  = packet.getContext().getClientConnection();
			servContext.getRemoteService().setClientConnection( client );

			// Check for closing frame
			if (frame instanceof CloseWebSocketFrame) {
				client.setState(State.DISCONNECTED);
				((WebSocketClient) client).getHandshaker().close(packet.getCtx().getChannel(), (CloseWebSocketFrame) frame);
				return;
			} else if (frame instanceof PingWebSocketFrame) {
				packet.getCtx().getChannel().write(new PongWebSocketFrame(frame.getBinaryData()));
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
			socketioFrame = processSocketIOFrame(socketioFrame, client, servContext);

			// Send result
			if (socketioFrame != null)
				client.sendFrame(socketioFrame);
		} finally {
			servContext.getRemoteService().clearClientConnection();
		}
	}

	private String getWebSocketLocation(HttpRequest req, boolean useSSL) {
		return "ws" + useSSL + "://" + req.getHeader(HttpHeaders.Names.HOST) + req.getUri();
	}

	@Override
	public String getName() {
		return "websocket";
	}

	public void handleDisconnect(ConnectionContext connContext, ServerContext servContext)
			throws ClientConnectionNotExists, ClientConnectionMismatch {

		try {
			ClientConnection client = connContext.getClientConnection();
			
			// Set current client in thread-local
			servContext.getRemoteService().setClientConnection( client );

			// Stop tasks and setconnection state
			client.stopCleanupTimers();
			client.stopHeartbeatTask();
			client.setState(State.DISCONNECTED);

			// Remove from clients store
			servContext.getClientsStorage().removeClient( connContext.getClientConnection() );
		} finally {
			servContext.getRemoteService().clearClientConnection();
		}
	}

}
