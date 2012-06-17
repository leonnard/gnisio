package net.gnisio.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import net.gnisio.server.clients.ClientConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.RpcTokenException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.server.rpc.RPC;
import com.google.gwt.user.server.rpc.RPCRequest;
import com.google.gwt.user.server.rpc.SerializationPolicy;
import com.google.gwt.user.server.rpc.SerializationPolicyLoader;
import com.google.gwt.user.server.rpc.SerializationPolicyProvider;
import com.google.gwt.user.server.rpc.UnexpectedException;
import com.mycila.event.Dispatcher;
import com.mycila.event.Dispatchers;
import com.mycila.event.Subscriber;
import com.mycila.event.Topic;
import com.mycila.event.Topics;

/**
 * Base interface for remote RPC service
 * 
 * @author c58
 * 
 */
public abstract class AbstractRemoteService implements SerializationPolicyProvider {
	private static final Logger LOG = LoggerFactory.getLogger(AbstractRemoteService.class);

	private static final ThreadLocal<ClientConnection> currentClient = new ThreadLocal<ClientConnection>();

	/**
	 * A cache of moduleBaseURL and serialization policy strong name to
	 * {@link SerializationPolicy}.
	 */
	private final Map<String, SerializationPolicy> serializationPolicyCache = new HashMap<String, SerializationPolicy>();

	/**
	 * Location of GWT application (for loading serialization police files)
	 */
	private final String baseAppPath;

	/**
	 * Push event dispatcher
	 */
	private final Dispatcher dispatcher;

	/**
	 * Constructor for knowing where GWT application located
	 * 
	 * @param basePath
	 */
	public AbstractRemoteService(String gwtAppLocation) {
		this.baseAppPath = gwtAppLocation;
		this.dispatcher = Dispatchers.broadcastUnordered();
	}

	/**
	 * Set local-thread client connection
	 * 
	 * @param conn
	 */
	public void setClientConnection(ClientConnection conn) {
		currentClient.set(conn);
	}

	/**
	 * Clear thread-local client connection. Must be paired with
	 * setClientConnection
	 */
	public void clearClientConnection() {
		currentClient.remove();
	}

	/**
	 * Return current session id
	 * 
	 * @return
	 */
	public String getSessionId() {
		return getClientConnection().getSessionId();
	}

	/**
	 * Return current client connection
	 * 
	 * @return
	 */
	public ClientConnection getClientConnection() {
		return currentClient.get();
	}

	/**
	 * Process some RPC request
	 * 
	 * @param data
	 * @return
	 */
	public String processPayload(String data) {
		ClientConnection client = getClientConnection();

		// First message by client must contain strongName and moduleName
		// that initialize client-server RPC session
		if (!client.isInitialized()) {
			// Split message by |
			String[] parts = data.split("\\|", 2);

			// Check splitting result
			if (parts.length != 2)
				return null;

			// Initialize RPC session
			client.initializeRPCSession(parts[0], parts[1]);
			LOG.debug("Initialize RPC session: " + parts[0] + "  " + parts[1]);
			return null;
		} else {
			// Get data pattern type: 0XXXXdata
			// Where 0 is identity of req/resp pattern;
			// XXXX is HEX unique id of request;
			// data... is a content
			String rrType = data.substring(0, 1);
			if (!rrType.equals("0"))
				return null;

			// Get request id
			String reqId = data.substring(1, 5);
			data = data.substring(5);
			String result = null;

			LOG.debug("Process RPC request with ID: " + reqId);

			// Do RPC invoke
			try {
				result = processRPCRequest(data);
			} catch (SerializationException e) {
				LOG.warn(e.toString() + e.getStackTrace());
			}

			// Return result
			return "0" + reqId + result;
		}
	}

