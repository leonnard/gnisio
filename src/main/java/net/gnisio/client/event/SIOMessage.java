package net.gnisio.client.event;

import com.gwtplatform.dispatch.annotation.GenEvent;
import com.gwtplatform.dispatch.annotation.Order;

@GenEvent
public class SIOMessage {
	@Order(1) String message;
}
