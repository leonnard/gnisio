package net.gnisio.server.impl;

import java.io.Serializable;
import java.util.Date;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.gnisio.server.SessionsStorage;
import net.gnisio.server.SocketIOManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MemorySessionsStorage implements SessionsStorage, Runnable {
	protected static final Logger LOG = LoggerFactory.getLogger(MemorySessionsStorage.class);

	private ConcurrentMap<String, DefaultSession> sessionsMap = new ConcurrentHashMap<String, DefaultSession>();

	private class DefaultSession implements Session {
		private static final long serialVersionUID = 4943435821783416155L;
		private final ConcurrentMap<Object, Object> data = new ConcurrentHashMap<Object, Object>();
		private final String id;
		private int priority = 0;
		private Date lastActivity;
		private String userAgent;

		public DefaultSession(String id, String userAgent) {
			this.id = id;
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
			LOG.debug("Session update activity: " + getId());
			lastActivity = new Date();
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

	public MemorySessionsStorage() {
		// Start session garbage collector
		SocketIOManager.scheduleSessionTimeoutTask(this);
	}

	private void removeSession(String sessionId) {
		LOG.debug("Session removed: " + sessionId);
		sessionsMap.remove(sessionId);
	}

	@Override
	public Session getSession(String id, String userAgent) {
		Session sess = sessionsMap.get(id);

		if (sess != null && userAgent.equals(sess.getUserAgent()))
			return sess;

		return null;
	}

	@Override
	public Session getSession(String id) {
		return sessionsMap.get(id);
	}

	@Override
	public Session createSession(String userAgent) {
		DefaultSession sess = new DefaultSession(SocketIOManager.generateString(64), userAgent);
		sessionsMap.put(sess.getId(), sess);
		return sess;
	}

	@Override
	public void resetClearTimer(String id) {
		if (sessionsMap.containsKey(id))
			sessionsMap.get(id).resetClearTimer();
	}

	@Override
	public void run() {
		Date startTime = new Date();
		long timeOut = (SocketIOManager.option.session_timeout * 1000);
		Iterator<Entry<String, DefaultSession>> it = sessionsMap.entrySet().iterator();

		while (it.hasNext()) {
			DefaultSession sess = it.next().getValue();

			if (sess.getLastActivityDate().getTime() + timeOut < startTime.getTime())
				removeSession(sess.getId());
		}
	}

}
