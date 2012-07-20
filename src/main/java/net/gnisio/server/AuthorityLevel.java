package net.gnisio.server;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for protecting GWT RPC methods
 * @author c58
 */

@Target( METHOD )
@Retention( RUNTIME )
public @interface AuthorityLevel {
	int value();
}
