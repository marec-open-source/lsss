package no.imr.tools.plugins;

import no.imr.tools.parameter.Name;

import java.util.ServiceLoader;
import java.util.stream.Stream;

/**
 * A service providing a {@link BasePlugin}.
 * <p>
 * Note that the service provider should be stateless.
 */
public abstract class BaseService {
   private final Name name;

   protected BaseService(Name name) {
      this.name = name;
   }

   public Name getName() {
      return name;
   }

   @Override
   public String toString() {
      return name.persistentName();
   }

   public boolean canBeUsed() {
      return true;
   }

   public static <T extends BaseService> Stream<T> getUsableServices(Class<T> clazz) {
      return ServiceLoader.load(clazz).stream()
            .map(ServiceLoader.Provider::get)
            .filter(BaseService::canBeUsed);
   }
}
