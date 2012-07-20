package net.gnisio.server;

import java.io.Serializable;
import java.util.Date;

/**
 * Storage for sessions
 * 
 * @author c58
 * 
 */
public interface SessionsStorage {

	/**
	 * Serializable session interface
	 * 
	 * @author c58
	 */
	public interface Session extends Serializable {
		String getId();

		<T, Y extends Serializable> void put(T key, Y value);

		<T, Y extends Serializable> T get(Y key);

		<T extends Serializable> void remove(T key);

		/**
		 * Set session authority level. It may be used for setting up authority
		 * of the session
		 * 
		 * @param level
		 */
		void setAuthorityLevel(int level);

		int getAuthorityLevel();

		/**
		 * Return Date object of last activity time of the user within session
		 * 
		 * @return
		 */
		Date getLastActivityDate();
		
		/**
		 * Return user agent of the session
		 * @return
		 */
		String getUserAgent();
	}

	/**
	 * Return session by given Id and check that session used by given userAgent
	 * 
	 * @param value
	 * @return
	 */
	Session getSession(String id, String userAgent);
	
	/**
	 * Return session by given id without userAgent checcking
	 * @param id
	 * @return
	 */
	Session getSession(String id);

	/**
	 * Create new session
	 * 
	 * @return
	 */
	Session createSession(String userAgent);

	/**
	 * Reset cleanup timer of session with given Id
	 * 
	 * @param id
	 */
	void resetClearTimer(String id);

}
