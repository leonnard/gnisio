package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public class SIOConnectionFailedEvent extends GwtEvent<SIOConnectionFailedEvent.SIOConnectionFailedHandler> { 

  public interface HasSIOConnectionFailedHandlers extends HasHandlers {
    HandlerRegistration addSIOConnectionFailedHandler(SIOConnectionFailedHandler handler);
  }

  public interface SIOConnectionFailedHandler extends EventHandler {
    public void onSIOConnectionFailed(SIOConnectionFailedEvent event);
  }

  private static final Type<SIOConnectionFailedHandler> TYPE = new Type<SIOConnectionFailedHandler>();

  public static void fire(HasHandlers source, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIOConnectionFailedEvent(connectionState));
  }

  public static Type<SIOConnectionFailedHandler> getType() {
    return TYPE;
  }

  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIOConnectionFailedEvent(net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.connectionState = connectionState;
  }

  protected SIOConnectionFailedEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOConnectionFailedHandler> getAssociatedType() {
    return TYPE;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIOConnectionFailedHandler handler) {
    handler.onSIOConnectionFailed(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOConnectionFailedEvent other = (SIOConnectionFailedEvent) obj;
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
    return "SIOConnectionFailedEvent["
                 + connectionState
    + "]";
  }
}
