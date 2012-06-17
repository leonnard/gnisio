package net.gnisio.client.wrapper;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import net.gnisio.client.event.SIOConnectedEvent;
import net.gnisio.client.event.SIOConnectedEvent.SIOConnectedHandler;
import net.gnisio.client.event.SIODisconnectEvent;
import net.gnisio.client.event.SIODisconnectEvent.SIODisconnectHandler;
import net.gnisio.client.event.SIOMessageEvent;
import net.gnisio.client.event.SIOMessageEvent.SIOMessageHandler;
import net.gnisio.client.event.SIOReconnectedEvent;
import net.gnisio.client.event.SIOReconnectedEvent.SIOReconnectedHandler;
import net.gnisio.shared.RPCUtils;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.Header;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Timer;

/**
 * Controller of socket connection with GWT RPC server
 * 
 * @author c58
 */
public class SocketRPCController implements SIOConnectedHandler, SIOReconnectedHandler, SIODisconnectHandler,
		SIOMessageHandler {

	/**
	 * Static instance holder
	 */
	private static SocketRPCController INSTANCE = null;

	public static SocketRPCController getInstance() {
		if (INSTANCE == null)
			INSTANCE = new SocketRPCController();

		return INSTANCE;
	}

	// Active connection
	private final SocketIOClient socket;

	// Map with push callback objects
	private Map<String, RequestCallback> pushMethodsCallback = new HashMap<String, RequestCallback>();
	private Map<Integer, RequestCallback> requestCallback = new HashMap<Integer, RequestCallback>();

	// Serialization policy string
	private String serizlizationPolicy;

	// Requests queue
	private boolean initiated = false;
	private final LinkedList<String> waitQueue = new LinkedList<String>();

	/**
	 * Initialize socket connection
	 */
	public SocketRPCController() {
		// Create socket connection
		socket = SocketIOClient.getInstance();

		// Add handlers
		socket.addSIOConnectedHandler(this);
		socket.addSIOReconnectedHandler(this);
		socket.addSIODisconnectHandler(this);
		socket.addSIOMessageHandler(this);
		socket.connect();
	}

	/**
	 * Register a push method in controller. Only one async handler may process
	 * one push method. If push method with given name already registered it do
	 * nothing.
	 * 
	 * @param methodName
	 * @param callback
	 */
	public void registerPushMethod(String methodName, RequestCallback callback) {
		// Get method name(it was ToweeService_Proxy.methodName)
		methodName = methodName.substring(methodName.lastIndexOf('.') + 1);

		// Put it to map
		if (!pushMethodsCallback.containsKey(methodName))
			pushMethodsCallback.put(methodName, callback);
	}

	/**
	 * Send an RPC request
	 * 
	 * @param requestId
	 * @param data
	 * @param callback
	 */
	public void sendRPCRequest(int requestId, String data, RequestCallback callback) {
		if (requestCallback.containsKey(requestId))
			throw new RuntimeException("Try to send request with not unique id!");

		// Register callback
		requestCallback.put(requestId, callback);

		// Create request data with stucture: 0XXXXdata...
		// Where 0 is identity of req/resp pattern;
		// XXXX is HEX unique id of request;
		// data... is a content
		String requestData = "0" + RPCUtils.hexToLength(Integer.toHexString(requestId), 4) + data;

		if (!initiated)
			waitQueue.add(requestData);
		else
			socket.sendMessage(requestData);
	}

	/**
	 * Send init message on connect
	 */
	private void sendInitMessage() {
		// Need a timer for JSONP transport
		// I don't understand why, but if send init message immediately after
		// connect event
		// it don't really send a message by JSONP transport (XHR-polling and
		// WebSocket fine)
		new Timer() {
			@Override
			public void run() {
				// Send init message
				socket.sendMessage(serizlizationPolicy + "|" + GWT.getModuleBaseURL());

				// Free requests queue
				String req = null;
				while ((req = waitQueue.poll()) != null)
					socket.sendMessage(req);

				// Set initiated flag
				initiated = true;
			}
		}.schedule(1);
	}

	/**
	 * Process received message
	 * 
	 * @param message
	 */
	public void processMessage(String message) {
		// Get response type
		int respType = Integer.parseInt(message.substring(0, 1));

		// It's a request/response pattern
		if (respType == 0) {
			int requestId = Integer.parseInt(message.substring(1, 5), 16);

			if (requestCallback.containsKey(requestId)) {
				// Get callback
				RequestCallback callback = requestCallback.get(requestId);
				requestCallback.remove(requestId);

				// Get data
				final String data = message.substring(5);

				// Run callback
				callback.onResponseReceived(null, new Response() {
					public String getHeader(String header) {
						return null;
					}

					public Header[] getHeaders() {
						return null;
					}

					public String getHeadersAsString() {
						return null;
					}

					public int getStatusCode() {
						return Response.SC_OK;
					}

					public String getStatusText() {
						return null;
					}

					public String getText() {
						return data;
					}
				});
			}
		}

		// It's a push message
		else if (respType == 1) {
			// Get methodName
			int nameLength = Integer.parseInt(message.substring(1, 3), 16);
			String methodName = message.substring(3, nameLength + 3);

			// Get data
			final String data = message.substring(nameLength + 3);

			// Invoke method
			if (pushMethodsCallback.containsKey(methodName)) {
				pushMethodsCallback.get(methodName).onResponseReceived(null, new Response() {
					public String getHeader(String header) {
						return null;
					}

					public Header[] getHeaders() {
						return null;
					}

					public String getHeadersAsString() {
						return null;
					}

					public int getStatusCode() {
						return Response.SC_OK;
					}

					public String getStatusText() {
						return null;
					}

					public String getText() {
						return data;
					}
				});
			}
		}
	}

	/**
	 * Set the serialization policy string for serializing RPC calls by GWT
	 * 
	 * @param serializationPolicy
	 */
	public void setSerializationPolicy(String serializationPolicy) {
		this.serizlizationPolicy = serializationPolicy;
	}

	/**
	 * On connected events send init message
	 */
	@Override
	public void onSIOReconnected(SIOReconnectedEvent event) {
		sendInitMessage();
	}

	@Override
	public void onSIOConnected(SIOConnectedEvent event) {
		sendInitMessage();
	}

	@Override
	public void onSIODisconnect(SIODisconnectEvent event) {
		initiated = false;
	}

	@Override
	public void onSIOMessage(SIOMessageEvent event) {
		processMessage(event.getMessage());
	}
}
