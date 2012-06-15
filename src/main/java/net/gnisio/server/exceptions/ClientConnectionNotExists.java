package net.gnisio.server.exceptions;

public class ClientConnectionNotExists extends Exception {

	private static final long serialVersionUID = -7394647557160193038L;

	public ClientConnectionNotExists(String uuid) {
		super(uuid);
	}
}
