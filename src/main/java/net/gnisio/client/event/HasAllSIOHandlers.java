package net.gnisio.client.event;

import net.gnisio.client.event.SIOConnectedEvent.HasSIOConnectedHandlers;
import net.gnisio.client.event.SIOConnectingEvent.HasSIOConnectingHandlers;
import net.gnisio.client.event.SIOConnectionFailedEvent.HasSIOConnectionFailedHandlers;
import net.gnisio.client.event.SIODisconnectEvent.HasSIODisconnectHandlers;
import net.gnisio.client.event.SIOMessageEvent.HasSIOMessageHandlers;
import net.gnisio.client.event.SIOReconnectFailedEvent.HasSIOReconnectFailedHandlers;
import net.gnisio.client.event.SIOReconnectedEvent.HasSIOReconnectedHandlers;
import net.gnisio.client.event.SIOReconnectingEvent.HasSIOReconnectingHandlers;

public interface HasAllSIOHandlers extends HasSIOConnectedHandlers, HasSIOConnectingHandlers,
		HasSIOConnectionFailedHandlers, HasSIODisconnectHandlers, HasSIOMessageHandlers, HasSIOReconnectedHandlers,
		HasSIOReconnectFailedHandlers, HasSIOReconnectingHandlers {

}
