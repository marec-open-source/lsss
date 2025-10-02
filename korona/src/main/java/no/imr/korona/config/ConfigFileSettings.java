package no.imr.korona.config;

import no.imr.korona.computation.ModulePredicate;
import no.imr.korona.data.datamanager.labelling.DataFileLabelling;
import no.imr.tools.io.FileType;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.misc.ReferenceDirectory;
import no.imr.tools.parameter.misc.ReferenceDirectoryCollection;
import no.imr.tools.parameter.misc.ReferenceDirectoryManager;
import no.imr.tools.plugins.BaseService;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class ConfigFileSettings extends Configurable {
   public static final String FILE_SUFFIX = ".cfs";
   public static final FileType FILE_TYPE = new FileType(FILE_SUFFIX, "Config file settings");
   public static final String DEFAULT_FILE_NAME = "ConfigFileSettings" + FILE_SUFFIX;

   public static final ConfigFileSettingsContext ALL = new ConfigFileSettingsContext(new Name("All"), MiscIcons.EMPTY);
   private static final String XML_CONTEXT = "context";
   private static final String XML_DATA_FILE_LABEL = "dataFileLabel";

   private final ReferenceDirectory referenceDirectory = new ReferenceDirectory(new Name("CfsDirectory"));
   private final ReferenceDirectoryManager referenceDirectoryManager = new ReferenceDirectoryManager()
         .add(new ReferenceDirectoryCollection(new Name("cfsFile"), referenceDirectory));

   private ConfigFileSettingsContext context = ALL;
   private String dataFileLabelTitle = "";
   private final List<ConfigFileService> allAvailableFileServices;
   private final Map<String, ConfigFileService> configFileServices = new LinkedHashMap<>();
   private final Map<String, FileParameter> fileParameters = new LinkedHashMap<>();

   private final ChangeManager changeManager = new ChangeManager();

   private @Nullable Path file;

   private ModulePredicate modulePredicate = ModulePredicate.alwaysTrue();
   private Supplier<@Nullable Path> defaultBrowseDirectorySupplier = () -> null;
   private Supplier<@Nullable DataFileLabelling> dataFileLabellingSupplier = () -> null;

   public ConfigFileSettings() {
      super(new Name("ConfigFiles", "Config file settings"));

      allAvailableFileServices = BaseService.getUsableServices(ConfigFileService.class).toList();
      updateFileParameters();
   }

   public List<ConfigFileService> getAllAvailableFileServices() {
      return allAvailableFileServices;
   }

   public ConfigFileSettingsContext getContext() {
      return context;
   }

   public void setContext(ConfigFileSettingsContext context) {
      if (this.context.equals(context)) {
         return;
      }
      this.context = context;
      updateFileParameters();
   }

   public String getDataFileLabelTitle() {
      return dataFileLabelTitle;
   }

   public void setDataFileLabelTitle(String dataFileLabelTitle) {
      this.dataFileLabelTitle = dataFileLabelTitle;
   }

   private void updateFileParameters() {
      Map<String, FileParameter> oldFileParameters = new HashMap<>(fileParameters);

      fileParameters.clear();
      configFileServices.clear();

      for (ConfigFileService service : allAvailableFileServices) {
         if (context.equals(ALL) || service.getContext().equals(context)) {
            configFileServices.put(service.getName().persistentName(), service);
            FileParameter newFileParameter = service.createFileParameter(this);
            add(newFileParameter);
            FileParameter oldFileParameter = oldFileParameters.get(newFileParameter.getPersistentName());
            if (oldFileParameter != null) {
               newFileParameter.setFile(oldFileParameter.getFile());
            }
         }
      }
   }

   public Set<ConfigFileSettingsContext> getContexts() {
      Set<ConfigFileSettingsContext> contexts = getContextsExcludingAll();
      contexts.add(ALL);
      return contexts;
   }

   public Set<ConfigFileSettingsContext> getContextsExcludingAll() {
      Set<ConfigFileSettingsContext> contexts = new LinkedHashSet<>();
      for (ConfigFileService service : allAvailableFileServices) {
         contexts.add(service.getContext());
      }
      return contexts;
   }

   private ConfigFileSettingsContext getContextByName(String contextName) {
      for (ConfigFileSettingsContext context : getContexts()) {
         if (context.name().persistentName().equals(contextName)) {
            return context;
         }
      }
      return ALL;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public ReferenceDirectoryManager getReferenceDirectoryManager() {
      return referenceDirectoryManager;
   }

   public @Nullable Path getFile() {
      return file;
   }

   public void setFile(@Nullable Path file) {
      this.file = file;
   }

   public void save(Path file) throws IOException {
      this.file = file;
      referenceDirectory.setFile(file.getParent());
      XmlUtils.writeDocument(toXml(), file);
   }

   public void load(Path file) throws IOException {
      this.file = file;
      referenceDirectory.setFile(file.getParent());
      fromXml(XmlUtils.readDocument(file).getRootElement());
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return getFileParameters();
   }

   @Override
   protected Configurable possiblyCreateNewSubConfigurable(String persistentName) {
      FileParameter fileParameter = new FileParameter(new Name(persistentName), null, FileParameter.Mode.FILE);
      fileParameter.setVisible(false);
      add(fileParameter);
      return fileParameter;
   }

   private void add(FileParameter fileParameter) {
      fileParameter.setReferenceDirectoryManager(referenceDirectoryManager);
      fileParameter.subscribe(changeManager);
      fileParameters.put(fileParameter.getPersistentName(), fileParameter);
   }

   public Collection<FileParameter> getFileParameters() {
      return fileParameters.values();
   }

   public FileParameter getFileParameter(Name fileServiceName) {
      return fileParameters.get(fileServiceName.persistentName());
   }

   public @Nullable FileParameter getOptionalFileParameter(Name fileServiceName) {
      return fileParameters.get(fileServiceName.persistentName());
   }

   public @Nullable Path getFile(Name fileServiceName) {
      FileParameter fileParameter = getOptionalFileParameter(fileServiceName);
      return fileParameter != null ? fileParameter.getFile() : null;
   }

   public ConfigFileService getFileService(Name fileServiceName) {
      return configFileServices.get(fileServiceName.persistentName());
   }

   public Collection<ConfigFileService> getFileServices() {
      return configFileServices.values();
   }

   public Collection<Name> getFileServiceNames() {
      Set<Name> names = new LinkedHashSet<>();
      for (ConfigFileService service : getFileServices()) {
         names.add(service.getName());
      }
      return names;
   }

   public void restoreInstallationLocations() {
      for (ConfigFileService service : getFileServices()) {
         FileParameter fileParameter = getFileParameter(service.getName());
         fileParameter.setFile(service.getInstallationLocation());
      }
   }

   @Override
   public Element toXml() {
      Element element = super.toXml();
      if (context != ALL) {
         element.addAttribute(XML_CONTEXT, context.name().persistentName());
      }
      if (!dataFileLabelTitle.isEmpty()) {
         element.addAttribute(XML_DATA_FILE_LABEL, dataFileLabelTitle);
      }
      return element;
   }

   @Override
   public void fromXml(Element element) {
      setContext(getContextByName(element.attributeValue(XML_CONTEXT, ALL.name().persistentName())));
      setDataFileLabelTitle(element.attributeValue(XML_DATA_FILE_LABEL, ""));
      super.fromXml(element);
   }

   public ModulePredicate getModulePredicate() {
      return modulePredicate;
   }

   public void setModulePredicate(ModulePredicate modulePredicate) {
      this.modulePredicate = modulePredicate;
   }

   public Supplier<@Nullable Path> getDefaultBrowseDirectorySupplier() {
      return defaultBrowseDirectorySupplier;
   }

   public void setDefaultBrowseDirectorySupplier(Supplier<@Nullable Path> defaultBrowseDirectorySupplier) {
      this.defaultBrowseDirectorySupplier = defaultBrowseDirectorySupplier;
   }

   public Supplier<@Nullable DataFileLabelling> getDataFileLabellingSupplier() {
      return dataFileLabellingSupplier;
   }

   public void setDataFileLabellingSupplier(Supplier<@Nullable DataFileLabelling> dataFileLabellingSupplier) {
      this.dataFileLabellingSupplier = dataFileLabellingSupplier;
   }

   public FileParameter getModuleConfigurationFileParameter() {
      for (ConfigFileService service : getFileServices()) {
         if (service.isModuleConfiguration()) {
            return getFileParameter(service.getName());
         }
      }
      throw new IllegalStateException();
   }

   public @Nullable Path getModuleConfigurationFile() {
      FileParameter fileParameter = getModuleConfigurationFileParameter();
      return fileParameter.getFile();
   }
}
