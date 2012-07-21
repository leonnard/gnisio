package net.gnisio.client.wrapper;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention( RetentionPolicy.RUNTIME )
public @interface PushClass {
	Class<?> value();
}
