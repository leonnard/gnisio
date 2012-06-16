package net.gnisio.server;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import net.gnisio.server.exceptions.ClientConnectionNotExists;
import net.gnisio.server.transports.HTMLTransport;
import net.gnisio.server.transports.JSONPTransport;
import net.gnisio.server.transports.Transport;
import net.gnisio.server.transports.WebSocketTransport;
import net.gnisio.server.transports.XHRTransport;

import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class contains socket.io options, some helper methods and tasks
 * scheduler.
 * 
 * @author c58
 */
public class SocketIOManager {
	private static final Logger LOG = LoggerFactory.getLogger(SocketIOManager.class);
	
	public static Option option = new Option();
	public static Map<String, Transport> transports = new HashMap<String, Transport>();
	private static final ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(2);

	// Initialize transports
	static {
		transports.put("xhr-polling", new XHRTransport());
		transports.put("htmlfile", new HTMLTransport());
		transports.put("jsonp-polling", new JSONPTransport());
		transports.put("websocket", new WebSocketTransport());
	}

	public static final class Option {
		public boolean heartbeat = true;
		public int heartbeat_timeout = 60;
		public int close_timeout = 60;
		public int heartbeat_interval = 25;
		public boolean flash_policy_server = true;
		public int flash_policy_port = 10843;
		public String transports = "websocket,xhr-polling,jsonp-polling,htmlfile";
		public String socketio_namespace = "io";

		{
			ResourceBundle bundle = ResourceBundle.getBundle("socketio");

			heartbeat = bundle.getString("heartbeat").equals("true");
			heartbeat_timeout = Integer.parseInt(bundle
					.getString("heartbeat_timeout"));
			close_timeout = Integer.parseInt(bundle.getString("close_timeout"));
			heartbeat_interval = Integer.parseInt(bundle
					.getString("heartbeat_interval"));
			flash_policy_server = bundle.getString("flash_policy_server")
					.equals("true");
			flash_policy_port = Integer.parseInt(bundle
					.getString("flash_policy_port"));
			transports = bundle.getString("transports");
			socketio_namespace = bundle.getString("socketio_namespace");
		}
	}

	/**
	 * Return handshake template with some oprions of socket.io
	 * 
	 * @return
	 */
	public static String getHandshakeTemplate() {
		return "%s:"
				+ (option.heartbeat ? Integer
						.toString(option.heartbeat_timeout) : "") + ":"
				+ option.close_timeout + ":" + option.transports;
	}

	/**
	 * This method get the transport name by given URI. Given uri must be in
	 * format:
	 * /{socket.io_namespace}/{protocol_ver}/{transport_name}/{client_id}
	 * 
	 * @param uri
	 * @return
	 */
	public static Transport getTransportByURI(String uri) {
		// Get transport name
		int pos1 = uri.lastIndexOf("/");
		int pos = uri.substring(0, pos1).lastIndexOf("/");
		String transportName = uri.substring(pos + 1, pos1);

		// Try to get transport from map
		return transports.get(transportName);
	}

	/**
	 * Helper method for sending HTTP response
	 * 
	 * @param ctx
	 * @param req
	 * @param res
	 */
	public static void sendHttpResponse(ChannelHandlerContext ctx,
			HttpRequest req, HttpResponse res) {
		// Set response length
		if (isKeepAlive(req)) {
			// Add 'Content-Length' header only for a keep-alive connection.
			res.setHeader(HttpHeaders.Names.CONTENT_LENGTH, res.getContent().readableBytes());
			// Add keep alive header as per:
			// http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
			res.setHeader(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);

		}

		LOG.debug("Write HTTP response to client: " + res.getContent().toString(Charset.forName("UTF-8")));
		
		// Send data
		ChannelFuture f = ctx.getChannel().write(res);

		// Close connection if not keep-alive
		if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}
	
	/**
	 * By request return client id
	 * 
	 * @param uri
	 * @return
	 * @throws ClientConnectionNotExists 
	 */
	public static String getClientId(HttpRequest req) throws ClientConnectionNotExists {
		QueryStringDecoder decoder = new QueryStringDecoder(req.getUri());
		String path = decoder.getPath().endsWith("/") ? decoder.getPath().substring(0, decoder.getPath().length()-1) : decoder.getPath();
		String clientId = path.substring( path.lastIndexOf("/")+1 );
		LOG.debug("Decoded clientId is: "+clientId+"; path is: "+path);
		return clientId;
	}

	public static ScheduledFuture<?> scheduleClearTask(Runnable runnable) {
		return scheduledExecutorService.schedule(runnable,
				option.close_timeout, TimeUnit.SECONDS);
	}

	public static ScheduledFuture<?> scheduleHeartbeatTask(Runnable runnable) {
		return scheduledExecutorService.schedule(runnable, option.heartbeat_interval,
				TimeUnit.SECONDS);
	}
	
	public static ScheduledFuture<?> scheduleHeartbeatTimeoutTask(Runnable runnable) {
		return scheduledExecutorService.schedule(runnable, option.heartbeat_timeout,
				TimeUnit.SECONDS);
	}

}