	/**
	 * Process a call originating from the given request. Uses the
	 * {@link RPC#invokeAndEncodeResponse(Object, java.lang.reflect.Method, Object[])}
	 * method to do the actual work.
	 * 
	 * @param message
	 * @param outbound
	 * @throws SerializationException
	 *             if we cannot serialize the response
	 * @throws UnexpectedException
	 *             if the invocation throws a checked exception that is not
	 *             declared in the service method's signature
	 * @throws RuntimeException
	 *             if the service method throws an unchecked exception (the
	 *             exception will be the one thrown by the service)
	 */
	private String processRPCRequest(String data) throws SerializationException {
		String result = null;
		try {
			RPCRequest rpcRequest = RPC.decodeRequest(data, this.getClass(), this);

			result = RPC.invokeAndEncodeResponse(this, rpcRequest.getMethod(), rpcRequest.getParameters(),
					rpcRequest.getSerializationPolicy(), rpcRequest.getFlags());
			LOG.info("Encoded result: Flags(" + rpcRequest.getFlags() + ") " + result);
		} catch (IncompatibleRemoteServiceException ex) {
			LOG.info("An IncompatibleRemoteServiceException was thrown while processing this call.", ex);
			result = RPC.encodeResponseForFailure(null, ex);
		} catch (RpcTokenException tokenException) {
			LOG.info("An RpcTokenException was thrown while processing this call.", tokenException);
			result = RPC.encodeResponseForFailure(null, tokenException);
		}

		return result;
	}

	@Override
	public SerializationPolicy getSerializationPolicy(String moduleBaseURL, String strongName) {
		// Try to get policy from cache
		SerializationPolicy serializationPolicy = getCachedSerializationPolicy(moduleBaseURL, strongName);

		// Policy found in cache, return it
		if (serializationPolicy != null) {
			return serializationPolicy;
		}

		// Load policy and store it in cache
		serializationPolicy = doGetSerializationPolicy(moduleBaseURL, strongName);

		if (serializationPolicy == null) {
			// Failed to get the requested serialization policy; use the default
			LOG.warn("WARNING: Failed to get the SerializationPolicy '"
					+ strongName
					+ "' for module '"
					+ moduleBaseURL
					+ "'; a legacy, 1.3.3 compatible, serialization policy will be used.  You may experience SerializationExceptions as a result.");
			serializationPolicy = RPC.getDefaultSerializationPolicy();
		}

		// This could cache null or an actual instance. Either way we will not
		// attempt to lookup the policy again.
		putCachedSerializationPolicy(moduleBaseURL, strongName, serializationPolicy);

		return serializationPolicy;
	}

	/**
	 * Used by HybridServiceServlet.
	 * 
	 * @param strongName2
	 */
	static SerializationPolicy loadSerializationPolicy(String baseAppPath, String moduleBaseURL, String strongName) {
		String modulePath = null;
		if (moduleBaseURL != null) {
			try {
				modulePath = new URL(moduleBaseURL).getPath();
			} catch (MalformedURLException ex) {
				// log the information, we will default
				LOG.warn("Malformed moduleBaseURL: " + moduleBaseURL, ex);
			}
		}

		SerializationPolicy serializationPolicy = null;

		/*
		 * Check that the module path must be in the same web app as the servlet
		 * itself. If you need to implement a scheme different than this,
		 * override this method.
		 */
		if (modulePath == null) {
			String message = "ERROR: The module path requested, " + modulePath
					+ ", is not in the same web application as this servlet. "
					+ "  Your module may not be properly configured or your client and server code maybe out of date.";
			LOG.warn(message);
		} else {
			String serializationPolicyFilePath = SerializationPolicyLoader.getSerializationPolicyFileName(modulePath
					+ strongName);

			// Open the RPC resource file and read its contents.
			InputStream is = getResourceAsStream(baseAppPath + serializationPolicyFilePath);
			try {
				if (is != null) {
					try {
						serializationPolicy = SerializationPolicyLoader.loadFromStream(is, null);
						LOG.info("Serialization policy loaded successfully.");
					} catch (ParseException e) {
						LOG.error("ERROR: Failed to parse the policy file '" + serializationPolicyFilePath + "'", e);
					} catch (IOException e) {
						LOG.error("ERROR: Could not read the policy file '" + serializationPolicyFilePath + "'", e);
					}
				} else {
					String message = "ERROR: The serialization policy file '" + serializationPolicyFilePath
							+ "' was not found; did you forget to include it in this deployment?";
					LOG.error(message);
				}
			} finally {
				if (is != null) {
					try {
						is.close();
					} catch (IOException e) {
						// Ignore this error
					}
				}
			}
		}

		return serializationPolicy;
	}

