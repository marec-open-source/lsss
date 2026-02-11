package no.imr.korona.computation;

import no.imr.tools.logging.Log;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * A list of modules.
 *
 * @see BaseModule
 */
final class ModuleList {
   static final String XML_MODULES = "modules";
   static final String XML_MODULE = "module";
   static final String XML_NAME = "name";

   private final ModuleContainer moduleContainer;
   private final List<BaseModule> modules = new ArrayList<>();

   ModuleList(ModuleContainer moduleContainer) {
      this.moduleContainer = moduleContainer;
   }

   @Override
   public String toString() {
      return modules.stream()
            .map(BaseModule::getPersistentName)
            .collect(Collectors.joining(" -> "));
   }

   <T extends BaseModule> T addModule(T module) {
      return addModule(modules.size(), module);
   }

   <T extends BaseModule> T addModule(int index, T module) {
      modules.add(index, module);
      module.setModuleContainer(moduleContainer);
      return module;
   }

   List<BaseModule> getModules() {
      return modules;
   }

   void removeModule(BaseModule module) {
      modules.remove(module);
   }

   void removeAllModules() {
      modules.clear();
   }

   /**
    * Moves a module.
    *
    * @param module the module to move
    * @param shift  the number of steps to move the module
    */
   void moveModule(BaseModule module, int shift) {
      int index = modules.indexOf(module);
      if (index == -1) {
         return;
      }
      modules.remove(module);
      modules.add(index + shift, module);
   }

   Element toXml() {
      return toXml(modules);
   }

   static Element toXml(Collection<BaseModule> modules) {
      Element root = DocumentHelper.createElement(XML_MODULES);
      for (BaseModule module : modules) {
         root.add(module.toXml());
      }
      return root;
   }

   void appendXml(Element element) {
      for (Element moduleElement : element.elements()) {
         String name = moduleElement.attributeValue(XML_NAME);
         try {
            BaseModule module = moduleContainer.getKorona().getModuleManager().createModule(name);
            addModule(module);
            module.fromXml(moduleElement);
         } catch (ModuleCreationException e) {
            Log.global.log(Level.WARNING, "Error creating module " + name, e);
         }
      }
   }
}
