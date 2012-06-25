package net.gnisio.server;

import net.gnisio.server.clients.ClientConnection;
import net.gnisio.server.clients.ClientsStorage;
import net.gnisio.server.processors.RequestProcessorsCollection;
import net.gnisio.server.transports.Transport;

import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * This is interface for packet processing.
 * 
 * @author c58
 */
public interface PacketsProcessor {
	/**
	 * Represents packet of netty message
	 * @author c58
	 */
	public interface Packet {
		public ChannelHandlerContext getCtx();
		
		public Object getMessage();
		
		public ConnectionContext getContext();
	}
	
	/**
	 * This interface used for storing data about current
	 * active connection.
	 * 
	 * @author c58
	 */
	public interface ConnectionContext {
		public ClientConnection getClientConnection();
		
		public String getSessionId();
		
		public Transport getTransport();
		
		public void setClientConnection(ClientConnection connection);
		
		public void setSessionId(String sess);
		
		public void setTransport(Transport transport);
	}
	
	/**
	 * Server context contain storages and remote service 
	 * @author c58
	 */
	public interface ServerContext {
		public AbstractRemoteService getRemoteService();
		
		public SessionsStorage getSessionsStorage();
		
		public ClientsStorage getClientsStorage();
		
		public RequestProcessorsCollection getProcessorsCollection();
		
		public boolean isSSL();
		
		public String getHost();
		
		public int getPort();
	}
	
	/**
	 * Add new message for processing
	 * @param mess
	 */
	void queueMessage(Packet mess);
	
	/**
	 * Stop any processing of packets
	 */
	void stopProcessing();
}
