package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.SocketIOManager;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;

import com.google.gwt.thirdparty.guava.common.base.Charsets;

public class HTMLClient extends XHRClient {
	private final static String TEMPLATE = "<script>_('%s');</script>";

	public HTMLClient(String id, String sessionId, ServerContext servContext) {
		super(id, sessionId, servContext);
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
		// Make result message
		String result = String.format(TEMPLATE,
				SocketIOManager.jsonStringify(SocketIOFrame.encodePayload(resultFrames)));

		// Get channel
		Channel channel = ctx.getChannel();

		// Make http chunk
		ChannelBuffer chunkContent = ChannelBuffers.dynamicBuffer(channel.getConfig().getBufferFactory());
		chunkContent.writeBytes(result.getBytes(Charsets.UTF_8));
		HttpChunk chunk = new DefaultHttpChunk(chunkContent);

		LOG.debug("Write HTTP chunk by htmlfile: " + result);

		// Write chunk
		channel.write(chunk);
	}
}
