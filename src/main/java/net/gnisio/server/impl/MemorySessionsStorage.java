package net.gnisio.server.impl;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;

import net.gnisio.server.SessionsStorage;
import net.gnisio.server.SocketIOManager;

public class MemorySessionsStorage implements SessionsStorage {
	
	private ConcurrentMap<String, DefaultSession> sessionsMap = new ConcurrentHashMap<String, DefaultSession>();
	
	private class DefaultSession implements Session, Runnable {
		private static final long serialVersionUID = 4943435821783416155L;
		private final ConcurrentMap<Object, Object> data = new ConcurrentHashMap<Object, Object>();
		private ScheduledFuture<?> clearTimer;
		private final String id;

		public DefaultSession(String id) {
			this.id = id;
			this.clearTimer = SocketIOManager.scheduleSessionTimeoutTask(this);
		}
		
		@Override
		public String getId() {
			return id;
		}

		@Override
		public <T, Y extends Serializable> void put(T key, Y value) {
			data.put(key, value);
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T, Y extends Serializable> T get(Y key) {
			return (T) data.get(key);
		}

		@Override
		public <T extends Serializable> void remove(T key) {
			data.remove(key);
		}
		
		public void resetClearTimer() {
			if(clearTimer != null)
				clearTimer.cancel(false);
			
			clearTimer = SocketIOManager.scheduleSessionTimeoutTask(this);
		}

		@Override
		public void run() {
			MemorySessionsStorage.this.removeSession( getId() );
		}
		
	}

	private void removeSession(String sessionId) {
		sessionsMap.remove(sessionId);
	}
	
	@Override
	public Session getSession(String id) {
		return sessionsMap.get(id);
	}

	@Override
	public Session createSession() {
		DefaultSession sess = new DefaultSession( SocketIOManager.generateString(64) );
		return sess;
	}

	@Override
	public void resetClearTimer(String id) {
		sessionsMap.get(id).resetClearTimer();
	}

}
