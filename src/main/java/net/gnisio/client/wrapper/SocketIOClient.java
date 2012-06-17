package net.gnisio.client.wrapper;

import net.gnisio.client.event.HasAllSIOHandlers;
import net.gnisio.client.event.SIOConnectedEvent;
import net.gnisio.client.event.SIOConnectedEvent.SIOConnectedHandler;
import net.gnisio.client.event.SIOConnectingEvent;
import net.gnisio.client.event.SIOConnectingEvent.SIOConnectingHandler;
import net.gnisio.client.event.SIOConnectionFailedEvent;
import net.gnisio.client.event.SIOConnectionFailedEvent.SIOConnectionFailedHandler;
import net.gnisio.client.event.SIODisconnectEvent;
import net.gnisio.client.event.SIODisconnectEvent.SIODisconnectHandler;
import net.gnisio.client.event.SIOMessageEvent;
import net.gnisio.client.event.SIOMessageEvent.SIOMessageHandler;
import net.gnisio.client.event.SIOReconnectFailedEvent;
import net.gnisio.client.event.SIOReconnectFailedEvent.SIOReconnectFailedHandler;
import net.gnisio.client.event.SIOReconnectedEvent;
import net.gnisio.client.event.SIOReconnectedEvent.SIOReconnectedHandler;
import net.gnisio.client.event.SIOReconnectingEvent;
import net.gnisio.client.event.SIOReconnectingEvent.SIOReconnectingHandler;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Wrapper for socket.io library versions 0.8.x
 * 
 * @author c58
 */
public class SocketIOClient extends HandlerManager implements HasAllSIOHandlers {

	/**
	 * Static inctance holder
	 */
	private static SocketIOClient INSTANCE;

	public static SocketIOClient getInstance() {
		if (INSTANCE == null)
			INSTANCE = new SocketIOClient();

		return INSTANCE;
	}

	/**
	 * JavaScript wrapper
	 * 
	 * @author c58
	 */
	private static final class SocketIOImpl extends JavaScriptObject {

		public static native SocketIOImpl create(SocketIOClient wrapper) /*-{
																			var socket = $wnd.io.connect();

																			socket.on('connect', function() {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onConnected()();
																			});
																			socket.on('connecting',	function(transportName) {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onConnecting(Ljava/lang/String;)(transportName);
																			});
																			socket.on('connect_failed', function() {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onConnectionFailed()();
																			});			
																			socket.on('message', function(msg) {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onMessage(Ljava/lang/String;)(msg);
																			});
																			socket.on('disconnect',	function() {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onDisconnect()();
																			});
																			socket.on('reconnect',	function(transport_type, reconnectionAttempts) {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onReconnected(Ljava/lang/String;I)(transport_type, reconnectionAttempts);
																			});
																			socket.on('reconnecting', function(reconnectionDelay,reconnectionAttempts) {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onReconnecting(II)(reconnectionDelay, reconnectionAttempts);
																			});
																			socket.on('reconnect_failed', function() {
																			wrapper.@net.gnisio.client.wrapper.SocketIOClient::onReconnectFailed();
																			});
																			
																			return socket;
																			}-*/;

		// Empty constructor special for GWT
		protected SocketIOImpl() {
		}

		/**
		 * Establishes a connection.
		 */
		public native void connect() /*-{
										this.connect();
										}-*/;

		/**
		 * Send a string of data
		 * 
		 * @param message
		 */
		public native void send(String message) /*-{
												this.send(message);
												}-*/;

		/**
		 * Closes the connection.
		 */
		public native void disconnect() /*-{
										this.disconnect();
										}-*/;
	}

	// State of socket connection
	public enum ConnectionState {
		UNKNOWN, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
	}

	// JS wrapped object
	private SocketIOImpl impl;

	// Current connection state
	private ConnectionState state = ConnectionState.DISCONNECTED;

	/**
	 * Default constructor that initialize HandlerManager. By default it not
	 * start connecting to the server
	 */
	public SocketIOClient() {
		super("Socket.IO");
	}

