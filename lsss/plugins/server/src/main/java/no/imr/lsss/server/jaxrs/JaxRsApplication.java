package no.imr.lsss.server.jaxrs;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.LsssServerSettings;
import no.imr.lsss.server.jaxrs.resources.RootResource;
import no.imr.lsss.server.pojo.ApiCall;
import no.imr.tools.listening.ArgChangeManager;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.jakarta.rs.json.JacksonJsonProvider;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class JaxRsApplication {
   private final ResourceConfig resourceConfig;
   private final LSSS lsss;
   private final JsonMapper jsonMapper;
   private final ArgChangeManager<ApiCall> apiCallChangeManager = new ArgChangeManager<>();
   private final List<Runnable> onClose = new CopyOnWriteArrayList<>();

   public JaxRsApplication(LSSS lsss) {
      this.lsss = lsss;
      LsssServerSettings lsssServerSettings = lsss.getConfigurationManager().getAppMiscConf().getLsssServerConf().getLsssServerSettings();
      jsonMapper = JsonMapper.builder()
            .changeDefaultNullHandling(_ -> JsonSetter.Value.forValueNulls(Nulls.FAIL, Nulls.FAIL))
            .changeDefaultPropertyInclusion(_ -> JsonInclude.Value.construct(JsonInclude.Include.NON_NULL, JsonInclude.Include.NON_NULL))
            .disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            .configure(SerializationFeature.INDENT_OUTPUT, lsssServerSettings.prettyPrint.getBooleanValue())
            .configure(JsonWriteFeature.WRITE_NAN_AS_STRINGS, lsssServerSettings.quoteNonNumericNumbers.getBooleanValue())
            .build();
      resourceConfig = new ResourceConfig()
            .property(ServerProperties.RESPONSE_SET_STATUS_OVER_SEND_ERROR, true)
            .register(new AbstractBinder() {
               @Override
               protected void configure() {
                  bind(JaxRsApplication.this).to(JaxRsApplication.class);
               }
            })
            .register(RootResource.class)
            .register(new JacksonJsonProvider(jsonMapper))
            .register(InteractiveModeFilter.class)
            .register(ApiCallsFilter.class)
            .register(ErrorMessageExceptionMapper.class);
   }

   public ResourceConfig getResourceConfig() {
      return resourceConfig;
   }

   public LSSS getLSSS() {
      return lsss;
   }

   public JsonMapper getJsonMapper() {
      return jsonMapper;
   }

   public ArgChangeManager<ApiCall> getApiCallChangeManager() {
      return apiCallChangeManager;
   }

   public void addOnClose(Runnable runnable) {
      onClose.add(runnable);
   }

   public void close() {
      onClose.forEach(Runnable::run);
   }
}
