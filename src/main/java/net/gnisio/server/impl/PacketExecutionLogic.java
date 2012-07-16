package net.gnisio.server.impl;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import net.gnisio.server.PacketsProcessor.ConnectionContext;
import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SessionsStorage.Session;
import net.gnisio.server.SocketIOManager;
import net.gnisio.server.clients.ConnectingClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;
import net.gnisio.server.exceptions.ForceCloseConnection;
import net.gnisio.server.exceptions.StopRequestProcessing;
import net.gnisio.server.transports.WebSocketTransport;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.Cookie;
import org.jboss.netty.handler.codec.http.CookieDecoder;
import org.jboss.netty.handler.codec.http.CookieEncoder;
import org.jboss.netty.handler.codec.http.DefaultCookie;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class execute some raw packet
 * 
 * @author c58
 * 
 */
public class PacketExecutionLogic {
	static final Logger LOG = LoggerFactory.getLogger(PacketExecutionLogic.class);

	// Request check patterns
	private final static Pattern SOCKETIO_REQUEST_PATTERN = Pattern.compile("/"
			+ SocketIOManager.option.socketio_namespace + "/\\d{1}/([^/]+/[^/]+/?)?([^/]*)?");
	private final static Pattern HANDSHAKE_REQUEST_PATTERN = Pattern.compile("/"
			+ SocketIOManager.option.socketio_namespace + "/\\d{1}/([^/]*)?");

	// For processing
	protected final ServerContext servContext;

	// Cookie decoder and encoder
	private final CookieDecoder cookDecoder = new CookieDecoder();

	public PacketExecutionLogic(ServerContext serverContext) {
		this.servContext = serverContext;
	}

	/**
	 * Start point of netty raw packet processing
	 * 
	 * @param packet
	 * @throws Exception
	 */
	protected void processRawPacket(Packet packet) throws Exception {
		// handle websocket disconnect packet
		if(packet instanceof DisconnectPacket) {
			ConnectionContext connContext = packet.getContext();
			if (connContext != null && connContext.getTransport() != null && connContext.getTransport() instanceof WebSocketTransport) 
				((WebSocketTransport) connContext.getTransport()).handleDisconnect(connContext, servContext);
		}
		
		// handle general HTTP request
		else if (packet.getMessage() instanceof HttpRequest) {
			handleHttpRequest(packet, (HttpRequest) packet.getMessage());
		}
		
		// Process WebSocket frame (for websocket transport)
		else if (packet.getMessage() instanceof WebSocketFrame) {
			LOG.debug("Received WebSocket frame: "
					+ ((WebSocketFrame) packet.getMessage()).getBinaryData().toString(Charset.forName("UTF-8")));

			if (packet.getContext().getTransport() != null) {
				servContext.getSessionsStorage().resetClearTimer( packet.getContext().getSessionId() );
				packet.getContext().getTransport().processWebSocketFrame((WebSocketFrame) packet.getMessage(), packet, servContext);
			} else {
				LOG.warn("WebSocket frame received before websocket transport activated. Close connection");
				packet.getCtx().getChannel().close().addListener(ChannelFutureListener.CLOSE);
			}

		}
		// Close connection if received unknown message
		else {
			LOG.warn("Unknown message received. Close connection.");
			packet.getCtx().getChannel().close().addListener(ChannelFutureListener.CLOSE);
		}
	}

