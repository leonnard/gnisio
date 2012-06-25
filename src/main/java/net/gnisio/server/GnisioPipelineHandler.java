package net.gnisio.server;

import net.gnisio.server.PacketsProcessor.ConnectionContext;
import net.gnisio.server.impl.DefaultConnectionContext;
import net.gnisio.server.impl.DefaultPacket;
import net.gnisio.server.impl.DisconnectPacket;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

/**
 * This pipeline handler is simply receive the message and add it to
 * packet processor queue.
 * @author c58
 */
public class GnisioPipelineHandler extends SimpleChannelUpstreamHandler  {
	/**
	 * Packets processor instance
	 */
	private PacketsProcessor packetsProcessor;
	
	/**
	 * Current active packet context
	 */
	private ConnectionContext connContext;

	public GnisioPipelineHandler(PacketsProcessor packetProcessor) {
		this.packetsProcessor = packetProcessor;
	}
	
	@Override
	public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
		// Queue disconnect packet
		packetsProcessor.queueMessage( new DisconnectPacket(connContext) );
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		// Get the message
		Object msg = e.getMessage();
		
		// Create connection context if not created yet
		if(connContext == null)
			connContext = new DefaultConnectionContext();
		
		// Queue the packet
		packetsProcessor.queueMessage( new DefaultPacket(ctx, msg, connContext) );
	}
	
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
		Channel ch = e.getChannel();
		Throwable cause = e.getCause();
		if (cause instanceof TooLongFrameException) {
			// sendError(ctx, BAD_REQUEST);
			return;
		}

		cause.printStackTrace();
		if (ch.isConnected()) {
			// sendError(ctx, INTERNAL_SERVER_ERROR);
		}
	}
}
