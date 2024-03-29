package net.gnisio.server.clients;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import net.gnisio.server.PacketsProcessor.ServerContext;
import net.gnisio.server.SocketIOFrame;
import net.gnisio.server.SocketIOManager;
import net.gnisio.shared.PushEventType;
import net.gnisio.shared.RPCUtils;

import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamWriter;
import com.mycila.event.Event;
import com.mycila.event.Subscriber;

/**
 * This abstract client connection realise full ClientConnection interface logic
 * and add one abstract method for sending frames correctly
 * 
 * @author c58
 */
public abstract class AbstractClient implements ClientConnection, Subscriber<Object[]> {
	protected static final Logger LOG = LoggerFactory.getLogger(AbstractClient.class);
	protected ChannelHandlerContext ctx;

	// Current state of client
	private State state = State.DISCONNECTED;
	private final String id;
	private final String session;

	// Cleanup and heartbeat taskss
	protected ScheduledFuture<?> cleanupTask;
	protected ScheduledFuture<?> heartbeatTimeoutTask;
	protected ScheduledFuture<?> heartbeatTask;

	// For removing myself
	private final ServerContext servContext;

	// Frames buffer
	private List<SocketIOFrame> buffer = new ArrayList<SocketIOFrame>();

	// RPC session variables
	private boolean init = false;
	private String strongName;
	private String moduleName;

	public AbstractClient(String id, String sessionId, ServerContext servContext) {
		this.id = id;
		this.session = sessionId;
		this.servContext = servContext;
	}

	@Override
	public synchronized void sendFrames(List<SocketIOFrame> resultFrames) {
		// If we can send data now - send it
		if (getState() == State.CONNECTED && ctx != null && ctx.getChannel().isConnected()) {
			LOG.debug("Connection is alive. Send data");

			doSendFrames(resultFrames);
		}
		// Otherwise add to buffer
		else {
			LOG.debug("Connection is NOT alive. Add data to buffer: " + resultFrames);
			buffer.addAll(resultFrames);
			LOG.debug("Buffer of client is: " + buffer.toString());
		}
	}

	@Override
	public synchronized void sendFrame(SocketIOFrame frame) {
		sendFrames(Arrays.asList(frame));
	}

	@Override
	public boolean isBufferEmpty() {
		LOG.debug("Checking buffer: client(" + this.toString() + "); buffer(" + buffer.toString() + "); isEmpty("
				+ buffer.isEmpty() + ")");
		return buffer.isEmpty();
	}

	@Override
	public synchronized List<SocketIOFrame> flushBuffer() {
		List<SocketIOFrame> buff = buffer;
		buffer = new ArrayList<SocketIOFrame>();
		return buff;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getSessionId() {
		return session;
	}

	@Override
	public void setCtx(ChannelHandlerContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public ChannelHandlerContext getCtx() {
		return this.ctx;
	}

	@Override
	public void resetCleanupTimers() {
		stopCleanupTimers();

		cleanupTask = SocketIOManager.scheduleClearTask(new Runnable() {
			@Override
			public void run() {
				LOG.debug("Timeout CLOSE. Close connection");

				if (ctx != null)
					ctx.getChannel().close().addListener(ChannelFutureListener.CLOSE);

				servContext.getClientsStorage().removeClient(AbstractClient.this);
			}
		});

		heartbeatTimeoutTask = SocketIOManager.scheduleHeartbeatTimeoutTask(new Runnable() {
			@Override
			public void run() {
				LOG.debug("Timeout HEARTBEAT. Set client as disconnected");

				try {
					servContext.getRemoteService().setClientConnection(AbstractClient.this);
					setState(State.DISCONNECTED);
				} finally {
					servContext.getRemoteService().clearClientConnection();
				}
			}
		});
	}

	@Override
	public void stopCleanupTimers() {
		if (cleanupTask != null)
			cleanupTask.cancel(true);

		if (heartbeatTimeoutTask != null)
			heartbeatTimeoutTask.cancel(true);
	}

	@Override
	public void startHeartbeatTask() {
		stopHeartbeatTask();

		heartbeatTask = SocketIOManager.scheduleHeartbeatTask(new Runnable() {
			@Override
			public void run() {
				LOG.debug("Send HEARTBEAT");
				sendFrame(SocketIOFrame.makeHeartbeat());
				resetCleanupTimers();
			}
		});
	}

	@Override
	public void stopHeartbeatTask() {
		if (heartbeatTask != null)
			heartbeatTask.cancel(true);
	}

	@Override
	public synchronized void setState(State state) {
		// Notify remoteService that client connected
		if (this.state == State.DISCONNECTED && state != State.DISCONNECTED) {
			this.state = state;
			servContext.getRemoteService().onClientConnected();
		}

		// Notify remoteService that client disconnected
		else if (this.state != State.DISCONNECTED && state == State.DISCONNECTED) {
			this.state = state;
			servContext.getRemoteService().onClientDisconnected();
		} else
			this.state = state;
	}

	@Override
	public void onEvent(Event<Object[]> event) throws Exception {
		LOG.info("Start pushing event");
		Object[] source = event.getSource();

		// Do nothing if this publisher is subscriber too
		if (source[2] == this || !isInitialized())
			return;

		// Get PushEventType and result object
		PushEventType eventType = (PushEventType) source[0];
		String eventCanonicalName = eventType.getClass().getCanonicalName() + "." + eventType.toString();
		Object result = source[1];

		// Get serialization policy
		SerializationPolicy serializationPolicy = servContext.getRemoteService().getSerializationPolicy(
				getModuleName(), getStrongName());

		// Encode result
		ServerSerializationStreamWriter stream = new ServerSerializationStreamWriter(serializationPolicy);
		stream.setFlags(1);
		stream.prepareToWrite();

		if (result != null && result.getClass() != void.class)
			stream.serializeValue(result, result.getClass());

		// Create response message
		String responsePayload = "1" + RPCUtils.hexToLength(Integer.toHexString(eventCanonicalName.length()), 2)
				+ eventCanonicalName + "//OK" + stream.toString();

		LOG.debug("Send push result to event: " + eventCanonicalName + ". Response payload to send: " + responsePayload);

		// Send response if RPC connection initialized
		sendFrame(SocketIOFrame.makeMessage(responsePayload));
	}

	@Override
	public synchronized State getState() {
		return state;
	}

	@Override
	public synchronized boolean isInitialized() {
		return init;
	}

	@Override
	public synchronized void initializeRPCSession(String strongName, String moduleName) {
		init = true;
		this.strongName = strongName;
		this.moduleName = moduleName;
	}

	@Override
	public String getStrongName() {
		return strongName;
	}

	@Override
	public String getModuleName() {
		return moduleName;
	}

	@Override
	public String toString() {
		return " [ ID:" + id + "; SessionID:" + session + "; State: " + state + "; Buffer: " + buffer + " ] ";
	}

	/**
	 * Abstract method for sending frames
	 * 
	 * @param resultFrames
	 */
	protected abstract void doSendFrames(List<SocketIOFrame> resultFrames);
}
