package net.gnisio.server.impl;

import java.util.Set;

import org.jboss.netty.handler.codec.http.Cookie;

import net.gnisio.server.PacketsProcessor.ConnectionContext;
import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.transports.Transport;

/**
 * Default implementation of connection context
 * @author c58
 */
public class DefaultConnectionContext implements ConnectionContext {
	private ClientConnection conn;
	private String sess;
	private Transport transport;
	private Set<Cookie> cookies;

	@Override
	public ClientConnection getClientConnection() {
		return conn;
	}

	@Override
	public String getSessionId() {
		return sess;
	}

	@Override
	public Transport getTransport() {
		return transport;
	}

	@Override
	public void setClientConnection(ClientConnection connection) {
		this.conn = connection;
	}

	@Override
	public void setSessionId(String sess) {
		this.sess = sess;
	}

	@Override
	public void setTransport(Transport transport) {
		this.transport = transport;
	}

	@Override
	public Set<Cookie> getCookies() {
		return cookies;
	}

	@Override
	public void setCookies(Set<Cookie> cookies) {
		this.cookies = cookies;
	}

}
