package no.imr.korona.computation;

import com.google.common.collect.Lists;
import no.imr.korona.Korona;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.upgrade.UpgradeEngine;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.xml.XmlUtils;
import no.imr.tools.xml.XslUpgraderFactory;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Stream;

/**
 * Manages a chain of computation modules.
 */
public final class ModuleContainer {
   private static final String XML_NEWEST_VERSION = "4";
   static final UpgradeEngine<Element> XML_UPGRADE_ENGINE = new UpgradeEngine<>(
         "Module setup", XML_NEWEST_VERSION, XmlUtils::getVersion,
         new XslUpgraderFactory("no/imr/korona/resources/modules/moduleContainerUpgrade"));

   private static final String XML_MODULE_CONTAINER = "ModuleContainer";

   private final Korona korona;

   private @Nullable Path manuallySetAssociatedKoronaDirectory;

   private @Nullable Path configFile;
   private final ConfigFileSettings configFileSettings;

   private final ModuleList moduleList = new ModuleList(this);

   public ModuleContainer(Korona korona) {
      this(korona, korona.createConfigFileSettings());
   }

   public ModuleContainer(Korona korona, ConfigFileSettings configFileSettings) {
      this.korona = korona;
      this.configFileSettings = configFileSettings;
   }

   public Korona getKorona() {
      return korona;
   }

   public ConfigFileSettings getConfigFileSettings() {
      return configFileSettings;
   }

   public <T extends BaseModule> T addModule(T module) {
      return moduleList.addModule(module);
   }

   public <T extends BaseModule> T addModule(int index, T module) {
      return moduleList.addModule(index, module);
   }

   public <T extends BaseModule> @Nullable T getModule(Class<T> clazz) {
      return Utils.getFirstOrNull(getModules(), clazz);
   }

   public <T extends BaseModule> Stream<T> getModules(Class<T> clazz) {
      return Utils.getAllOfType(getModules(), clazz);
   }

   public List<BaseModule> getModules() {
      return moduleList.getModules();
   }

   ModuleList getModuleList() {
      return moduleList;
   }

   void removeModule(BaseModule module) {
      moduleList.removeModule(module);
   }

   /**
    * Moves a module.
    *
    * @param module the module to move
    * @param shift  the number of steps to move the module
    */
   void moveModule(BaseModule module, int shift) {
      moduleList.moveModule(module, shift);
   }

   void configureWithoutData() {
      for (BaseModule module : getModules()) {
         module.configureWithoutData();
      }
   }

   /**
    * Reads a configuration from a File.
    *
    * @param configFile the file to read from
    * @throws IOException if some IO error occurs
    */
   public void readConfiguration(Path configFile) throws IOException {
      this.configFile = configFile;

      Log.global.info("Loading module configuration file: " + configFile);
      Element element = XmlUtils.readDocument(configFile).getRootElement();
      if (element.getName().equals(XML_MODULE_CONTAINER)) {
         fromXml(element);
      } else {
         Log.global.warning("Wrong format '" + element.getName() + "' in module configuration file " + configFile);
      }
   }

   /**
    * Returns the configuration file (if any) used for configuring this container.
    *
    * @return the configuration file (if any) used for configuring this container
    */
   public @Nullable Path getConfigFile() {
      return configFile;
   }

   /**
    * Writes this configuration to a File.
    *
    * @param configFile the file to write to
    */
   public void writeConfiguration(Path configFile) throws IOException {
      this.configFile = configFile;
      XmlUtils.writeDocument(toXml(), configFile);
   }

   @Nullable Path deriveAssociatedKoronaDirectory() {
      if (manuallySetAssociatedKoronaDirectory != null) {
         return manuallySetAssociatedKoronaDirectory;
      } else {
         WriterModule writerModule = getLastWriterModule();
         if (writerModule != null) {
            return writerModule.directory.getFile();
         } else {
            return null;
         }
      }
   }

   public @Nullable WriterModule getLastWriterModule() {
      return Utils.getFirstOrNull(Lists.reverse(getModules()), WriterModule.class);
   }

   /**
    * Sets the associated KORONA directory.
    *
    * @param dir a directory. If null then (directory of output file) / KORONA is used.
    */
   public void setAssociatedKoronaDirectory(@Nullable Path dir) {
      manuallySetAssociatedKoronaDirectory = dir;
   }

   public ModuleContainerComputation createComputation(PingReader pingReader) throws IOException {
      return createComputation(pingReader, new AsyncHandle());
   }

   public ModuleContainerComputation createComputation(PingReader pingReader, AsyncHandle asyncHandle) throws IOException {
      ComputationContext computationContext = new ComputationContext(this, pingReader, asyncHandle);
      return new ModuleContainerComputation(computationContext);
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(XML_MODULE_CONTAINER)
            .addAttribute(XmlUtils.VERSION, XML_NEWEST_VERSION);
      element.add(moduleList.toXml());
      return element;
   }

   public void appendXml(Element element) {
      fromXml(element, false);
   }

   public void fromXml(Element element) {
      fromXml(element, true);
   }

   private void fromXml(Element element, boolean clearModuleList) {
      element = upgrade(element);
      if (clearModuleList) {
         moduleList.removeAllModules();
      }
      Element modulesElement = element.element(ModuleList.XML_MODULES);
      if (modulesElement != null) {
         moduleList.appendXml(modulesElement);
      }
   }

   private static Element upgrade(Element element) {
      try {
         return XML_UPGRADE_ENGINE.upgrade(element);
      } catch (UpgradeException e) {
         Log.global.log(Level.WARNING, "Error upgrading module configuration", e);
         return element;
      }
   }

   public void clear() {
      moduleList.removeAllModules();
   }
}
