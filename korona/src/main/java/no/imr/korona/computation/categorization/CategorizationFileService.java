package no.imr.korona.computation.categorization;

import no.imr.korona.Korona;
import no.imr.korona.computation.feature.CategoryVisualizer;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.config.ConfigFileParameter;
import no.imr.korona.config.ConfigFileParameterEditor;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.korona.config.KoronaConfigFileService;
import no.imr.tools.Version;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Component;
import java.awt.Dialog;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CategorizationFileService extends ConfigFileService {
   public static final Name NAME = new Name("Categorization", "Categorization library");
   public static final String CATEGORIZATION_IMR = "categorizationIMR";
   public static final String CATEGORIZATION_BASIC = "categorizationBasic";
   private static final String TRAINING_DATA_SETS = "trainingdatasets";

   public CategorizationFileService() {
      super(NAME);
   }

   @Override
   public ConfigFileSettingsContext getContext() {
      return KoronaConfigFileService.CONTEXT;
   }

   @Override
   public Path getInstallationLocation() {
      Path imrCategorizationXml = getInstallationConfigDir().resolve(CATEGORIZATION_IMR).resolve(Configurator.CATEGORIZATION_FILE);
      if (Files.exists(imrCategorizationXml)) {
         return imrCategorizationXml;
      }
      for (Path dir : getAdditionalInstallationConfigDirs()) {
         imrCategorizationXml = dir.resolve(CATEGORIZATION_IMR).resolve(Configurator.CATEGORIZATION_FILE);
         if (Files.exists(imrCategorizationXml)) {
            return imrCategorizationXml;
         }
      }
      return getInstallationConfigDir().resolve(CATEGORIZATION_BASIC).resolve(Configurator.CATEGORIZATION_FILE);
   }

   @Override
   public Path getInstallationConfigDir() {
      return Korona.getInstallationDir().resolveSibling("categorization").resolve(TRAINING_DATA_SETS);
   }

   @Override
   public List<Path> getAdditionalInstallationConfigDirs() {
      Path marecInstallationDir = Korona.getInstallationDir().getParent().getParent();
      Pattern pattern = Pattern.compile("Categorization (\\d+\\.\\d+(|.\\d+(|-.*)))");
      Version newestVersion = null;
      Path newestDir = null;
      try {
         for (Path dir : FileUtils.listFiles(marecInstallationDir)) {
            Matcher matcher = pattern.matcher(dir.getFileName().toString());
            if (!matcher.matches()) {
               continue;
            }
            Version version = new Version(matcher.group(1));
            if (newestVersion == null || version.isNewerThan(newestVersion)) {
               newestVersion = version;
               newestDir = dir;
            }
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing files in " + marecInstallationDir, e);
      }
      return newestDir != null ? List.of(newestDir.resolve(TRAINING_DATA_SETS)) : List.of();
   }

   @Override
   public FileParameter createFileParameter(ConfigFileSettings configFileSettings) {
      return new ConfigFileParameter(this, configFileSettings) {
         @Override
         public void applyFileChooser(JFileChooser fileChooser) {
            Path file = fileChooser.getSelectedFile().toPath();
            if (Files.isDirectory(file)) {
               Path configurationXml = file.resolve(Configurator.CATEGORIZATION_FILE);
               if (Files.exists(configurationXml)) {
                  file = configurationXml;
               }
            }
            setFile(file);
         }
      };
   }

   @Override
   protected ConfigFileParameterEditor createFileParameterEditor(ConfigFileSettings configFileSettings) {
      return new ConfigFileParameterEditor(this, configFileSettings) {
         @Override
         public boolean edit(@Nullable Component referenceComponent, boolean editable) {
            Configurator configurator = new Configurator(getConfigFileSettings(), null);
            CategoryVisualizer categoryVisualizer = new CategoryVisualizer(configurator, referenceComponent, Dialog.ModalityType.DOCUMENT_MODAL);
            return categoryVisualizer.getDidSave();
         }

         @Override
         public boolean createNew(@Nullable Component referenceComponent) {
            Path categorizationFile = getFileParameter().getFile();
            if (categorizationFile == null) {
               JOptionPane.showMessageDialog(referenceComponent, "Please choose a " + Configurator.CATEGORIZATION_FILE + " to edit.");
               return false;
            }
            Path transducerRangesFile = getConfigFileSettings().getFile(TransducerRangesFileService.NAME);
            Configurator configurator = new Configurator(categorizationFile, transducerRangesFile, null);
            CategoryVisualizer categoryVisualizer = new CategoryVisualizer(configurator, referenceComponent, Dialog.ModalityType.DOCUMENT_MODAL);
            return categoryVisualizer.getDidSave();
         }
      };
   }

   @Override
   protected FileParameter.@Nullable Copier createFileParameterCopier(ConfigFileSettings configFileSettings) {
      return null;
   }

   @Override
   public boolean canBePlatformSpecific() {
      return false;
   }

   @Override
   public Map<Path, Path> getAdditionalFilesToCopy(Path sourceFile, Path destinationFile) {
      Path sourceDir = Configurator.getCategoriesDirectory(sourceFile);
      if (!Files.exists(sourceDir)) {
         return Map.of();
      }
      Path destinationDir = Configurator.getCategoriesDirectory(destinationFile);
      return Map.of(sourceDir, destinationDir);
   }
}
