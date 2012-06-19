package net.gnisio.server.clients;

import java.util.List;

import net.gnisio.server.SocketIOFrame;

import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * This interface define abstraction of client connection
 * 
 * @author c58
 */
public interface ClientConnection {
	public enum State {
		CONNECTED, CONNECTING, DISCONNECTED
	}

	/**
	 * Return ID of client
	 * 
	 * @return
	 */
	String getId();

	/**
	 * Return id of current session
	 * 
	 * @return
	 */
	String getSessionId();

	/**
	 * Send multiple socket.io frames
	 * 
	 * @param resultFrames
	 */
	void sendFrames(List<SocketIOFrame> resultFrames);

	/**
	 * Send single socket.io frame
	 * 
	 * @param frame
	 */
	void sendFrame(SocketIOFrame frame);

	/**
	 * Set state of connection
	 * 
	 * @param state
	 */
	void setState(State state);

	/**
	 * Return current state of connection
	 * 
	 * @param state
	 */
	State getState();

	/**
	 * Set recent alive channel context
	 * 
	 * @param ctx
	 */
	void setCtx(ChannelHandlerContext ctx);

	/**
	 * Reset close and heartbeat timeout tasks
	 */
	void resetCleanupTimers();

	/**
	 * Stop all timeout tasks
	 */
	void stopCleanupTimers();

	/**
	 * Start task for sending heartbeat
	 */
	void startHeartbeatTask();

	/**
	 * Stop task for sending heartbeat
	 */
	void stopHeartbeatTask();

	/**
	 * Flush buffer of frames
	 * 
	 * @return
	 */
	List<SocketIOFrame> flushBuffer();

	/**
	 * Return true if buffer of frames not empty
	 * 
	 * @return
	 */
	boolean isBufferEmpty();

	/**
	 * Return CTX of client
	 * 
	 * @return
	 */
	ChannelHandlerContext getCtx();

	/**
	 * Return true if GWT RPC session intialized
	 * 
	 * @return
	 */
	boolean isInitialized();

	/**
	 * Initialize RPC session. Without this initialization, any push messages
	 * can't send to client and ignored.
	 * 
	 * @param strongName
	 * @param moduleName
	 */
	void initializeRPCSession(String strongName, String moduleName);

	String getStrongName();

	String getModuleName();
}
