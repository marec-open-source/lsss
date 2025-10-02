package no.imr.lsss.framework.extensions;

import no.marec.lsss.api.internal.DevUtils;
import no.marec.lsss.api.internal.InternalLsss;
import no.marec.lsss.api.internal.InternalLsssUtils;
import no.marec.lsss.api.internal.InternalObjectFactory;
import no.marec.lsss.api.internal.InternalParameterFactory;

public final class InternalLsssImpl implements InternalLsss {
   public InternalLsssImpl() {
   }

   @Override
   public DevUtils devUtils() {
      return DevUtilsImpl.INSTANCE;
   }

   @Override
   public InternalLsssUtils lsssUtils() {
      return InternalLsssUtilsImpl.INSTANCE;
   }

   @Override
   public InternalObjectFactory objectFactory() {
      return InternalObjectFactoryImpl.INSTANCE;
   }

   @Override
   public InternalParameterFactory parameterFactory() {
      return InternalParameterFactoryImpl.INSTANCE;
   }
}
