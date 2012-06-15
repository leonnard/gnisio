package net.gnisio.server.processors;

import net.gnisio.server.exceptions.StopRequestProcessing;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * It's processors collection interface used for invoking processors
 * when HTTP request is received.
 * 
 * PreProcessor invoked before any HTTP request (socket.io requests too). So you can cancel any other
 * processing of request. For example when session is not authorized.
 * 
 * @author c58
 */
public interface RequestProcessorsCollection {

	/**
	 * Invoke preprocessor for given request
	 * @param req
	 * @param resp
	 * @param ctx 
	 * @param sessionId
	 * @throws StopRequestProcessing
	 * @throws Throwable 
	 */
	void invokeRequestPreProcessor(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx, String sessionId) throws Exception;

	void invokeRequestProcessor(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx, String sessionId) throws Exception;

	void invokeRequestPostProcessor(HttpRequest req, HttpResponse resp, ChannelHandlerContext ctx, String sessionId) throws Exception;
	
	void addProcessor(RequestProcessor processor, String regexp);
	
	void addPreProcessor(RequestProcessor processor, String regexp);

	void addPostProcessor(RequestProcessor processor, String regexp);
}
