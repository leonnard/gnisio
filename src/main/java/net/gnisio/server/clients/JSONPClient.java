package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.SocketIOManager;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.util.CharsetUtil;

public class JSONPClient extends XHRClient {
	private final static String TEMPLATE = "io.j[%s]('%s');";

	public JSONPClient(String id, String sessionId, ServerContext servContext) {
		super(id, sessionId, servContext);
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
		setState(State.CONNECTING);

		// Get ID of request
		String reqIndex = null;
		int pos = req.getUri().lastIndexOf("&i="); // It's not good, but it's
													// fastest :)
		if (pos < 0)
			reqIndex = "0";
		else
			reqIndex = req.getUri().substring(pos + 3);

		// Encode frames
		String result = String.format(TEMPLATE, reqIndex,
				SocketIOManager.jsonStringify(SocketIOFrame.encodePayload(resultFrames)));

		// Set result in response
		ChannelBuffer content = ChannelBuffers.copiedBuffer(result, CharsetUtil.UTF_8);
		resp.setContent(content);

		// Set headers
		resp.setHeader(HttpHeaders.Names.CONTENT_TYPE, "text/javascript; charset=UTF-8");
		resp.addHeader("X-XSS-Protection", "0");

		// Send response
		LOG.debug("Send result by JSONP: " + result);
		SocketIOManager.sendHttpResponse(ctx, req, resp);
	}
}