	/**
	 * Process HTTP request.
	 * 
	 * It prepare session and invoke handler method after session preparation.
	 * Next thing is checking what type of request: static content request or
	 * socket.io request.
	 * 
	 * @param ctx
	 * @param req
	 * @param e
	 * @throws Exception
	 */
	private void handleHttpRequest(Packet packet, HttpRequest req) throws Exception {
		LOG.debug("Request received: " + req.toString() + "\r\n\r\n"
				+ req.getContent().toString(Charset.forName("UTF-8")));
		HttpResponse resp = getInitResponse(req, HttpResponseStatus.OK);
		ChannelHandlerContext ctx = packet.getCtx();
		boolean isOk = false;

		// Prepare user session
		if (packet.getContext().getSessionId() == null)
			packet.getContext().setSessionId( prepareSession(req, resp) );

		try {
			// Invoke preprocessor before invoking request processor
			servContext.getProcessorsCollection().invokeRequestPreProcessor(req, resp, packet);
			isOk = true;

			// Check for Socket.IO request
			if (checkIORequest(req)) {
				processSocketIORequest(req, resp, packet);
			} else {
				// Invoke request processor for given request
				servContext.getProcessorsCollection().invokeRequestProcessor(req, resp, packet);
			}

		} catch (StopRequestProcessing ex) {
			LOG.warn("Request filtred and stoped by pre processor. Close connection");
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (ForceCloseConnection ex) {
			LOG.warn("Processor force close the connection. Send response and close connection: "+req.toString());
			SocketIOManager.sendHttpResponse(ctx, req, resp).addListener(ChannelFutureListener.CLOSE);
		} catch (ClientConnectionNotExists ex) {
			LOG.warn("Client connection with UUID=" + ex.toString() + " not exists in storage");
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (ClientConnectionMismatch ex) {
			LOG.warn("Client connection missmatch: " + ex.toString());
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (Exception ex) {
			LOG.error("Some unknown exception! " + ex.getClass() + ": " + ex.getMessage());
			ex.printStackTrace();
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} finally {
			// Invoke request post processor if pre-processor not throws the
			// exception
			if (isOk)
				servContext.getProcessorsCollection().invokeRequestPostProcessor(req, resp, packet);
		}
	}

	/**
	 * Prepare session by given Http request. Try to get session ID from cookie.
	 * On success update activity time in SessionStorage, otherwise create new
	 * session and set cookie ID
	 * 
	 * @param req
	 * @param resp
	 * @return
	 */
	private String prepareSession(HttpRequest req, HttpResponse resp) {
		String cookieString = req.getHeader(HttpHeaders.Names.COOKIE);

		// Try to find alive session in cookies
		if (cookieString != null) {
			Set<Cookie> cookies = cookDecoder.decode(cookieString);
			Iterator<Cookie> it = cookies.iterator();

			while (it.hasNext()) {
				Cookie cook = it.next();

				if (cook.getName().equals("__sessId")
						&& servContext.getSessionsStorage().getSession(cook.getValue()) != null) {
					servContext.getSessionsStorage().resetClearTimer(cook.getValue());
					return cook.getValue();
				}
			}
		}

		// Create session
		Session sess = servContext.getSessionsStorage().createSession();

		// Create cookie
		Cookie sessCookie = new DefaultCookie("__sessId", sess.getId());
		sessCookie.setPath("/");
		sessCookie.setDomain(servContext.getHost());
		sessCookie.setMaxAge(-1);

		// Set cookie in response
		CookieEncoder cookEncoder = new CookieEncoder(false);
		cookEncoder.addCookie(sessCookie);
		resp.setHeader(HttpHeaders.Names.SET_COOKIE, cookEncoder.encode());

		return sess.getId();
	}

	/**
	 * Check request URI for Socket.IO requesting. If given request is Socket.IO
	 * request return true. False otherwise.
	 * 
	 * @return
	 */
	private boolean checkIORequest(HttpRequest req) {
		return SOCKETIO_REQUEST_PATTERN.matcher(req.getUri()).matches();
	}

	/**
	 * Process any socket.io request
	 * 
	 * @param req
	 * @param resp
	 * @param context
	 * @throws ClientConnectionMismatch
	 * @throws ClientConnectionNotExists
	 */
	private void processSocketIORequest(HttpRequest req, HttpResponse resp, Packet packet)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		// If request is handshake
		if (HANDSHAKE_REQUEST_PATTERN.matcher(req.getUri()).matches()) {
			processHandshake(req, resp, packet);
		} else {
			processRegularSocketRequest(req, resp, packet);
		}
	}

	/**
	 * Process all socket.io request expect handshake
	 * 
	 * @param ctx
	 * @param req
	 * @param resp
	 * @param uri
	 * @param context
	 * @throws ClientConnectionMismatch
	 * @throws ClientConnectionNotExists
	 */
	private void processRegularSocketRequest(HttpRequest req, HttpResponse resp, Packet packet)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		if (packet.getContext().getTransport() == null)
			packet.getContext().setTransport(SocketIOManager.getTransportByURI(req.getUri()));

		if (packet.getContext().getTransport() != null) {
			// Process request
			LOG.debug("Process regular socket.io request by transport: " + packet.getContext().getTransport().getName());
			packet.getContext().getTransport()
					.processRequest(req, resp, SocketIOManager.getClientId(req), packet, servContext);
		} else {
			resp.setStatus(HttpResponseStatus.FORBIDDEN);
			SocketIOManager.sendHttpResponse(packet.getCtx(), req, resp);
		}
	}

	/**
	 * Process handshake request
	 * 
	 * @param ctx
	 * @param req
	 * @param resp
	 * @param uri
	 * @param context
	 */
	private void processHandshake(HttpRequest req, HttpResponse resp, Packet packet) {
		LOG.debug("Process handshake: " + req.getUri());

		// Create unique ID
		final String uID = getUniqueID();

		// Reserve uid in storage
		servContext.getClientsStorage().addClient(
				new ConnectingClient(uID, packet.getContext().getSessionId(), servContext));

		// Create handshake answer
		String contentString = String.format(SocketIOManager.getHandshakeTemplate(), uID);
		ChannelBuffer content = ChannelBuffers.copiedBuffer(contentString, CharsetUtil.UTF_8);

		LOG.debug("Handshake response string is: " + contentString);

		// Make response
		resp.setContent(content);
		resp.addHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		resp.addHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

		// Write response
		SocketIOManager.sendHttpResponse(packet.getCtx(), req, resp);
	}

	/**
	 * Return unique ID for new connected client
	 * 
	 * @return
	 */
	protected String getUniqueID() {
		return SocketIOManager.generateString(64);
	}

	/**
	 * Helper method for creating HTTP response
	 * 
	 * @param req
	 * @param status
	 * @return
	 */
	private HttpResponse getInitResponse(HttpRequest req, HttpResponseStatus status) {
		HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1, status);

		if (req != null && req.getHeader("Origin") != null) {
			resp.addHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
			resp.addHeader("Access-Control-Allow-Credentials", "true");
		}

		return resp;
	}
}
