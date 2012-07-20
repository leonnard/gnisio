package net.gnisio.server;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation for protecting GWT RPC methods
 * @author c58
 */

@Retention( RetentionPolicy.RUNTIME )
public @interface AuthorityLevel {
	int value();
}
