package no.marec.lsss.api.internal;

import java.util.ServiceLoader;

/**
 * This is an implementation detail and is not part of the LSSS API.
 */
public interface InternalLsss {
   InternalLsss INSTANCE = ServiceLoader.load(InternalLsss.class).findFirst().orElseThrow();

   DevUtils devUtils();

   InternalLsssUtils lsssUtils();

   InternalObjectFactory objectFactory();

   InternalParameterFactory parameterFactory();
}
