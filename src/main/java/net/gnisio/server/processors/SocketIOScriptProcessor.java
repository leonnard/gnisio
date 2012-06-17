package net.gnisio.server.processors;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * This processor send socket.io.js as response. It load js to memory and use it
 * any time. For changing socket.io version you need restart the server.
 * 
 * @author c58
 */
public class SocketIOScriptProcessor extends RequestProcessor {
	private ChannelBuffer socketioJs;
	private long lastModified;
	private long fileLength;

	// Load default socket.io js
	public SocketIOScriptProcessor() throws IOException {
		// Get resource
		ClassLoader classLoader = getClass().getClassLoader();
		URL url = classLoader.getResource("net/gnisio/socket.io.js");

		loadFile(new File(url.getFile()));
	}

	// Load socket.io file to memory
	public SocketIOScriptProcessor(String jsPath) throws IOException {
		loadFile(new File(jsPath));
	}

	private void loadFile(File f) throws IOException {
		// Check file existing
		if (!f.exists() || !f.isFile() || !f.canRead())
			throw new FileNotFoundException(f.getAbsolutePath());

		// Create socketio byte array
		byte[] socketioJsBytes = new byte[(int) f.length()];
		lastModified = f.lastModified();
		fileLength = f.length();

		// Reade bytes
		InputStream stream = new BufferedInputStream(new FileInputStream(f));
		stream.read(socketioJsBytes);
		socketioJs = ChannelBuffers.copiedBuffer(socketioJsBytes);
	}

	@Override
	public void processRequest(HttpRequest request, HttpResponse resp, ChannelHandlerContext ctx) throws Exception {
		if (request.getMethod() != GET)
			StaticContentProcessor.sendError(resp, METHOD_NOT_ALLOWED);

		// Check for cached
		StaticContentProcessor.checkLastModification(request, resp, lastModified);

		// Set headers
		StaticContentProcessor.setDateAndCacheHeaders(resp, lastModified);
		setContentLength(resp, fileLength);
		resp.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/javascript; charset=UTF-8");

		// Write the initial line and the header.
		Channel ch = ctx.getChannel();
		ch.write(resp);

		// Write the file
		ChannelFuture writeFuture = ch.write(socketioJs);

		// Decide whether to close the connection or not.
		if (!isKeepAlive(request)) {
			// Close the connection when the whole content is written out.
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

}
