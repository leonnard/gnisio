package net.gnisio.shared;

import com.google.gwt.user.client.rpc.AsyncCallback;

// For disabling error
public interface SocketIOServiceAsync {
	public <T> void handleEvent(PushEventType event, AsyncCallback<T> callback);
}
