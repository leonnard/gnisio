package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public class SIOConnectedEvent extends GwtEvent<SIOConnectedEvent.SIOConnectedHandler> { 

  public interface HasSIOConnectedHandlers extends HasHandlers {
    HandlerRegistration addSIOConnectedHandler(SIOConnectedHandler handler);
  }

  public interface SIOConnectedHandler extends EventHandler {
    public void onSIOConnected(SIOConnectedEvent event);
  }

  private static final Type<SIOConnectedHandler> TYPE = new Type<SIOConnectedHandler>();

  public static void fire(HasHandlers source, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIOConnectedEvent(connectionState));
  }

  public static Type<SIOConnectedHandler> getType() {
    return TYPE;
  }

  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIOConnectedEvent(net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.connectionState = connectionState;
  }

  protected SIOConnectedEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOConnectedHandler> getAssociatedType() {
    return TYPE;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIOConnectedHandler handler) {
    handler.onSIOConnected(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOConnectedEvent other = (SIOConnectedEvent) obj;
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
    return "SIOConnectedEvent["
                 + connectionState
    + "]";
  }
}
