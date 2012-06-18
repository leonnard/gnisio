package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import com.google.gwt.event.shared.HasHandlers;

public class SIOReconnectingEvent extends GwtEvent<SIOReconnectingEvent.SIOReconnectingHandler> { 

  public interface HasSIOReconnectingHandlers extends HasHandlers {
    HandlerRegistration addSIOReconnectingHandler(SIOReconnectingHandler handler);
  }

  public interface SIOReconnectingHandler extends EventHandler {
    public void onSIOReconnecting(SIOReconnectingEvent event);
  }

  private static final Type<SIOReconnectingHandler> TYPE = new Type<SIOReconnectingHandler>();

  public static void fire(HasHandlers source, int reconnectionDelay, int reconnectionAttempts, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIOReconnectingEvent(reconnectionDelay, reconnectionAttempts, connectionState));
  }

  public static Type<SIOReconnectingHandler> getType() {
    return TYPE;
  }

  int reconnectionDelay;
  int reconnectionAttempts;
  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIOReconnectingEvent(int reconnectionDelay, int reconnectionAttempts, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.reconnectionDelay = reconnectionDelay;
    this.reconnectionAttempts = reconnectionAttempts;
    this.connectionState = connectionState;
  }

  protected SIOReconnectingEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOReconnectingHandler> getAssociatedType() {
    return TYPE;
  }

  public int getReconnectionDelay() {
    return reconnectionDelay;
  }

  public int getReconnectionAttempts() {
    return reconnectionAttempts;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIOReconnectingHandler handler) {
    handler.onSIOReconnecting(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOReconnectingEvent other = (SIOReconnectingEvent) obj;
    if (reconnectionDelay != other.reconnectionDelay)
        return false;
    if (reconnectionAttempts != other.reconnectionAttempts)
        return false;
    if (connectionState == null) {
      if (other.connectionState != null)
        return false;
    } else if (!connectionState.equals(other.connectionState))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 23;
    hashCode = (hashCode * 37) + new Integer(reconnectionDelay).hashCode();
    hashCode = (hashCode * 37) + new Integer(reconnectionAttempts).hashCode();
    hashCode = (hashCode * 37) + (connectionState == null ? 1 : connectionState.hashCode());
    return hashCode;
  }

  @Override
  public String toString() {
    return "SIOReconnectingEvent["
                 + reconnectionDelay
                 + ","
                 + reconnectionAttempts
                 + ","
                 + connectionState
    + "]";
  }
}
