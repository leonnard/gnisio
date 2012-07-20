package net.gnisio.shared;

import com.google.gwt.user.client.rpc.IsSerializable;

@SuppressWarnings("serial")
public final class LowAuthorityLevel extends Exception implements IsSerializable {

	private static final String DEFAULT_MESSAGE = "Low authority level";

	/**
	 * Constructor used by RPC serialization. Note that the client side code
	 * will always get a generic error message.
	 */
	public LowAuthorityLevel() {
		super(DEFAULT_MESSAGE);
	}

	/**
	 * Constructs an instance with the specified message.
	 */
	public LowAuthorityLevel(String msg) {
		super(DEFAULT_MESSAGE + " ( " + msg + " )");
	}

	/**
	 * Constructs an instance with the specified message and cause.
	 */
	public LowAuthorityLevel(String msg, Throwable cause) {
		super(msg, cause);
	}
}