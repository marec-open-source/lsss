package no.imr.lsss.server;

import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.server.jaxrs.resources.lsss.PluginResource;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BaseService;
import org.jspecify.annotations.Nullable;

public abstract class ResourceService extends BaseService {
   protected ResourceService(Name name) {
      super(name);
   }

   public abstract @Nullable PluginResource getPluginResource(FeaturePlugin plugin);
}
