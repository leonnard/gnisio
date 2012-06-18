package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;

import com.google.gwt.event.shared.HasHandlers;

public class SIOConnectingEvent extends GwtEvent<SIOConnectingEvent.SIOConnectingHandler> { 

  public interface HasSIOConnectingHandlers extends HasHandlers {
    HandlerRegistration addSIOConnectingHandler(SIOConnectingHandler handler);
  }

  public interface SIOConnectingHandler extends EventHandler {
    public void onSIOConnecting(SIOConnectingEvent event);
  }

  private static final Type<SIOConnectingHandler> TYPE = new Type<SIOConnectingHandler>();

  public static void fire(HasHandlers source, java.lang.String transportName, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    source.fireEvent(new SIOConnectingEvent(transportName, connectionState));
  }

  public static Type<SIOConnectingHandler> getType() {
    return TYPE;
  }

  java.lang.String transportName;
  net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState;

  public SIOConnectingEvent(java.lang.String transportName, net.gnisio.client.wrapper.SocketIOClient.ConnectionState connectionState) {
    this.transportName = transportName;
    this.connectionState = connectionState;
  }

  protected SIOConnectingEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOConnectingHandler> getAssociatedType() {
    return TYPE;
  }

  public java.lang.String getTransportName() {
    return transportName;
  }

  public net.gnisio.client.wrapper.SocketIOClient.ConnectionState getConnectionState() {
    return connectionState;
  }

  @Override
  protected void dispatch(SIOConnectingHandler handler) {
    handler.onSIOConnecting(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOConnectingEvent other = (SIOConnectingEvent) obj;
    if (transportName == null) {
      if (other.transportName != null)
        return false;
    } else if (!transportName.equals(other.transportName))
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
    hashCode = (hashCode * 37) + (connectionState == null ? 1 : connectionState.hashCode());
    return hashCode;
  }

  @Override
  public String toString() {
    return "SIOConnectingEvent["
                 + transportName
                 + ","
                 + connectionState
    + "]";
  }
}
