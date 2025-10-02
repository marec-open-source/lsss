package no.imr.lsss.modules.korona.experimentation;

import com.google.common.util.concurrent.Runnables;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.io.WriterModule;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.DataFileSetPingReader;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.CopyingPingReader;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.WorkFileException;
import no.imr.korona.viewer.coloring.ColorConverter;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.LsssConfig;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.application.ApplicationConfigurationXml;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.dom4j.Node;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

final class KoronaExperimentationProcessor {
   private final KoronaExperimentationModule module;
   private final LSSS sourceLsss;
   private final LSSS destinationLsss;
   private final Path destinationMainDir;
   private AsyncHandle processingAsyncHandle = new AsyncHandle();

   KoronaExperimentationProcessor(KoronaExperimentationModule module) {
      this.module = module;
      sourceLsss = module.getLSSS();

      String namespace = "KoronaExperimentation";
      if (module.getNameSpaceCounter() > 0) {
         namespace = namespace + "_" + module.getNameSpaceCounter();
      }
      destinationMainDir = Utils.getTmpDir().resolve(namespace);

      LsssConfig lsssConfig = new LsssConfig(sourceLsss.getLsssConfig().serviceCollection);
      lsssConfig.loadSettings = false;
      lsssConfig.isPrimaryLSSS = false;
      lsssConfig.onClose = () -> {
         lsssConfig.onClose = Runnables.doNothing();
         module.executeAlways(module::removeProcessor);
      };
      lsssConfig.preferencesNode = lsssConfig.preferencesNode + '/' + namespace;
      destinationLsss = new LSSS(lsssConfig);
      Element applicationXml = sourceLsss.getConfigurationManager().getApplicationConfiguration().toXml();

      Optional<Integer> port = module.serverPort.getValue();
      Node lsssServerPortNode = new ApplicationConfigurationXml(XmlUtils.toDocument(applicationXml)).lsssServerPortNode();
      if (lsssServerPortNode != null && port.isPresent()) {
         lsssServerPortNode.setText(port.get().toString());
      }
      Node lsssServerActiveNode = new ApplicationConfigurationXml(XmlUtils.toDocument(applicationXml)).lsssServerActiveNode();
      if (lsssServerActiveNode != null) {
         lsssServerActiveNode.setText(Boolean.toString(port.isPresent()));
      }
      destinationLsss.getConfigurationManager().getApplicationConfiguration().fromXml(applicationXml);
      destinationLsss.getSurveyManager().getSurveyReferenceDirectory().setFile(sourceLsss.getSurveyManager().getSurveyReferenceDirectory().getFile());
      destinationLsss.getSurveyManager().getLsssReferenceDirectory().setFile(sourceLsss.getSurveyManager().getLsssReferenceDirectory().getFile());
      destinationLsss.getSurveyManager().fromSurveyXml(sourceLsss.getSurveyManager().toSurveyXml());
      destinationLsss.getSurveyManager().saveAs(destinationMainDir.resolve(DataConfLSSS.LSSS_SUB_DIR.defaultRelativePath()).resolve("KoronaExperimentation" + SurveyManager.SURVEY_FILE_SUFFIX));
      List.of(DataConfLSSS.RAW_SUB_DIR, DataConfLSSS.KORONA_SUB_DIR, DataConfLSSS.REPORTS_DIR, DataConfLSSS.WORK_SUB_DIR, DataConfLSSS.EXPORT_SUB_DIR).forEach(subDir -> {
         destinationLsss.getConfigurationManager().getDataConf().getDir(subDir).setFile(destinationMainDir.resolve(subDir.defaultRelativePath()));
      });
      destinationLsss.getModuleManager().getModule(KoronaExperimentationModule.class).setNameSpaceCounter(module.getNameSpaceCounter() + 1);
      SwingUtilities.invokeLater(() -> destinationLsss.getModuleManager().getModule(KoronaExperimentationModule.class).setEnabled(false));
   }

   void close() {
      stopProcessing();
      selectNoData();
      destinationLsss.close();
      try {
         FileUtils.repeatedlyTryDeleteRecursively(destinationMainDir);
      } catch (IOException e) {
         SwingUtilities.invokeLater(() -> sourceLsss.showError(module.getViewHolder().getComponent(), "Error deleting KORONA experimentation directory " + destinationMainDir, e));
      }
   }

   void stopProcessing() {
      processingAsyncHandle.cancel();
   }

