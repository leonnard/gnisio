package net.gnisio.client.event;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;

public class SIOMessageEvent extends GwtEvent<SIOMessageEvent.SIOMessageHandler> { 

  public interface HasSIOMessageHandlers extends HasHandlers {
    HandlerRegistration addSIOMessageHandler(SIOMessageHandler handler);
  }

  public interface SIOMessageHandler extends EventHandler {
    public void onSIOMessage(SIOMessageEvent event);
  }

  private static final Type<SIOMessageHandler> TYPE = new Type<SIOMessageHandler>();

  public static void fire(HasHandlers source, java.lang.String message) {
    source.fireEvent(new SIOMessageEvent(message));
  }

  public static Type<SIOMessageHandler> getType() {
    return TYPE;
  }

  java.lang.String message;

  public SIOMessageEvent(java.lang.String message) {
    this.message = message;
  }

  protected SIOMessageEvent() {
    // Possibly for serialization.
  }

  @Override
  public Type<SIOMessageHandler> getAssociatedType() {
    return TYPE;
  }

  public java.lang.String getMessage() {
    return message;
  }

  @Override
  protected void dispatch(SIOMessageHandler handler) {
    handler.onSIOMessage(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
        return true;
    if (obj == null)
        return false;
    if (getClass() != obj.getClass())
        return false;
    SIOMessageEvent other = (SIOMessageEvent) obj;
    if (message == null) {
      if (other.message != null)
        return false;
    } else if (!message.equals(other.message))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 23;
    hashCode = (hashCode * 37) + (message == null ? 1 : message.hashCode());
    return hashCode;
  }

  @Override
  public String toString() {
    return "SIOMessageEvent["
                 + message
    + "]";
  }
}