	private static InputStream getResourceAsStream(String serializationPolicyFilePath) {
		File pol = new File(serializationPolicyFilePath);

		if (!pol.exists() || !pol.isFile() || !pol.canRead())
			return null;

		try {
			return new FileInputStream(pol);
		} catch (FileNotFoundException e) {
			return null;
		}
	}

	/**
	 * Gets the {@link SerializationPolicy} for given module base URL and strong
	 * name if there is one.
	 * 
	 * Override this method to provide a {@link SerializationPolicy} using an
	 * alternative approach.
	 * 
	 * @param moduleBaseURL
	 *            as specified in the incoming payload
	 * @param strongName
	 *            a strong name that uniquely identifies a serialization policy
	 *            file
	 * @param strongName2
	 * @return a {@link SerializationPolicy} for the given module base URL and
	 *         strong name, or <code>null</code> if there is none
	 */
	protected SerializationPolicy doGetSerializationPolicy(String moduleBaseURL, String strongName) {
		return loadSerializationPolicy(baseAppPath, moduleBaseURL, strongName);
	}

	private SerializationPolicy getCachedSerializationPolicy(String moduleBaseURL, String strongName) {
		synchronized (serializationPolicyCache) {
			return serializationPolicyCache.get(moduleBaseURL + strongName);
		}
	}

	private void putCachedSerializationPolicy(String moduleBaseURL, String strongName,
			SerializationPolicy serializationPolicy) {
		synchronized (serializationPolicyCache) {
			serializationPolicyCache.put(moduleBaseURL + strongName, serializationPolicy);
		}
	}

	/**
	 * Push the message to subscribers by given node
	 * 
	 * @param methodName
	 * @param node
	 * @param result
	 */
	protected <T> void publishMessage(Method method, T result, String node) {
		dispatcher.publish(Topic.topic(node), new Object[] { method, result, getClientConnection() });
	}

	/**
	 * Add the subscriber to given nodes
	 * 
	 * @param methodName
	 * @param node
	 */
	@SuppressWarnings("unchecked")
	protected void addSubscriber(String... nodes) {
		if (nodes != null && nodes.length > 0) {
			dispatcher.subscribe(Topics.anyOf(nodes), Object.class, (Subscriber<Object[]>) getClientConnection());
		}
	}

	/**
	 * Remove subscriber from given topics
	 * 
	 * @param nodes
	 */
	@SuppressWarnings("unchecked")
	protected void removeSubscriber(String... nodes) {
		dispatcher.unsubscribe(Topics.anyOf(nodes), (Subscriber<Object[]>) getClientConnection());
	}

	/**
	 * Fully remove subscriber
	 */
	@SuppressWarnings("unchecked")
	protected void removeSubscriber() {
		dispatcher.unsubscribe((Subscriber<Object[]>) getClientConnection());
	}

	/**
	 * Return method of this object. Neede for push methods
	 * 
	 * @param name
	 * @param response
	 * @return
	 */
	protected Method getMethod(String name, Class<?> response) {
		try {
			return getClass().getMethod(name, response, String.class);
		} catch (SecurityException e) {
			throw new RuntimeException(e);
		} catch (NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This method invoked when client connected
	 */
	public abstract void onClientConnected();

	/**
	 * This method invoked when client disconnected
	 */
	public abstract void onClientDisconnected();
}
