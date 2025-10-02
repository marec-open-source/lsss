package no.imr.lsss.framework.packages;

import no.imr.lsss.framework.config.application.packages.pojo.CallbackInfo;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class PackageManager {
   public final LsssPackage lsssPackage = new LsssPackage("lsss", null);

   private final Map<String, LsssPackage> packageMap = new ConcurrentHashMap<>();

   public PackageManager() {
      addPackage(lsssPackage);
   }

   public @Nullable LsssPackage getPackage(String packageId) {
      return packageMap.get(packageId);
   }

   public Collection<LsssPackage> getPackages() {
      return packageMap.values();
   }

   public void addPackage(LsssPackage aPackage) {
      packageMap.put(aPackage.getId(), aPackage);
   }

   public void removePackage(LsssPackage aPackage) {
      packageMap.remove(aPackage.getId());
   }

   public void dispatchCallbackEvent(LsssCallbackEvent event) {
      getPackages().stream()
            .map(LsssPackage::getUserDefinedPackage)
            .filter(Objects::nonNull)
            .forEach(p -> {
               for (CallbackInfo callbackInfo : p.info.callbacks) {
                  if (callbackInfo.event.equals(event.name())) {
                     LsssAction action = p.uiInfoToLsssAction(callbackInfo);
                     if (action != null) {
                        action.run(new ActionArgument());
                     }
                  }
               }
            });
   }

   public void dispatchKeyStroke(String context, KeyStroke keyStroke, ActionArgument argument) {
      getPackages().stream()
            .map(pack -> pack.getKeyStrokeMap().get(context))
            .filter(Objects::nonNull)
            .map(map -> map.get(keyStroke))
            .filter(Objects::nonNull)
            .flatMap(Collection::stream)
            .filter(ActionExecutor::isEnabled)
            .forEach(task -> task.run(argument));
   }
}
