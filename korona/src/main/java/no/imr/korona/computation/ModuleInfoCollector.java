package no.imr.korona.computation;

import no.imr.korona.plugins.ModulePlugin;
import no.imr.tools.parameter.Name;

import java.util.EnumSet;
import java.util.List;

public final class ModuleInfoCollector {
   private final ModulePlugin modulePlugin;
   private final ModuleManager moduleManager;

   ModuleInfoCollector(ModulePlugin modulePlugin, ModuleManager moduleManager) {
      this.modulePlugin = modulePlugin;
      this.moduleManager = moduleManager;
   }

   public Group group(String name) {
      return new Group(List.of(name));
   }

   public Group top() {
      return new Group(List.of());
   }

   public final class Group {
      private final List<String> grouping;

      private Group(List<String> grouping) {
         this.grouping = grouping;
      }

      public Group add(Class<? extends BaseModule> moduleClass, Name name, EnumSet<ModuleCategory> categories, String description) {
         return add(ModuleInfo.State.NORMAL, moduleClass, name, categories, description);
      }

      public Group add(ModuleInfo.State state, Class<? extends BaseModule> moduleClass, Name name, EnumSet<ModuleCategory> categories, String description) {
         moduleManager.addModuleInfo(new ModuleInfo(state, modulePlugin, moduleClass, name, categories, description, grouping));
         return this;
      }
   }
}
