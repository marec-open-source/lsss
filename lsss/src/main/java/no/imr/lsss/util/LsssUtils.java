package no.imr.lsss.util;

import no.imr.korona.computation.categorization.CategorizationFileService;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.feature.EchogramWindow;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import java.awt.Window;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

/**
 * Utility functions for LSSS.
 */
public final class LsssUtils {
   private LsssUtils() {
   }

   /**
    * Enables all view modules for a feature plugin if all those modules are currently off.
    *
    * @param featurePlugin the plugin
    */
   public static void setViewModulesEnabled(FeaturePlugin featurePlugin) {
      List<BaseViewModule> viewModules = featurePlugin.getLSSS().getModuleManager().getModules(BaseViewModule.class)
            .filter(module -> module.getPlugin() == featurePlugin)
            .toList();

      if (viewModules.stream().noneMatch(BaseViewModule::isEnabled)) {
         viewModules.forEach(viewModule -> viewModule.setEnabled(true));
      }
   }

   public static void mergeSelectedLayers(LSSS lsss) {
      showErrors(lsss, lsss.getRegionManager().getLayerManager().mergeSelectedLayers());
   }

   public static void mergeSelectedSchools(LSSS lsss) {
      showErrors(lsss, lsss.getRegionManager().mergeSelectedSchools());
   }

   public static void deleteSelectedSchools(LSSS lsss) {
      showErrors(lsss, lsss.getRegionManager().deleteSelectedSchools());
   }

   private static void showErrors(LSSS lsss, List<String> errors) {
      if (!errors.isEmpty() && lsss.getInterpretationSettings().isInteractiveMode()) {
         String message = String.join("\n", errors);
         JOptionPane.showMessageDialog(lsss.getFrame(), message, "Error", JOptionPane.ERROR_MESSAGE);
      }
   }

   public static @Nullable Path getExistingSubDir(LSSS lsss, JComponent referenceComponent, SubDir mainSubDir, String... furtherSubDirs) {
      DataConf dataConf = lsss.getConfigurationManager().getSurveyConfiguration().getDataConf();
      FileParameter dirParameter = dataConf.getDir(mainSubDir);
      if (dirParameter.getFile() == null) {
         JOptionPane.showMessageDialog(referenceComponent, mainSubDir.parameterName().displayName() + " directory must be configured");
         dataConf.showInConfigurationDialog();
         if (dirParameter.getFile() == null) {
            return null;
         }
      }
      Path dir = dirParameter.getFile();
      for (String furtherSubDir : furtherSubDirs) {
         dir = dir.resolve(furtherSubDir);
      }
      if (!Files.exists(dir)) {
         int answer = JOptionPane.showConfirmDialog(referenceComponent,
               "Directory\n" + dir + "\ndoes not exist.\n\nCreate directory?",
               "Create directory?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
         if (answer != JOptionPane.OK_OPTION) {
            return null;
         }
         try {
            FileUtils.createDirectories(dir);
         } catch (IOException e) {
            lsss.showError(referenceComponent, "Error creating directory " + dir, e);
            return null;
         }
      }
      return dir;
   }

   public static PingRange getPingRange(DataFileSet dataFileSet, Scatter scatter) {
      Instant instant = DatabaseTime.toInstant(scatter);
      PingIndex begin = dataFileSet.getClosestPingIndex(PingMapping.instantToTimeValue(instant), PingMapping.TIME);
      PingIndex end = dataFileSet.getClosestPingIndex(PingMapping.instantToTimeValue(instant.plusMillis(10L * scatter.getDuration())), PingMapping.TIME);
      return PingRange.of(begin, end);
   }

   public static @Nullable EchogramWindow getEchogramWindow(LSSS lsss) throws IOException {
      List<Region> regions = lsss.getRegionManager().getSelectedRegions();

      PingRange regionPingRange = RegionManager.getPingRange(regions);
      PingRange visiblePingRange = lsss.getInterpretationSettings().getPingRange();
      FloatRange selectedDepthRange = lsss.getRegionManager().getDepthRange(regions);
      PingRange selectedPingRange = regionPingRange.intersection(visiblePingRange);
      if (selectedPingRange.isEmpty()) {
         JOptionPane.showMessageDialog(lsss.getFrame(), "Nothing selected!");
         return null;
      }

      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      ConfigFileSettings configFileSettings = lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getMainSetup().createConfigFileSettings();
      if (configFileSettings.getFile(CategorizationFileService.NAME) == null ||
            configFileSettings.getFile(TransducerRangesFileService.NAME) == null) {
         JTextPane message = GuiUtils.labelLikeHtmlTextPane("""
               Config files are not properly configured for this survey.<br>
               Go to <a href="preprocessing">Survey configuration - Preprocessing</a> and
               make sure the config file settings points to valid config files.""");
         GuiUtils.addHrefListener(message, href -> {
            switch (href) {
               case "preprocessing" -> {
                  Window window = GuiUtils.windowForComponent(message);
                  if (window != null) {
                     window.dispose();
                  }
                  lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().showInConfigurationDialog();
               }
               default -> {
               }
            }
         });
         JOptionPane.showMessageDialog(lsss.getFrame(), message);
         return null;
      }

      Configurator configurator = new Configurator(configFileSettings, dataFileSet.getRawFileConfiguration());
      if (configurator.getReferenceChannel() <= 0) {
         JOptionPane.showMessageDialog(lsss.getFrame(), "The reference frequency (" +
               KoronaUtils.hzToKHz(configurator.getReferenceFrequency()) + " kHz) is not available.");
         return null;
      }

      for (PingIndex pingIndex : dataFileSet.getPingIndices(selectedPingRange)) {
         ChannelData channelData = dataFileSet.getPing(pingIndex).getFirstAvailableChannelData();
         if (channelData != null) {
            long sampleCount = selectedPingRange.getPingCount() * (long) (selectedDepthRange.getSize() / channelData.getSampleDistance());
            if (sampleCount > 100000) {
               int answer = JOptionPane.showConfirmDialog(lsss.getFrame(),
                     "Selected area contains approximately " + sampleCount + " samples.\nContinue?",
                     "A large area selected", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
               if (answer != JOptionPane.OK_OPTION) {
                  return null;
               }
            }
            break;
         }
      }

      ProgressView progressView = new ProgressView("Loading pings", selectedPingRange.getPingCount());
      return new WorkerDialog(lsss.getFrame(), progressView.getComponent())
            .startMakeValue(asyncHandle -> {
               List<Ping> pings = dataFileSet.getPingIndexStream(selectedPingRange)
                     .map(dataFileSet::getPing)
                     .toList();
               DataFile firstSelectedDataFile = dataFileSet.getDataFile(selectedPingRange.begin());
               int pingOffset = (int) (selectedPingRange.begin().getPingNumber() - firstSelectedDataFile.getPingRange().begin().getPingNumber());
               EchogramWindow echogramWindow = new EchogramWindow(configurator, pingOffset, pings, selectedDepthRange, asyncHandle, progressView.getMainProgressHandler());
               echogramWindow.setRawFile(firstSelectedDataFile.getSegmentHandle().getMainFile());
               return echogramWindow;
            });
   }

   public static String infoText(String begin, FeaturePlugin plugin, String end) {
      if (plugin instanceof BaseSystemFeaturePlugin) {
         return begin + ' ' + end;
      }
      return begin + ' ' + plugin.getName().displayName() + ' ' + end;
   }
}
