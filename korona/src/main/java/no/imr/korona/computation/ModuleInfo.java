package no.imr.korona.computation;

import no.imr.korona.plugins.ModulePlugin;
import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.Name;

import java.util.List;
import java.util.Set;

/**
 * Info about one module.
 *
 * @see BaseModule
 * @see ModuleManager
 */
public record ModuleInfo(
      State state,
      ModulePlugin modulePlugin,
      Class<? extends BaseModule> moduleClass,
      Name name,
      Set<ModuleCategory> categories,
      String description,
      List<String> grouping
) {
   @Override
   public String toString() {
      return getPersistentName();
   }

   public String getPersistentName() {
      return name.persistentName();
   }

   public String getDisplayName() {
      return name.displayName();
   }

   public HelpID getHelpID() {
      return modulePlugin.getHelpSet().createHelpID(getPersistentName());
   }

   public boolean isDeprecated() {
      return state == State.DEPRECATED;
   }

   public boolean isBeta() {
      return state == State.BETA;
   }

   public enum State {
      NORMAL, BETA, DEPRECATED
   }
}
