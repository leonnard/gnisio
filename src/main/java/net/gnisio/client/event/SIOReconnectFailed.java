package net.gnisio.client.event;

import net.gnisio.client.wrapper.SocketIOClient.ConnectionState;

import com.gwtplatform.dispatch.annotation.GenEvent;
import com.gwtplatform.dispatch.annotation.Order;

@GenEvent
public class SIOReconnectFailed {
	@Order(1) ConnectionState connectionState;
}
