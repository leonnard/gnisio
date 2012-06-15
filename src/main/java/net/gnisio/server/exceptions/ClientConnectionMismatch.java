package net.gnisio.server.exceptions;


public class ClientConnectionMismatch extends Exception {
	private static final long serialVersionUID = -2172363103130231671L;

	@SuppressWarnings("rawtypes")
	public ClientConnectionMismatch(Class actual, Class expect) {
		super("Expected client type: "+expect.toString()+", actual: "+actual.toString());
	}
}
