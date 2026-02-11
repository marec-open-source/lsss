package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Layer;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.korona.region.WorkData;
import no.imr.korona.region.WorkFile;
import no.imr.korona.region.WorkFileException;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionModule;
import no.imr.lsss.plugins.WorkFileManager;
import no.imr.tools.ProgressHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.DirectoryListing;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.range.RangeSet;
import no.imr.tools.range.RangeUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.upgrade.UpgradeException;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.RecursiveAction;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class BaseSystemWorkFileManager implements WorkFileManager {
   private static final String DATA_FILE_LABELS_FILE_NAME = "dataFileLabels.xml";

   private static final String XML_EXTRA = "extra";

   private final LSSS lsss;
   private final List<WorkFileExtra> workFileExtras = new CopyOnWriteArrayList<>();
   private @Nullable KoronaRegionModule koronaRegionModule; // Can be null in tests

   private WorkData workData = new WorkData();

   BaseSystemWorkFileManager(LSSS lsss) {
      this.lsss = lsss;
   }

   void setup() {
      koronaRegionModule = lsss.getModuleManager().getModules(KoronaRegionModule.class).findFirst().orElse(null);
   }

   public WorkData getWorkData() {
      return workData;
   }

   @Override
   public void load(ProgressHandler progressHandler) {
      workFileExtras.forEach(WorkFileExtra::beginFromXml);
      workData = new WorkData();
      Path workDir = getWorkDir();
      if (workDir != null) {
         lsss.getRegionManager().setSkipNotifications(true);
         lsss.getRegionManager().getRegionConfiguration().setReadOnlyPingsEnabled(false);
         DirectoryListing workDirListing = DirectoryListing.ofOrEmpty(workDir, new AsyncHandle());
         Path workDataFile = workDir.resolve(WorkFile.WORK_DATA_FILE_NAME);
         if (workDirListing.exists(workDataFile)) {
            try {
               workData.fromXml(XmlUtils.readDocument(workDataFile).getRootElement());
            } catch (Exception e) {
               showErrorDialog("Error reading " + workDataFile, e);
            }
         }
         DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
         Listener progressListener = progressHandler.asCountingListener(dataFileSet.getDataFiles().size());
         LoadContext context = new LoadContext(this, workDir, workDirListing, progressListener);
         new LoadAction(lsss.getRegionManager(), dataFileSet, context).invoke();
         if (!context.errors.isEmpty()) {
            showWorkFileErrorDialog(context.errors);
         }
         lsss.getRegionManager().getRegionConfiguration().setReadOnlyPingsEnabled(true);
         lsss.getRegionManager().setSkipNotifications(false);
      }
      workFileExtras.forEach(WorkFileExtra::endFromXml);
      lsss.getRegionManager().regionStream()
            .filter(Region::hasObjectNumber)
            .mapToInt(Region::getObjectNumber)
            .max()
            .ifPresent(workData::possiblyAdjustObjectNumber);
      lsss.getRegionManager().getSchoolManager().removeAllUndoEdits();
   }

   private void showWorkFileErrorDialog(Map<String, String> errors) {
      String message = new HtmlStringBuilder()
            .text("Error reading " + errors.size() + " work files.")
            .html("<p style='color: red;'>")
            .html("<br>").text("These work files will not be used.")
            .html("<br>").text("Please check that the interpretation is still appropriate.")
            .build();

      String errorText = errors.entrySet().stream()
            .map(entry -> entry.getKey() + ": " + entry.getValue())
            .collect(Collectors.joining("\n"));

      if (lsss.getInterpretationSettings().isInteractiveMode()) {
         GuiUtils.invokeNowOrWait(() -> GuiUtils.showErrorDialog(lsss.getReferenceComponent(), message, errorText));
      } else {
         Log.global.warning(message + ": " + errorText);
      }
   }

   private void showInconsistentLayerErrorDialog(String error) {
      showErrorDialog("The layer system is inconsistent:\n"
            + error + "\n"
            + "It is recommended to restart LSSS.", null);
   }

   private void showErrorDialog(String message, @Nullable Throwable throwable) {
      GuiUtils.invokeNowOrWait(() -> lsss.showError(message, throwable));
   }

   @Override
   public @Nullable Path getWorkDir() {
      return lsss.getConfigurationManager().getDataConf().getDir(DataConfLSSS.WORK_SUB_DIR).getFile();
   }

   @Override
   public String getDataFileLabelsFileName() {
      return DATA_FILE_LABELS_FILE_NAME;
   }

   private void fromXml(RegionManager regionManager, DataFile dataFile, Path workFile, Element rootElement) throws WorkFileException, UpgradeException {
      int originalVersion = getVersion(rootElement);
      rootElement = WorkFile.upgrade(rootElement);

      regionManager.fromXml(rootElement);

      Element convertedElement = rootElement.element(KoronaRegionModule.XML_CONVERTED);
      if (convertedElement != null && koronaRegionModule != null) {
         koronaRegionModule.convertedRegionsFromXml(workFile, convertedElement);
      }

      Element extraElement = rootElement.element(XML_EXTRA);
      for (WorkFileExtra workFileExtra : workFileExtras) {
         Element element = extraElement != null ? extraElement.element(workFileExtra.id) : null;
         synchronized (workFileExtra.lock) {
            workFileExtra.fromXml(dataFile, element, originalVersion);
         }
      }
   }

   private Element toXml(RegionManager regionManager, DataFilePartition dataFilePartition) {
      Element rootElement = regionManager.toXml(dataFilePartition.dataFile.getPingRange(), dataFilePartition.layers, dataFilePartition.schools);

      if (koronaRegionModule != null) {
         Element convertedKoronaRegions = koronaRegionModule.convertedRegionsToXml(dataFilePartition.dataFile.getPingRange());
         if (convertedKoronaRegions != null) {
            rootElement.add(convertedKoronaRegions);
         }
      }

      Element extraElement = rootElement.addElement(XML_EXTRA);
      for (WorkFileExtra workFileExtra : workFileExtras) {
         Element element = extraElement.addElement(workFileExtra.id);
         workFileExtra.toXml(dataFilePartition.dataFile, element);
         if (!element.hasContent() && element.attributeCount() == 0) {
            extraElement.remove(element);
         }
      }
      if (!extraElement.hasContent()) {
         rootElement.remove(extraElement);
      }

      return rootElement;
   }

   private static int getVersion(Element element) throws WorkFileException {
      String version = element.attributeValue(XmlUtils.VERSION);
      if (version == null) {
         throw new WorkFileException("Missing version");
      }
      if (version.equals("1.0")) {
         return 1;
      }
      try {
         return Integer.parseInt(version);
      } catch (NumberFormatException _) {
         throw new WorkFileException("Invalid version: " + version);
      }
   }

   public void addWorkFileExtra(WorkFileExtra workFileExtra) {
      workFileExtras.add(workFileExtra);
   }

   @Override
   public List<Path> getModifiedFiles(ProgressHandler progressHandler, AsyncHandle asyncHandle) {
      List<DataFile> dataFiles = lsss.getDataManager().getDataFileSet().getDataFiles();
      if (dataFiles.isEmpty()) {
         return List.of();
      }
      String error = lsss.getRegionManager().getLayerManager().checkForError();
      if (error != null) {
         showInconsistentLayerErrorDialog(error);
      }
      Path workDir = getWorkDir();
      if (workDir == null) {
         return List.of();
      }
      DirectoryListing workDirListing = DirectoryListing.ofOrEmpty(workDir, asyncHandle);
      List<Path> modifiedFiles = Collections.synchronizedList(new ArrayList<>());
      Listener progressListener = progressHandler.asCountingListener(dataFiles.size());
      partitionDataFiles(dataFiles, lsss.getRegionManager()).parallelStream()
            .forEach(dataFilePartition -> {
               if (asyncHandle.isCancelled()) {
                  return;
               }
               progressListener.listen();
               String workFileBaseName = WorkFile.getWorkFileBaseName(dataFilePartition.dataFile);
               Path workFile = workDir.resolve(workFileBaseName + WorkFile.WORK_FILE_SUFFIX);
               Path snapFile = workDir.resolve(workFileBaseName + WorkFile.SNAP_FILE_SUFFIX);
               Element element = toXml(lsss.getRegionManager(), dataFilePartition);
               if (!XmlUtils.equalContent(element, workFile)) {
                  modifiedFiles.add(workFile);
               }
               if (workDirListing.exists(snapFile)) {
                  modifiedFiles.add(snapFile);
               }
            });
      // Check workData.xml last since object number counter can change during toXml.
      Path workDataFile = workDir.resolve(WorkFile.WORK_DATA_FILE_NAME);
      if (!XmlUtils.equalContent(workData.toXml(), workDataFile)) {
         modifiedFiles.add(workDataFile);
      }
      return modifiedFiles;
   }

   @Override
   public void save(ProgressHandler progressHandler, AsyncHandle asyncHandle) {
      List<DataFile> dataFiles = lsss.getDataManager().getDataFileSet().getDataFiles();
      if (dataFiles.isEmpty()) {
         return;
      }
      String error = lsss.getRegionManager().getLayerManager().checkForError();
      if (error != null) {
         showInconsistentLayerErrorDialog(error);
         return;
      }
      Path workDir = getWorkDir();
      if (workDir == null) {
         return;
      }
      DirectoryListing workDirListing = DirectoryListing.ofOrEmpty(workDir, asyncHandle);
      Listener progressListener = progressHandler.asCountingListener(dataFiles.size());
      partitionDataFiles(dataFiles, lsss.getRegionManager()).parallelStream()
            .forEach(dataFilePartition -> {
               if (asyncHandle.isCancelled()) {
                  return;
               }
               progressListener.listen();
               String workFileBaseName = WorkFile.getWorkFileBaseName(dataFilePartition.dataFile);
               Path workFile = workDir.resolve(workFileBaseName + WorkFile.WORK_FILE_SUFFIX);
               Path snapFile = workDir.resolve(workFileBaseName + WorkFile.SNAP_FILE_SUFFIX);
               Element element = toXml(lsss.getRegionManager(), dataFilePartition);
               try {
                  XmlUtils.writeDocument(element, workFile);
               } catch (IOException e) {
                  Log.global.log(Level.WARNING, "Error writing work file " + workFile, e);
               }
               if (workDirListing.exists(snapFile)) {
                  try {
                     Files.delete(snapFile);
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, "Error deleting snap file " + snapFile, e);
                  }
               }
            });

      // Save workData.xml last since object number counter can change during toXml.
      Path workDataFile = workDir.resolve(WorkFile.WORK_DATA_FILE_NAME);
      try {
         XmlUtils.writeDocument(workData.toXml(), workDataFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error writing work data file " + workDataFile, e);
      }
   }

   private static final class LoadContext {
      private final LSSS lsss;
      private final BaseSystemWorkFileManager workFileManager;
      private final Path workDir;
      private final DirectoryListing workDirListing;
      private final Listener progressListener;
      private final Map<String, String> errors = new ConcurrentSkipListMap<>();

      private LoadContext(BaseSystemWorkFileManager workFileManager, Path workDir, DirectoryListing workDirListing, Listener progressListener) {
         lsss = workFileManager.lsss;
         this.workFileManager = workFileManager;
         this.workDir = workDir;
         this.workDirListing = workDirListing;
         this.progressListener = progressListener;
      }
   }

   private static final class LoadAction extends RecursiveAction {
      private final RegionManager regionManager;
      private final DataFileSet dataFileSet;
      private final LoadContext context;

      private LoadAction(RegionManager regionManager, DataFileSet dataFileSet, LoadContext context) {
         this.regionManager = regionManager;
         this.dataFileSet = dataFileSet;
         this.context = context;
      }

      @Override
      protected void compute() {
         List<DataFile> dataFiles = dataFileSet.getDataFiles();
         if (dataFiles.isEmpty()) {
            return;
         }
         if (dataFiles.size() == 1) {
            loadWorkFile(dataFiles.getFirst());
            context.progressListener.listen();
            return;
         }
         int i = dataFiles.size() / 2;
         LoadAction left = subLoadAction(0, i);
         LoadAction right = subLoadAction(i, dataFiles.size());
         invokeAll(left, right);
         regionManager.join(left.regionManager, right.regionManager);
      }

      private void loadWorkFile(DataFile dataFile) {
         Path workFile = WorkFile.getExistingWorkFile(context.workDir, context.workDirListing, WorkFile.getWorkFileBaseName(dataFile));
         if (workFile == null) {
            regionManager.setupDefaultBoundaries();
            return;
         }
         try {
            Element xml = XmlUtils.readDocument(workFile).getRootElement();
            context.workFileManager.fromXml(regionManager, dataFile, workFile, xml);
         } catch (Exception e) {
            String error = e instanceof WorkFileException ? e.getMessage() : e.toString();
            context.errors.put(workFile.getFileName().toString(), error);
            regionManager.setupDefaultBoundaries();
         }
      }

      private LoadAction subLoadAction(int beginIndex, int endIndex) {
         DataFileSet subDataFileSet = dataFileSet.subDataFileSet(beginIndex, endIndex);
         RegionManager subRegionManager = new RegionManager(new SubLsssRegionConfiguration(context.lsss, subDataFileSet));
         return new LoadAction(subRegionManager, subDataFileSet, context);
      }

      private static final class SubLsssRegionConfiguration extends LsssRegionConfiguration {
         private final DataFileSet dataFileSet;

         private SubLsssRegionConfiguration(LSSS lsss, DataFileSet dataFileSet) {
            super(lsss);
            this.dataFileSet = dataFileSet;
         }

         @Override
         protected DataFileSet getDataFileSet() {
            return dataFileSet;
         }

         @Override
         public PingRange getVisiblePingRange() {
            return dataFileSet.getTotalRange();
         }

         @Override
         public RangeSet<PingIndex> getReadOnlyPings() {
            return RangeUtils.emptyRangeSet();
         }
      }
   }

   private record DataFilePartition(
         DataFile dataFile,
         Collection<Layer> layers,
         Collection<School> schools
   ) {
   }

   private static List<DataFilePartition> partitionDataFiles(List<DataFile> dataFiles, RegionManager regionManager) {
      List<DataFilePartition> result = Arrays.asList(new DataFilePartition[dataFiles.size()]);
      DataFilePartitionContext context = new DataFilePartitionContext(dataFiles, result);
      new DataFilePartitionAction(context, 0, dataFiles.size(),
            regionManager.getLayerManager().getLayers(),
            regionManager.getSchoolManager().getSchools()
      ).invoke();
      return result;
   }

   private record DataFilePartitionContext(
         List<DataFile> dataFiles,
         List<DataFilePartition> result
   ) {
   }

   private static final class DataFilePartitionAction extends RecursiveAction {
      private final DataFilePartitionContext context;
      private final int iBegin;
      private final int iEnd;
      private final Collection<Layer> layers;
      private final Collection<School> schools;

      private DataFilePartitionAction(DataFilePartitionContext context, int iBegin, int iEnd,
                                      Collection<Layer> layers, Collection<School> schools) {
         this.context = context;
         this.iBegin = iBegin;
         this.iEnd = iEnd;
         this.layers = layers;
         this.schools = schools;
      }

      @Override
      protected void compute() {
         int size = iEnd - iBegin;
         if (size == 0) {
            return;
         }
         if (size == 1) {
            DataFile dataFile = context.dataFiles.get(iBegin);
            context.result.set(iBegin, new DataFilePartition(dataFile, layers, schools));
            return;
         }
         int iMiddle = iBegin + size / 2;
         long middlePingNumber = context.dataFiles.get(iMiddle).getPingRange().begin().getPingNumber();
         RegionSplit<Layer> layerSplit = split(layers, middlePingNumber);
         RegionSplit<School> schoolSplit = split(schools, middlePingNumber);
         invokeAll(
               new DataFilePartitionAction(context, iBegin, iMiddle, layerSplit.left, schoolSplit.left),
               new DataFilePartitionAction(context, iMiddle, iEnd, layerSplit.right, schoolSplit.right)
         );
      }

      private static <R extends Region> RegionSplit<R> split(Collection<R> regions, long splitPingNumber) {
         List<R> left = new ArrayList<>();
         List<R> right = new ArrayList<>();
         for (R region : regions) {
            PingRange pingRange = region.getPingRange();
            if (pingRange.begin().getPingNumber() < splitPingNumber) {
               left.add(region);
            }
            if (pingRange.end().getPingNumber() > splitPingNumber) {
               right.add(region);
            }
         }
         return new RegionSplit<>(left, right);
      }

      private record RegionSplit<R extends Region>(List<R> left, List<R> right) {
      }
   }
}
