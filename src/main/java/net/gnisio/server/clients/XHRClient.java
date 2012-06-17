package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.SocketIOManager;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;

public class XHRClient extends AbstractClient {

	// GET request fields
	protected HttpResponse resp;
	protected HttpRequest req;

	public XHRClient(String id, String sessionId, ClientsStorage clientsStorage, AbstractRemoteService remoteService) {
		super(id, sessionId, clientsStorage, remoteService);
	}

	public void setRequestFields(HttpRequest req, HttpResponse resp) {
		this.resp = resp;
		this.req = req;
	}

	@Override
	protected void doSendFrames(List<SocketIOFrame> resultFrames) {
		setState(State.CONNECTING);

		ChannelBuffer content = ChannelBuffers.copiedBuffer(SocketIOFrame.encodePayload(resultFrames),
				CharsetUtil.UTF_8);
		resp.addHeader(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=UTF-8");
		resp.setContent(content);

		SocketIOManager.sendHttpResponse(ctx, req, resp);
	}
}
