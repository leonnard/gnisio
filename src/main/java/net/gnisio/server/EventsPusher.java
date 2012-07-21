package net.gnisio.server;

import net.gnisio.shared.PushEventType;

public interface EventsPusher {
	
	public <T> void pushSystemEvent(PushEventType event, T result, String node);
	
}