   void process() {
      processingAsyncHandle = new AsyncHandle();

      ColorConverter colorConverter = destinationLsss.getInterpretationSettings().getColorConverterContainer().getColorConverter();

      selectNoData();

      PingRange pingRange = getPingRange();
      if (pingRange.isEmpty()) {
         return;
      }

      module.setProgress(0, true);
      DataFileSet dataFileSet = sourceLsss.getInterpretationSettings().getDataFileSet();
      long startTime = pingRange.begin().getTimeInMillis();
      double duration = dataFileSet.previousOrSame(pingRange.end()).getTimeInMillis() - startTime;
      try {
         ModuleContainer moduleContainer = module.createModuleContainerForProcessing();

         WriterModule writerModule = moduleContainer.addModule(new WriterModule());
         writerModule.directory.setFile(destinationLsss.getConfigurationManager().getDataConf().getDir(DataConfLSSS.RAW_SUB_DIR).getFile());
         writerModule.fileName.setValue("KoronaExperimentationModule.raw");

         try (ModuleContainerComputation computation = moduleContainer.createComputation(new CopyingPingReader(new DataFileSetPingReader(dataFileSet, pingRange)), processingAsyncHandle)) {
            while (!processingAsyncHandle.isCancelled()) {
               Ping ping = computation.nextPing();
               if (ping == null) {
                  break;
               }
               module.setProgress((ping.getTimeInMillis() - startTime) / duration, true);
            }
         }
         if (!processingAsyncHandle.isCancelled()) {
            SwingUtilities.invokeLater(() -> applyProcessingResult(pingRange, colorConverter));
         }
      } catch (IOException e) {
         SwingUtilities.invokeLater(() -> sourceLsss.showError(module.getViewHolder().getComponent(), "Error processing", e));
      } finally {
         module.setProgress(0, false);
      }
   }

   private PingRange getPingRange() {
      PingRange pingRange = RegionManager.getPingRange(sourceLsss.getRegionManager().getSelectedRegions());
      if (pingRange.isEmpty()) {
         return pingRange;
      }
      DataFileSet dataFileSet = sourceLsss.getInterpretationSettings().getDataFileSet();
      PingIndex begin = dataFileSet.getClosestPingIndex(pingRange.begin().getPingNumber() - module.pingPadding.getIntValue(), PingMapping.NUMBER);
      PingIndex end = dataFileSet.getClosestPingIndex(pingRange.end().getPingNumber() + module.pingPadding.getIntValue(), PingMapping.NUMBER);
      return PingRange.of(begin, end).intersection(sourceLsss.getInterpretationSettings().getPingRange());
   }

   private void selectNoData() {
      GuiUtils.invokeNowOrWait(() -> {
         destinationLsss.getConfigurationManager().getDataConf().selectNothing();
         destinationLsss.getConfigurationManager().getDataConf().apply();
      });
   }

   private void applyProcessingResult(PingRange pingRange, ColorConverter colorConverter) {
      destinationLsss.getConfigurationManager().getDataConf().refresh();
      // An extra invokeLater is needed to make the data file selected in DataConf.
      SwingUtilities.invokeLater(() -> applyProcessingResultAfterRefresh(pingRange, colorConverter));
   }

   private void applyProcessingResultAfterRefresh(PingRange pingRange, ColorConverter colorConverter) {
      destinationLsss.getConfigurationManager().getDataConf().selectAll();
      destinationLsss.getConfigurationManager().getDataConf().apply();
      try {
         if (destinationLsss.getInterpretationSettings().getDataFileSet().getTotalRange().equals(pingRange)) {
            Element workXml = sourceLsss.getRegionManager().toXml(pingRange);
            destinationLsss.getRegionManager().setSkipNotifications(true);
            destinationLsss.getRegionManager().fromXml(workXml);
            destinationLsss.getRegionManager().setSkipNotifications(false);
         }
      } catch (WorkFileException e) {
         sourceLsss.showError(module.getViewHolder().getComponent(), "Error copying interpretation", e);
      }
      synchronizeSettings();
      if (colorConverter.isUsableInContext()) {
         destinationLsss.getInterpretationSettings().getColorConverterContainer().setColorConverter(colorConverter);
      }
      JFrame frame = destinationLsss.getFrame();
      if (frame != null) {
         frame.toFront();
      }
   }

   private void synchronizeSettings() {
      Set<Integer> selectedRegionIds = sourceLsss.getRegionManager().getSelectedRegions().stream()
            .map(Region::getObjectNumber)
            .collect(Collectors.toSet());
      destinationLsss.getRegionManager().replaceSelectedRegions(region -> selectedRegionIds.contains(region.getObjectNumber()));
      destinationLsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.setBooleanValue(sourceLsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.getBooleanValue());
      destinationLsss.getInterpretationSettings().setChannel(Math.min(sourceLsss.getInterpretationSettings().getChannel(), destinationLsss.getInterpretationSettings().getDataFileSet().getTransducerCount()));
      FloatRange depthRange = sourceLsss.getRegionManager().getDepthRange(sourceLsss.getRegionManager().getSelectedRegions());
      if (depthRange.isEmpty()) {
         depthRange = sourceLsss.getInterpretationSettings().getPelagicZSettings().getZoomedZRange();
      } else {
         depthRange = FloatRange.ofCenterAndSize(depthRange.getCenter(), 1.2f * depthRange.getSize());
      }
      destinationLsss.getInterpretationSettings().getPelagicZSettings().setZ(depthRange);
      destinationLsss.getInterpretationSettings().getBottomZSettings().setZ(sourceLsss.getInterpretationSettings().getBottomZSettings().getZoomedZRange());
      destinationLsss.getActions().showTooltip.set(sourceLsss.getActions().showTooltip.get());
   }
}
