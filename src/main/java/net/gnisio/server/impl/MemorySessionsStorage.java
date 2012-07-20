package net.gnisio.server.impl;

import java.io.Serializable;
import java.util.Date;
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
		private int priority = 0;
		private Date lastActivity;
		private String userAgent;

		public DefaultSession(String id, String userAgent) {
			this.id = id;
			this.clearTimer = SocketIOManager.scheduleSessionTimeoutTask(this);
			this.lastActivity = new Date();
			this.userAgent = userAgent;
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
			lastActivity = new Date();
		}

		@Override
		public void run() {
			MemorySessionsStorage.this.removeSession( getId() );
		}

		@Override
		public void setAuthorityLevel(int level) {
			this.priority = level;
		}

		@Override
		public int getAuthorityLevel() {
			return priority;
		}

		@Override
		public Date getLastActivityDate() {
			return lastActivity;
		}

		@Override
		public String getUserAgent() {
			return userAgent;
		}
		
	}

	private void removeSession(String sessionId) {
		sessionsMap.remove(sessionId);
	}
	
	@Override
	public Session getSession(String id, String userAgent) {
		Session sess = sessionsMap.get(id);
		
		if(sess != null && userAgent.equals( sess.getUserAgent() ))
			return sess;
		
		return null;
	}
	
	@Override
	public Session getSession(String id) {
		return sessionsMap.get(id);
	}

	@Override
	public Session createSession(String userAgent) {
		DefaultSession sess = new DefaultSession( SocketIOManager.generateString(64), userAgent );
		sessionsMap.put(sess.getId(), sess);
		return sess;
	}

	@Override
	public void resetClearTimer(String id) {
		if(sessionsMap.containsKey(id))
			sessionsMap.get(id).resetClearTimer();
	}

}
