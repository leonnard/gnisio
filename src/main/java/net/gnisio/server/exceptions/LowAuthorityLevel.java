package net.gnisio.server.exceptions;

import com.google.gwt.user.client.rpc.IsSerializable;

public class LowAuthorityLevel extends Exception implements IsSerializable {
	private static final long serialVersionUID = 1L;

	public LowAuthorityLevel(String msg) {
		super(msg);
	}
}
