package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import com.google.gwt.event.shared.HasHandlers;

public class SIOReconnectFailedEvent extends GwtEvent<SIOReconnectFailedEvent.SIOReconnectFailedHandler> { 

  public interface HasSIOReconnectFailedHandlers extends HasHandlers {
    HandlerRegistration addSIOReconnectFailedHandler(SIOReconnectFailedHandler handler);
  }

  public interface SIOReconnectFailedHandler extends EventHandler {
    public void onSIOReconnectFailed(SIOReconnectFailedEvent event);
  }

  private static final Type<SIOReconnectFailedHandler> TYPE = new Type<SIOReconnectFailedHandler>();

  public static void fire(HasHandlers source, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIOReconnectFailedEvent(connectionState));
  }

  public static Type<SIOReconnectFailedHandler> getType() {
    return TYPE;
  }

  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIOReconnectFailedEvent(net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.connectionState = connectionState;
  }

  protected SIOReconnectFailedEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOReconnectFailedHandler> getAssociatedType() {
    return TYPE;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIOReconnectFailedHandler handler) {
    handler.onSIOReconnectFailed(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOReconnectFailedEvent other = (SIOReconnectFailedEvent) obj;
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
    hashCode = (hashCode * 37) + (connectionState == null ? 1 : connectionState.hashCode());
    return hashCode;
  }

  @Override
  public String toString() {
    return "SIOReconnectFailedEvent["
                 + connectionState
    + "]";
  }
}
