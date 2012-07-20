package net.gnisio.server;

import java.io.Serializable;

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
	}

	/**
	 * Return session by given Id
	 * 
	 * @param value
	 * @return
	 */
	Session getSession(String id);

	/**
	 * Create new session
	 * 
	 * @return
	 */
	Session createSession();

	/**
	 * Reset cleanup timer of session with given Id
	 * 
	 * @param id
	 */
	void resetClearTimer(String id);

}
