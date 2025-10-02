package no.imr.korona.util.echogram;

import no.imr.korona.data.datamanager.DefaultDataConfiguration;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;

public final class DefaultEchogramZSettings extends EchogramZSettings {
   public DefaultEchogramZSettings() {
      super(new DefaultDataConfiguration());
   }

   @Override
   public DepthTransform getDepthTransform() {
      return IdentityDepthTransform.INSTANCE;
   }
}
