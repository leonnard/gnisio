package net.gnisio.server;

import java.nio.charset.Charset;
import java.util.UUID;
import java.util.regex.Pattern;

import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.clients.ConnectingClient;
import net.gnisio.server.clients.WebSocketClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;
import net.gnisio.server.exceptions.ForceCloseConnection;
import net.gnisio.server.exceptions.StopRequestProcessing;
import net.gnisio.server.processors.RequestProcessorsCollection;
import net.gnisio.server.transports.Transport;
import net.gnisio.server.transports.WebSocketTransport;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
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

public class GnisioPipelineHandler extends SimpleChannelUpstreamHandler {
	static final Logger LOG = LoggerFactory
			.getLogger(GnisioPipelineHandler.class);

	// Request check patterns
	private final static Pattern SOCKETIO_REQUEST_PATTERN = Pattern.compile("/"
			+ SocketIOManager.option.socketio_namespace
			+ "/\\d{1}/([^/]+/[^/]+/?)?([^/]*)?");
	private final static Pattern HANDSHAKE_REQUEST_PATTERN = Pattern
			.compile("/" + SocketIOManager.option.socketio_namespace
					+ "/\\d{1}/([^/]*)?");

	// Storages
	private final ClientsStorage clientsStore;
	private final SessionsStorage sessionsStore;
	private final AbstractRemoteService remoteService;

	// Request processors collection
	private RequestProcessorsCollection requestProcessors;

	// For WebSocket and http keep-alive connections
	private Transport activeTransport = null;
	private String currentSessionId = null;
	private String currentClientId = null;
	private WebSocketClient wsClient = null;

