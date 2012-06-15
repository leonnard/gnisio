package net.gnisio.server.exceptions;

/**
 * This exception may be throw in Pre-Processor. It will be caught and
 * connection closed (response NOT sent)
 * 
 * @author c58
 */
public class StopRequestProcessing extends Exception {
	private static final long serialVersionUID = 6777360253010734545L;

}
