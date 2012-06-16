package net.gnisio.server.transports;

import java.util.Arrays;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.clients.HTMLClient;
import net.gnisio.server.clients.XHRClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.google.gwt.thirdparty.guava.common.base.Charsets;

/**
 * HTMLFile transport implementation
 * @author c58
 */
public class HTMLTransport extends XHRTransport {

	/**
	 * Return HTML client instance
	 */
	@Override
	protected XHRClient doGetClientConnection(String clientId,
			ClientsStorage clientsStore, AbstractRemoteService remoteService)
			throws ClientConnectionNotExists, ClientConnectionMismatch {

		return getClientConnection(clientId, clientsStore, remoteService,
				HTMLClient.class);
	}

	@Override
	protected void doPrepareConnection(ChannelHandlerContext ctx, XHRClient client, HttpResponse resp) {
		// Set header for chunked connection
		resp.setChunked(true);
		resp.setHeader(HttpHeaders.Names.CONTENT_TYPE,
				"text/html; charset=UTF-8");
		resp.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
		resp.setHeader(HttpHeaders.Names.TRANSFER_ENCODING,
				HttpHeaders.Values.CHUNKED);

		// Write header
		Channel chan = ctx.getChannel();
		chan.write(resp);
		
		// Create initial message
		StringBuilder builder = new StringBuilder();
		builder.append("<html><body><script>var _ = function (msg) { parent.s._(msg, document); };</script>");
		char[] spaces = new char[174];
		Arrays.fill(spaces, ' '); 
		builder.append(spaces);

		// Make initial http chunk
		ChannelBuffer chunkContent = ChannelBuffers.dynamicBuffer(chan
				.getConfig().getBufferFactory());
		chunkContent.writeBytes( builder.toString().getBytes( Charsets.UTF_8 ) );
		HttpChunk chunk = new DefaultHttpChunk(chunkContent);
		
		// Write chunk
		chan.write(chunk);
	}

	@Override
	public String getName() {
		return "htmlfile";
	}
}
