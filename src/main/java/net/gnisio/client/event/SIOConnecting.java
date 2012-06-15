package net.gnisio.client.event;

import net.gnisio.client.wrapper.SocketIOClient.ConnectionState;

import com.gwtplatform.dispatch.annotation.GenEvent;
import com.gwtplatform.dispatch.annotation.Order;

@GenEvent
public class SIOConnecting {
	@Order(1) String transportName;
	@Order(2) ConnectionState connectionState;
}
