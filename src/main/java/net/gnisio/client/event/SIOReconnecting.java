package net.gnisio.client.event;

import net.gnisio.client.wrapper.SocketIOClient.ConnectionState;

import com.gwtplatform.dispatch.annotation.GenEvent;
import com.gwtplatform.dispatch.annotation.Order;

@GenEvent
public class SIOReconnecting {
	@Order(1) int reconnectionDelay;
	@Order(2) int reconnectionAttempts;
	@Order(3) ConnectionState connectionState;
}
