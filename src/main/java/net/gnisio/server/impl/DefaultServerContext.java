package net.gnisio.server.impl;

import net.gnisio.server.AbstractRemoteService;
import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SessionsStorage;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.processors.RequestProcessorsCollection;

public class DefaultServerContext implements ServerContext {
	private final SessionsStorage sessionsStore;
	private final ClientsStorage clientStore;
	private final AbstractRemoteService remoteService;
	private final RequestProcessorsCollection procColl;
	private final boolean useSSL;
	private final String host;
	private final int port;

	public DefaultServerContext(SessionsStorage sessionsStore, ClientsStorage clientStore,
			AbstractRemoteService remoteService, RequestProcessorsCollection reqProcColl, boolean useSSL, String host, int port) {
		this.sessionsStore = sessionsStore;
		this.clientStore = clientStore;
		this.remoteService = remoteService;
		this.procColl = reqProcColl;
		this.useSSL = useSSL;
		this.host = host;
		this.port = port;
	}

	@Override
	public AbstractRemoteService getRemoteService() {
		return remoteService;
	}

	@Override
	public SessionsStorage getSessionsStorage() {
		return sessionsStore;
	}

	@Override
	public ClientsStorage getClientsStorage() {
		return clientStore;
	}

	@Override
	public boolean isSSL() {
		return useSSL;
	}

	@Override
	public String getHost() {
		return host;
	}

	@Override
	public int getPort() {
		return port;
	}

	@Override
	public RequestProcessorsCollection getProcessorsCollection() {
		return procColl;
	}

}
