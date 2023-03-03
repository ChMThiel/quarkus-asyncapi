package io.quarkiverse.asyncapi.annotation.scanner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Helper to describe messages. If the message-Object is type-safe, the Annotation-Scanner can obtain the
 * message-description via reflection. But if the message is send as String (e.g. JSON-encoded), this is no possible.
 *
 * @author christiant
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Implementation {

    /**
     * Array of classes, contained in the message. E.g. if a Message <code>List&lt;A&lt;B&gt;&gt;</code> can be described as
     * <code>@Implementation({List.class, A.class, B.class})</code>
     *
     * @return
     */
    Class[] value();
}
