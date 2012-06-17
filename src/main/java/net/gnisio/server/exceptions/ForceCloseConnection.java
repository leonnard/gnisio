package net.gnisio.server.exceptions;

/**
 * This exception may throw any processor (not post-processor). It will be
 * caught in AbstractGnisioHandler, sent current response and close connection
 * 
 * @author c58
 */
public class ForceCloseConnection extends Exception {
	private static final long serialVersionUID = -1809206272457679184L;

}