	public GnisioPipelineHandler(SessionsStorage sessionsStore,
			ClientsStorage clientsStore,
			RequestProcessorsCollection requestProcessors,
			AbstractRemoteService remoteService) {
		this.sessionsStore = sessionsStore;
		this.clientsStore = clientsStore;
		this.requestProcessors = requestProcessors;
		this.remoteService = remoteService;
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			ChannelStateEvent e) throws Exception {
		if (activeTransport != null
				&& activeTransport instanceof WebSocketTransport) {
			setWebSocketClient(ctx);
			((WebSocketTransport) activeTransport).handleDisconnect(
					clientsStore, wsClient, remoteService);
		}
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();

		// Process HTTP request
		if (msg instanceof HttpRequest) {
			handleHttpRequest(ctx, (HttpRequest) msg, e);
		}

		// Process WebSocket frame
		else if (msg instanceof WebSocketFrame) {
			LOG.debug("Received WebSocket frame: "
					+ ((WebSocketFrame) msg).getBinaryData().toString(
							Charset.forName("UTF-8")));

			if (activeTransport != null){
				setWebSocketClient(ctx);
				activeTransport.processWebSocketFrame(clientsStore, wsClient,
						(WebSocketFrame) msg, ctx, remoteService);
			}
			else {
				LOG.warn("WebSocket frame received before websocket transport activated. Close connection");
				ctx.getChannel().close()
						.addListener(ChannelFutureListener.CLOSE);
			}

		}
		// Close connection if received unknown message
		else {
			LOG.warn("Unknown message received. Close connection.");
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	/**
	 * Set WS client in this object and throw an exception of
	 * current client connection is not WSClient
	 * @throws Exception 
	 */
	private void setWebSocketClient(ChannelHandlerContext ctx) throws Exception {
		if(wsClient == null) {
			ClientConnection cl = clientsStore.getClient(currentClientId);
			
			if(cl instanceof WebSocketClient)
				wsClient = (WebSocketClient)cl;
			else {
				LOG.error("Current client must be WebSocket client but it dosn't. WTF?");
				ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
				throw new Exception();
			}
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
	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req,
			MessageEvent e) throws Exception {
		LOG.debug("Request received: " + req.toString() + "\r\n\r\n"
				+ req.getContent().toString(Charset.forName("UTF-8")));
		HttpResponse resp = getInitResponse(req, HttpResponseStatus.OK);
		boolean isOk = false;

		// Prepare user session
		if (currentSessionId == null)
			currentSessionId = prepareSession(req);

		try {
			// Invoke preprocessor before invoking request processor
			requestProcessors.invokeRequestPreProcessor(req, resp, ctx,
					currentSessionId);
			isOk = true;

			// Check for Socket.IO request
			if (checkIORequest(req)) {
				processSocketIORequest(ctx, req, resp);
			} else {
				// Invoke request processor for given request
				requestProcessors.invokeRequestProcessor(req, resp, ctx,
						currentSessionId);
			}

		} catch (StopRequestProcessing ex) {
			LOG.warn("Request filtred and stoped by pre processor. Close connection");
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (ForceCloseConnection ex) {
			LOG.warn("Processor force close the connection. Send response and close connection");
			SocketIOManager.sendHttpResponse(ctx, req, resp);
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (ClientConnectionNotExists ex) {
			LOG.warn("Client connection with UUID=" + ex.toString()
					+ " not exists in storage");
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (ClientConnectionMismatch ex) {
			LOG.warn("Client connection missmatch: " + ex.toString());
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} catch (Exception ex) {
			LOG.error("Some unknown exception! " + ex.getClass() + ": "
					+ ex.getMessage());
			ex.printStackTrace();
			ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);
		} finally {
			// Invoke request post processor if pre-processor not throws the
			// exception
			if (isOk)
				requestProcessors.invokeRequestPostProcessor(req, resp, ctx,
						currentSessionId);
		}
	}

	/**
	 * Prepare session by given Http request. Try to get session ID from cookie.
	 * On success update activity time in SessionStorage, otherwise create new
	 * session and set cookie ID
	 * 
	 * @param req
	 * @return
	 */
	private String prepareSession(HttpRequest req) {
		// TODO Auto-generated method stub
		return null;
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
	 * @throws ClientConnectionMismatch
	 * @throws ClientConnectionNotExists
	 */
	private void processSocketIORequest(ChannelHandlerContext ctx,
			HttpRequest req, HttpResponse resp)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		String uri = req.getUri();

		// If request is handshake
		if (HANDSHAKE_REQUEST_PATTERN.matcher(uri).matches()) {
			processHandshake(ctx, req, resp, uri);
		} else {
			processRegularSocketRequest(ctx, req, resp, uri);
		}
	}

	/**
	 * Process all socket.io request expect handshake
	 * 
	 * @param ctx
	 * @param req
	 * @param resp
	 * @param uri
	 * @throws ClientConnectionMismatch
	 * @throws ClientConnectionNotExists
	 */
	private void processRegularSocketRequest(ChannelHandlerContext ctx,
			HttpRequest req, HttpResponse resp, String uri)
			throws ClientConnectionNotExists, ClientConnectionMismatch {
		if (activeTransport == null)
			activeTransport = SocketIOManager.getTransportByURI(uri);

		if (activeTransport != null) {
			// Get client id
			currentClientId = SocketIOManager.getClientId(req);

			// Process request
			LOG.debug("Process regular socket.io request by transport: "
					+ activeTransport.getName());
			activeTransport.processRequest(clientsStore, currentClientId, req,
					resp, ctx, remoteService);
		} else {
			resp.setStatus(HttpResponseStatus.FORBIDDEN);
			SocketIOManager.sendHttpResponse(ctx, req, resp);
		}
	}

	/**
	 * Process handshake request
	 * 
	 * @param ctx
	 * @param req
	 * @param resp
	 * @param uri
	 */
	private void processHandshake(ChannelHandlerContext ctx, HttpRequest req,
			HttpResponse resp, String uri) {
		LOG.debug("Process handshake: " + uri);

		// Create unique ID
		final String uID = getUniqueID();

		// Reserve uid in storage
		clientsStore.addClient(new ConnectingClient(uID, currentSessionId,
				clientsStore, remoteService));

		// Create handshake answer
		String contentString = String.format(
				SocketIOManager.getHandshakeTemplate(), uID);
		ChannelBuffer content = ChannelBuffers.copiedBuffer(contentString,
				CharsetUtil.UTF_8);

		LOG.debug("Handshake response string is: " + contentString);

		// Make response
		resp.setContent(content);
		resp.addHeader(HttpHeaders.Names.CONTENT_TYPE,
				"text/plain; charset=UTF-8");
		resp.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);

		// Write response
		SocketIOManager.sendHttpResponse(ctx, req, resp);
	}

	/**
	 * Return unique ID for new connected client
	 * 
	 * @return
	 */
	protected String getUniqueID() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Helper method for creating HTTP response
	 * 
	 * @param req
	 * @param status
	 * @return
	 */
	private HttpResponse getInitResponse(HttpRequest req,
			HttpResponseStatus status) {
		HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				status);

		if (req != null && req.getHeader("Origin") != null) {
			resp.addHeader("Access-Control-Allow-Origin",
					req.getHeader("Origin"));
			resp.addHeader("Access-Control-Allow-Credentials", "true");
		}

		return resp;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		Channel ch = e.getChannel();
		Throwable cause = e.getCause();
		if (cause instanceof TooLongFrameException) {
			// sendError(ctx, BAD_REQUEST);
			return;
		}

		cause.printStackTrace();
		if (ch.isConnected()) {
			// sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}
}
