package no.imr.lsss.framework;

import no.imr.lsss.framework.extensions.ExtensionServiceCollection;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.Utils;
import no.imr.tools.plugins.BaseService;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public final class ServiceCollection {
   private final @Nullable ExtensionServiceCollection extensionServiceCollection;
   private final List<FeatureService> allFeatureServices;
   private List<FeatureService> featureServices;

   public ServiceCollection(List<FeatureService> featureServices) {
      extensionServiceCollection = null;
      allFeatureServices = featureServices;
      this.featureServices = featureServices;
   }

   public ServiceCollection() {
      extensionServiceCollection = Utils.isTestRun() ? null : new ExtensionServiceCollection();
      allFeatureServices = Stream.concat(
                  BaseService.getUsableServices(FeatureService.class),
                  extensionServiceCollection != null ? extensionServiceCollection.services() : Stream.of()
            )
            .sorted(Utils.comparingIgnoringCase(service -> service instanceof BaseSystemFeatureService ? "" : service.getName().displayName()))
            .toList();
      featureServices = allFeatureServices;
   }

   public void removeFeatures(Set<String> deactivatedFeatures) {
      if (deactivatedFeatures.isEmpty()) {
         return;
      }
      featureServices = allFeatureServices.stream()
            .filter(service -> !deactivatedFeatures.contains(service.getName().persistentName()))
            .toList();
   }

   public List<FeatureService> getAllFeatureServices() {
      return allFeatureServices;
   }

   public List<FeatureService> getFeatureServices() {
      return featureServices;
   }

   public void close() {
      if (extensionServiceCollection != null) {
         extensionServiceCollection.close();
      }
   }
}
