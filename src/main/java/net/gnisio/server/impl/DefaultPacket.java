package net.gnisio.server.impl;

import net.gnisio.server.PacketsProcessor.Packet;
import net.gnisio.server.PacketsProcessor.ConnectionContext;

import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * Default immutable implementation of packet
 * @author c58
 *
 */
public class DefaultPacket implements Packet {
	private final ConnectionContext context;
	private final Object msg;
	private final ChannelHandlerContext ctx;

	public DefaultPacket(ChannelHandlerContext ctx, Object msg, ConnectionContext connContext) {
		this.ctx = ctx;
		this.msg = msg;
		this.context = connContext;
	}

	@Override
	public ChannelHandlerContext getCtx() {
		return ctx;
	}

	@Override
	public Object getMessage() {
		return msg;
	}

	@Override
	public ConnectionContext getContext() {
		return context;
	}

}
