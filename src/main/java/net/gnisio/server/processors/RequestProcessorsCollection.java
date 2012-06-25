package net.gnisio.server.processors;

import net.gnisio.server.PacketsProcessor.Packet;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * It's processors collection interface used for invoking processors when HTTP
 * request is received.
 * 
 * PreProcessor invoked before any HTTP request (socket.io requests too). So you
 * can cancel any other processing of request. For example when session is not
 * authorized.
 * 
 * @author c58
 */
public interface RequestProcessorsCollection {

	/*
	 * Processor invokers
	 */
	void invokeRequestPreProcessor(HttpRequest req, HttpResponse resp, Packet packet)
			throws Exception;

	void invokeRequestProcessor(HttpRequest req, HttpResponse resp, Packet packet)
			throws Exception;

	void invokeRequestPostProcessor(HttpRequest req, HttpResponse resp, Packet packet)
			throws Exception;

	/*
	 * Processor adders
	 */
	void addProcessor(RequestProcessor processor, String regexp);

	void addPreProcessor(RequestProcessor processor, String regexp);

	void addPostProcessor(RequestProcessor processor, String regexp);
}
