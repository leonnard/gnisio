package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public class SIODisconnectEvent extends GwtEvent<SIODisconnectEvent.SIODisconnectHandler> { 

  public interface HasSIODisconnectHandlers extends HasHandlers {
    HandlerRegistration addSIODisconnectHandler(SIODisconnectHandler handler);
  }

  public interface SIODisconnectHandler extends EventHandler {
    public void onSIODisconnect(SIODisconnectEvent event);
  }

  private static final Type<SIODisconnectHandler> TYPE = new Type<SIODisconnectHandler>();

  public static void fire(HasHandlers source, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIODisconnectEvent(connectionState));
  }

  public static Type<SIODisconnectHandler> getType() {
    return TYPE;
  }

  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIODisconnectEvent(net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.connectionState = connectionState;
  }

  protected SIODisconnectEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIODisconnectHandler> getAssociatedType() {
    return TYPE;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIODisconnectHandler handler) {
    handler.onSIODisconnect(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIODisconnectEvent other = (SIODisconnectEvent) obj;
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
    return "SIODisconnectEvent["
                 + connectionState
    + "]";
  }
}
