package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;

import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebSocketClient extends AbstractClient {
	private static final Logger LOG = LoggerFactory.getLogger(WebSocketClient.class);
	private WebSocketServerHandshaker handshaker;

	public WebSocketClient(String id, String sessionId, ServerContext servContext) {
		super(id, sessionId, servContext);
	}

	public void setHandshaker(WebSocketServerHandshaker handshaker) {
		this.handshaker = handshaker;
	}

	public WebSocketServerHandshaker getHandshaker() {
		return handshaker;
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
		for (SocketIOFrame frame : resultFrames) {
			String encodedFrames = SocketIOFrame.encodePacket(frame);
			LOG.debug("Send WebSocket frame: " + encodedFrames);
			ctx.getChannel().write(new TextWebSocketFrame(encodedFrames));
		}
	}

}
