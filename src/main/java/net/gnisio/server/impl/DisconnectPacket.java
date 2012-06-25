package net.gnisio.server.impl;

import org.jboss.netty.channel.ChannelHandlerContext;

import net.gnisio.server.PacketsProcessor.ConnectionContext;
import net.gnisio.server.PacketsProcessor.Packet;

/**
 * This packet put to queue when websocket connection disconnected
 * @author c58
 */
public class DisconnectPacket implements Packet {
	private final ConnectionContext connContext;

	public DisconnectPacket(ConnectionContext connContext) {
		this.connContext = connContext;
	}

	@Override
	public ChannelHandlerContext getCtx() {
		return null;
	}

	@Override
	public Object getMessage() {
		return null;
	}

	@Override
	public ConnectionContext getContext() {
		return connContext;
	}

}
