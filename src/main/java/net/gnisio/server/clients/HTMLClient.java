package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.SocketIOFrame;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;

public class HTMLClient extends XHRClient {
	private final static String TEMPLATE = "<script>_('%s');</script>";

	public HTMLClient(String id, String sessionId,
			ClientsStorage clientsStorage, AbstractRemoteService remoteService) {
		super(id, sessionId, clientsStorage, remoteService);
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
		// Make result message
		String result = String.format(
				TEMPLATE,
				SocketIOFrame.encodePayload(resultFrames).replaceAll("\"",
						"\\\""));
		
		// Get channel
		Channel channel = ctx.getChannel();
		
		// Make http chunk
		ChannelBuffer chunkContent = ChannelBuffers.dynamicBuffer(channel
				.getConfig().getBufferFactory());
		chunkContent.writeBytes(result.getBytes());
		HttpChunk chunk = new DefaultHttpChunk(chunkContent);

		LOG.debug("Write HTTP chunk by htmlfile: " + result);
		
		// Write chunk
		channel.write(chunk);
	}
}
