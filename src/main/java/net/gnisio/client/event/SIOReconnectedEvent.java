package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import com.google.gwt.event.shared.HasHandlers;

public class SIOReconnectedEvent extends GwtEvent<SIOReconnectedEvent.SIOReconnectedHandler> { 

  public interface HasSIOReconnectedHandlers extends HasHandlers {
    HandlerRegistration addSIOReconnectedHandler(SIOReconnectedHandler handler);
  }

  public interface SIOReconnectedHandler extends EventHandler {
    public void onSIOReconnected(SIOReconnectedEvent event);
  }

  private static final Type<SIOReconnectedHandler> TYPE = new Type<SIOReconnectedHandler>();

  public static void fire(HasHandlers source, java.lang.String transportName, int resonnectionAttempts, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIOReconnectedEvent(transportName, resonnectionAttempts, connectionState));
  }

  public static Type<SIOReconnectedHandler> getType() {
    return TYPE;
  }

  java.lang.String transportName;
  int resonnectionAttempts;
  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIOReconnectedEvent(java.lang.String transportName, int resonnectionAttempts, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.transportName = transportName;
    this.resonnectionAttempts = resonnectionAttempts;
    this.connectionState = connectionState;
  }

  protected SIOReconnectedEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOReconnectedHandler> getAssociatedType() {
    return TYPE;
  }

  public java.lang.String getTransportName() {
    return transportName;
  }

  public int getResonnectionAttempts() {
    return resonnectionAttempts;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIOReconnectedHandler handler) {
    handler.onSIOReconnected(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOReconnectedEvent other = (SIOReconnectedEvent) obj;
    if (transportName == null) {
      if (other.transportName != null)
        return false;
    } else if (!transportName.equals(other.transportName))
      return false;
    if (resonnectionAttempts != other.resonnectionAttempts)
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
    hashCode = (hashCode * 37) + (transportName == null ? 1 : transportName.hashCode());
    hashCode = (hashCode * 37) + new Integer(resonnectionAttempts).hashCode();
    hashCode = (hashCode * 37) + (connectionState == null ? 1 : connectionState.hashCode());
    return hashCode;
  }

  @Override
  public String toString() {
    return "SIOReconnectedEvent["
                 + transportName
                 + ","
                 + resonnectionAttempts
                 + ","
                 + connectionState
    + "]";
  }
}