	/**
	 * Start connecting. It do nothing if we already have the connection with
	 * the server.
	 */
	public void connect() {
		if (impl == null)
			impl = SocketIOImpl.create(this);
	}

	/**
	 * Send a message to the server
	 * 
	 * @param message
	 * @return true if sending success, or false otherwise
	 */
	public boolean sendMessage(String message) {
		if (state == ConnectionState.CONNECTED) {
			impl.send(message);
			return true;
		}

		return false;
	}

	/**
	 * Return current connection state
	 * 
	 * @return
	 */
	public ConnectionState getConnectionState() {
		return state;
	}

	/*
	 * Wrapped events
	 */
	/**
	 * Fired when client connected to the server
	 */
	public void onConnected() {
		this.state = ConnectionState.CONNECTED;
		SIOConnectedEvent.fire(this, state);
	}

	/**
	 * Fired when a connection is attempted, passing the transport name.
	 * 
	 * @param transportName
	 */
	public void onConnecting(String transportName) {
		this.state = ConnectionState.CONNECTING;
		SIOConnectingEvent.fire(this, transportName, state);
	}

	/**
	 * Fired when the connection timeout occurs after the last connection
	 * attempt.
	 * 
	 * This only fires if the connectTimeout option is set. If the
	 * tryTransportsOnConnectTimeout option is set, this only fires once all
	 * possible transports have been tried.
	 */
	public void onConnectionFailed() {
		this.state = ConnectionState.DISCONNECTED;
		SIOConnectionFailedEvent.fire(this, state);
	}

	/**
	 * Fired when some message received
	 * 
	 * @param message
	 */
	public void onMessage(String message) {
		SIOMessageEvent.fire(this, message);
	}

	/**
	 * Fired when client disconnected from the server
	 */
	public void onDisconnect() {
		this.state = ConnectionState.DISCONNECTED;
		SIODisconnectEvent.fire(this, state);
	}

	/**
	 * Fired when the connection has been re-established. This only fires if the
	 * reconnect option is set.
	 * 
	 * @param transportName
	 * @param resonnectionAttempts
	 */
	public void onReconnected(String transportName, int resonnectionAttempts) {
		this.state = ConnectionState.CONNECTED;
		SIOReconnectedEvent.fire(this, transportName, resonnectionAttempts, state);
	}

	/**
	 * Fired when a reconnection is attempted, passing the next delay for the
	 * next reconnection.
	 * 
	 * @param reconnectionDelay
	 * @param reconnectionAttempts
	 */
	public void onReconnecting(int reconnectionDelay, int reconnectionAttempts) {
		this.state = ConnectionState.CONNECTING;
		SIOReconnectingEvent.fire(this, reconnectionDelay, reconnectionAttempts, state);
	}

	/**
	 * Fired when all reconnection attempts have failed and we where
	 * unsuccessful in reconnecting to the server.
	 */
	public void onReconnectFailed() {
		this.state = ConnectionState.DISCONNECTED;
		SIOReconnectFailedEvent.fire(this, state);
	}

	/**
	 * Event adders
	 */
	@Override
	public HandlerRegistration addSIOConnectedHandler(SIOConnectedHandler handler) {
		return addHandler(SIOConnectedEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIOConnectingHandler(SIOConnectingHandler handler) {
		return addHandler(SIOConnectingEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIOConnectionFailedHandler(SIOConnectionFailedHandler handler) {
		return addHandler(SIOConnectionFailedEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIODisconnectHandler(SIODisconnectHandler handler) {
		return addHandler(SIODisconnectEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIOMessageHandler(SIOMessageHandler handler) {
		return addHandler(SIOMessageEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIOReconnectedHandler(SIOReconnectedHandler handler) {
		return addHandler(SIOReconnectedEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIOReconnectFailedHandler(SIOReconnectFailedHandler handler) {
		return addHandler(SIOReconnectFailedEvent.getType(), handler);
	}

	@Override
	public HandlerRegistration addSIOReconnectingHandler(SIOReconnectingHandler handler) {
		return addHandler(SIOReconnectingEvent.getType(), handler);
	}
}
