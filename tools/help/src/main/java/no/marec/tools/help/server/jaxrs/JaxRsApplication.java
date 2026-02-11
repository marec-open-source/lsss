package no.marec.tools.help.server.jaxrs;

import no.imr.tools.help.HelpSystemInfo;
import no.imr.tools.misc.JsonUtils;
import no.marec.tools.help.server.jaxrs.resources.RootResource;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import java.util.List;

public final class JaxRsApplication {
   private final List<String> helpDirs;
   private final List<ClassLoader> classLoaders;
   private final HelpSystemInfo helpSystemInfo;
   private final ResourceConfig resourceConfig;

   public JaxRsApplication(List<String> helpDirs, List<ClassLoader> classLoaders, HelpSystemInfo helpSystemInfo) {
      this.helpDirs = helpDirs;
      this.classLoaders = classLoaders;
      this.helpSystemInfo = helpSystemInfo;

      resourceConfig = new ResourceConfig()
            .property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true)
            .register(new AbstractBinder() {
               @Override
               protected void configure() {
                  bind(JaxRsApplication.this).to(JaxRsApplication.class);
               }
            })
            .register(RootResource.class)
            .register(new JacksonJsonProvider(JsonUtils.JSON_MAPPER))
            .register(ErrorMessageExceptionMapper.class);
   }

   public List<String> getHelpDirs() {
      return helpDirs;
   }

   public List<ClassLoader> getClassLoaders() {
      return classLoaders;
   }

   public HelpSystemInfo getHelpSystemInfo() {
      return helpSystemInfo;
   }

   public ResourceConfig getResourceConfig() {
      return resourceConfig;
   }
}
