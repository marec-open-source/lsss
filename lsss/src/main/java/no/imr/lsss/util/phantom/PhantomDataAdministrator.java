package no.imr.lsss.util.phantom;

import no.imr.korona.apps.relay.KoronaRelayUpdate;
import no.imr.korona.apps.relay.KoronaRelayUpdateChecker;
import no.imr.korona.apps.relay.KoronaRelayUtils;
import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.FileOpenRequest;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.korona.data.track.SegmentInfoCache;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.data.DefaultSegmentHandleFactory;
import no.imr.lsss.framework.config.survey.data.SegmentHandlesAndAttributes;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.io.LastModifiedAndSize;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.logging.Level;

public final class PhantomDataAdministrator {
   private final LSSS lsss;
   private final String name;
   private final DataManager phantomDataManager;
   private final DataManager selectionPhantomDataManager;
   private @Nullable Path phantomDataDir;

   private NavigableMap<SegmentHandle, SegmentHandle> rawToPhantom = Collections.emptyNavigableMap();

   private Map<SegmentHandle, SegmentInfo> allSegmentInfos = Map.of();
   private Range<Long> ntDateRange = new DefaultRange<>(0L, 0L);

   private @Nullable KoronaRelayUpdateChecker koronaRelayUpdateChecker;

   private final Executor dataFileInfoExecutor = new SerialExecutor(Exec.CACHED_THREAD_POOL);
   private AsyncHandle dataFileInfoAsyncHandle = new AsyncHandle();

   private AsyncHandle prevFileOpenAsyncHandle = new AsyncHandle();

   private final ChangeManager changedManager = new ChangeManager();

   public PhantomDataAdministrator(FeaturePlugin plugin, DataConfiguration phantomDataConfiguration) {
      lsss = plugin.getLSSS();
      name = plugin.getPersistentName();
      phantomDataManager = new DataManager(phantomDataConfiguration);
      selectionPhantomDataManager = new DataManager(phantomDataConfiguration);
      lsss.getInterpretationSettings().getReloadChangeManager().addListener(() -> {
         updateSegmentInfo();
         updateSelectedSegmentHandles(ntDateRange);
         if (prepareApply(lsss.getReferenceComponent())) {
            activateSelectionChanged();
         }
      });
   }

   public void setRawToPhantom(NavigableMap<SegmentHandle, SegmentHandle> rawToPhantom) {
      this.rawToPhantom = rawToPhantom;
   }

   public NavigableMap<SegmentHandle, SegmentHandle> getRawToPhantom() {
      return rawToPhantom;
   }

   public void setDirectory(@Nullable Path directory) {
      if (Objects.equals(phantomDataDir, directory)) {
         return;
      }
      phantomDataDir = directory;
      koronaRelayUpdateChecker = directory != null ? new KoronaRelayUpdateChecker(directory) : null;
      updateSegmentInfo();
      updateSelectedSegmentHandles(ntDateRange);
   }

   public void update() {
      updateSegmentInfo();
   }

   private void updateSegmentInfo() {
      dataFileInfoAsyncHandle.cancel();
      dataFileInfoAsyncHandle.waitUntilFinished();
      dataFileInfoAsyncHandle = new AsyncHandle();

      dataFileInfoExecutor.execute(dataFileInfoAsyncHandle.createManagedRunnable(() -> {
         allSegmentInfos = loadAllSegmentInfos();
      }));
   }

   private Map<SegmentHandle, SegmentInfo> loadAllSegmentInfos() {
      if (phantomDataDir == null) {
         return Map.of();
      }
      SegmentHandlesAndAttributes segmentHandlesAndAttributes;
      try {
         segmentHandlesAndAttributes = new DefaultSegmentHandleFactory(lsss).createSegmentHandlesAndAttributes(phantomDataDir, dataFileInfoAsyncHandle);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing files in " + phantomDataDir, e);
         return Map.of();
      }
      SegmentInfoCache segmentInfoCache = lsss.getSurveyManager().readSegmentInfoCache(phantomDataDir, segmentHandlesAndAttributes);
      Map<SegmentHandle, SegmentInfo> segmentInfos = new TreeMap<>();
      for (SegmentHandle segmentHandle : segmentHandlesAndAttributes.segmentHandles()) {
         if (dataFileInfoAsyncHandle.isCancelled()) {
            return Map.of();
         }
         SegmentInfo segmentInfo = segmentInfoCache != null ? segmentInfoCache.get(segmentHandle) : null;
         try {
            if (segmentInfo == null) {
               segmentInfo = segmentHandle.createSegmentInfo();
               if (segmentInfoCache != null) {
                  LastModifiedAndSize lastModifiedAndSize = segmentHandle.getLastModifiedAndSize(segmentHandlesAndAttributes.attributes(), dataFileInfoAsyncHandle);
                  if (dataFileInfoAsyncHandle.isCancelled()) {
                     return Map.of();
                  }
                  segmentInfoCache.add(segmentHandle, lastModifiedAndSize.lastModified(), lastModifiedAndSize.size(), segmentInfo);
               }
            }
            segmentInfos.put(segmentHandle, segmentInfo);
         } catch (IOException _) {
            // Ignore.
         }
      }
      if (segmentInfoCache != null) { // Save even if cancelled
         segmentInfoCache.saveIfNeeded();
      }
      return segmentInfos;
   }

