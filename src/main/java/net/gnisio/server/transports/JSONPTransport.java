package net.gnisio.server.transports;

import java.util.List;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.clients.JSONPClient;
import net.gnisio.server.clients.XHRClient;
import net.gnisio.server.exceptions.ClientConnectionMismatch;
import net.gnisio.server.exceptions.ClientConnectionNotExists;

import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

public class JSONPTransport extends XHRTransport {

	@Override
	protected XHRClient doGetClientConnection(String clientId, ClientsStorage clientsStore,
			AbstractRemoteService remoteService) throws ClientConnectionNotExists, ClientConnectionMismatch {
		return getClientConnection(clientId, clientsStore, remoteService, JSONPClient.class);
	}

	@Override
	protected String decodePostData(String data) {
		QueryStringDecoder decoder = new QueryStringDecoder(data, CharsetUtil.UTF_8, false, 2);
		List<String> decoded = decoder.getParameters().get("d");
		String decodedString = (decoded != null && decoded.size() > 0) ? decoded.get(0) : "";

		// Remove brackets
		decodedString = decodedString.length() > 0 && decodedString.startsWith("\"") && decodedString.endsWith("\"") ? decodedString
				.substring(1, decodedString.length() - 1) : decodedString;
		decodedString = decodedString.trim();

		LOG.debug("POST data decodede by JSNOP transport: " + decodedString);
		return decodedString;
	}

	@Override
	public String getName() {
		return "jsonp-polling";
	}
}
