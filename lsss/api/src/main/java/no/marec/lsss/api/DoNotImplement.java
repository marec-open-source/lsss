package no.marec.lsss.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation on types that must not be implemented when developing an LSSS plugin.
 * Instead, instances of these types will be provided by the LSSS framework
 * to the plugin, either as arguments to methods on types that should be implemented
 * or as return values of methods called from the plugin.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DoNotImplement {
}