   private List<SegmentHandle> getSegmentHandles(Range<Long> ntDateRange) {
      new WorkerDialog(lsss::getReferenceComponent, "Loading info about " + name + " phantom data files\n" + phantomDataDir)
            .start(asyncHandle -> {
               while (!asyncHandle.isCancelled() && !dataFileInfoAsyncHandle.isFinished()) {
                  asyncHandle.sleep(10);
               }
            });

      List<SegmentHandle> result = new ArrayList<>();
      boolean hasFound = false;
      for (Map.Entry<SegmentHandle, SegmentInfo> entry : allSegmentInfos.entrySet()) {
         if (entry.getValue().pingRange().toNTDateRange().intersects(ntDateRange)) {
            result.add(entry.getKey());
            hasFound = true;
         } else if (hasFound) {
            // Since the map is sorted: The first time there is no intersection after there has been an intersection,
            // no more intersecting handles will be found.
            break;
         }
      }
      return result;
   }

   public void updateSelectedSegmentHandles(Range<Long> ntDateRange) {
      this.ntDateRange = ntDateRange;
      List<SegmentHandle> segmentHandles = getSegmentHandles(ntDateRange);

      prevFileOpenAsyncHandle.cancel();
      prevFileOpenAsyncHandle.waitUntilFinished();

      FileOpenRequest fileOpenRequest = new FileOpenRequest(segmentHandles);

      selectionPhantomDataManager.asyncOpenFiles(fileOpenRequest);
      prevFileOpenAsyncHandle = fileOpenRequest.getAsyncHandle();
   }

   public void checkForUpdatesFromKoronaRelay(boolean forceCheck, @Nullable Component referenceComponent) {
      if (koronaRelayUpdateChecker == null) {
         return;
      }
      try (KoronaRelayUpdateChecker.UpdateLoader updateLoader = koronaRelayUpdateChecker.createUpdateLoader(forceCheck)) {
         List<KoronaRelayUpdate> updates = updateLoader.getUpdates();
         if (updates.isEmpty()) {
            return;
         }

         int a = JOptionPane.showConfirmDialog(referenceComponent,
               "There are new processed files available in the " + name + " phantom data directory.\n" +
                     "\n\nDo you want to use these files now?",
               "File update",
               JOptionPane.YES_NO_OPTION,
               JOptionPane.QUESTION_MESSAGE);
         if (a == JOptionPane.YES_OPTION) {
            //close open files
            phantomDataManager.closeAllFiles();
            allSegmentInfos = Map.of();

            Range<Long> prevDateRange = ntDateRange;
            updateSelectedSegmentHandles(new DefaultRange<>(0L, 0L));

            ProgressView progressView = new ProgressView("Moving " + name + " processed files", updates.size())
                  .showRemainingTime();
            new WorkerDialog(referenceComponent, progressView.getComponent())
                  .start(asyncHandle -> {
                     for (KoronaRelayUpdate update : updates) {
                        if (asyncHandle.isCancelled()) {
                           return;
                        }
                        try {
                           update.move();
                        } catch (IOException e) {
                           updateLoader.saveStatus();
                           int answer = GuiUtils.getNowOrWait(() -> {
                              return JOptionPane.showConfirmDialog(referenceComponent, "Error: " + e.getMessage()
                                          + "\nDo you want to try to copy the rest of the files? ",
                                    "Error copying processed files", JOptionPane.YES_NO_OPTION);
                           });
                           if (answer != JOptionPane.YES_OPTION) {
                              break;
                           }
                        }
                     }
                  });
            updateSegmentInfo();
            updateSelectedSegmentHandles(prevDateRange);
         }
      } catch (IOException e) {
         if (GuiUtils.fileExists(koronaRelayUpdateChecker.getStatusFile(), referenceComponent)) {
            SwingUtilities.invokeLater(() -> {
               KoronaRelayUtils.showStatusFileErrorDialog(referenceComponent, e, koronaRelayUpdateChecker.getStatusFile());
            });
         }
      }
   }

   public boolean prepareApply(@Nullable Component component) {
      return new WorkerDialog(component, "Reading " + name + " phantom data index files...")
            .start(asyncHandle -> {
               while (!asyncHandle.isCancelled() && !prevFileOpenAsyncHandle.isFinished()) {
                  asyncHandle.sleep(10);
               }
            })
            .success();
   }

   public boolean isDataChanged() {
      return !DataConf.isEqual(selectionPhantomDataManager.getDataFileSet(), phantomDataManager.getDataFileSet());
   }

   public void activateSelectionChanged() {
      ProgressView preparingProgressView = new ProgressView("Preparing to use new " + name + " data files...", 1000)
            .hideMainProgressLabel();
      new WorkerDialog(lsss.getReferenceComponent(), preparingProgressView.getComponent())
            .startWithoutCancel(() -> {
               selectionPhantomDataManager.getDataFileSet().getMaxDepth(preparingProgressView.getMainProgressHandler());
            });
      phantomDataManager.setDataFileSet(selectionPhantomDataManager.getDataFileSet());
      changedManager.notifyListeners();
   }

   public ChangeManager getChangedManager() {
      return changedManager;
   }

   public DataManager getPhantomDataManager() {
      return phantomDataManager;
   }
}
