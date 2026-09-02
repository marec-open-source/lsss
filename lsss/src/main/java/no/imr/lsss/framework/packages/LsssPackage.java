package no.imr.lsss.framework.packages;

import no.imr.lsss.framework.config.application.packages.UserDefinedPackage;
import org.jspecify.annotations.Nullable;

import javax.swing.KeyStroke;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LsssPackage {
   static final LsssPackage NO_PACKAGE = new LsssPackage();
   public static final String KEY_STROKE_CONTEXT_ANYWHERE = "Anywhere";
   public static final String KEY_STROKE_CONTEXT_MAIN_WINDOW = "MainWindow";
   public static final String KEY_STROKE_CONTEXT_ANY_ECHOGRAM_MODULE = "AnyEchogramModule";

   private final String id;
   private final @Nullable UserDefinedPackage userDefinedPackage;
   private final Map<String, LsssAction> actionMap;
   private Map<String, Map<KeyStroke, ActionExecutor>> keyStrokeMap = Map.of();

   public LsssPackage(String id, @Nullable UserDefinedPackage userDefinedPackage) {
      this.id = id;
      this.userDefinedPackage = userDefinedPackage;
      actionMap = new ConcurrentHashMap<>();
   }

   private LsssPackage() {
      id = "";
      userDefinedPackage = null;
      actionMap = Map.of();
   }

   public String getId() {
      return id;
   }

   public String getLabel() {
      return userDefinedPackage != null ? userDefinedPackage.getEffectiveLabel() : id;
   }

   public @Nullable UserDefinedPackage getUserDefinedPackage() {
      return userDefinedPackage;
   }

   public @Nullable LsssAction getAction(String actionId) {
      return actionMap.get(actionId);
   }

   public Collection<LsssAction> getActions() {
      return actionMap.values();
   }

   public void addAction(LsssAction action) {
      action.setLsssPackage(this);
      actionMap.put(action.getId(), action);
   }

   public void removeAction(LsssAction action) {
      action.setLsssPackage(NO_PACKAGE);
      actionMap.remove(action.getId());
   }

   public Map<String, Map<KeyStroke, ActionExecutor>> getKeyStrokeMap() {
      return keyStrokeMap;
   }

   public void setKeyStrokeMap(Map<String, Map<KeyStroke, ActionExecutor>> keyStrokeMap) {
      this.keyStrokeMap = keyStrokeMap;
   }
}
