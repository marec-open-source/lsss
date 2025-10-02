package no.imr.korona.data.buffer;

import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.formats.ek60.EK60ExpandingSegment;
import no.imr.korona.data.formats.ek60.EK60FileSet;
import no.imr.korona.data.formats.ek60.EK60SegmentHandle;
import no.imr.korona.data.formats.ek60.io.EndOfInputHandler;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.Segment;
import no.imr.korona.data.track.Track;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * A track consisting of the files in a directory.
 * The last file is assumed being written to.
 */
public final class ExpandingTrack extends Track {
   private final DatagramTypeManager datagramTypeManager;
   private Consumer<Ping> pingListener = Utils.emptyConsumer();
   private Consumer<Object> noDataListener = Utils.emptyConsumer();
   private final AsyncHandle asyncHandle = new AsyncHandle();
   private boolean hasWaited;
   private boolean hasAddedPing;
   private @Nullable PingIndex lastPingNotAdded;
   private final SerialExecutor listenerExecutor = new SerialExecutor(Exec.CACHED_THREAD_POOL);

   public ExpandingTrack(DatagramTypeManager datagramTypeManager) {
      this.datagramTypeManager = datagramTypeManager;
   }

   public void setNoDataListener(Listener listener) {
      noDataListener = Listeners.inExecutor(listenerExecutor, listener);
   }

   public void read(Path directory, Consumer<Ping> listener) {
      read(directory, listener, Integer.MAX_VALUE);
   }

   public void read(Path directory, Consumer<Ping> listener, int filesToOpenInitially) {
      pingListener = Listeners.inExecutor(listenerExecutor, listener);
      read(directory, filesToOpenInitially);
   }

   public void stop() {
      asyncHandle.cancel();
      asyncHandle.waitUntilFinished();
   }

   private void read(Path directory, int filesToOpenInitially) {
      ExecutorService executor = Executors.newSingleThreadExecutor(Exec.newThreadFactory("ExpandingTrack-" + directory));
      executor.execute(asyncHandle.createManagedRunnable(() -> {
         Path currentRawFile = null;
         while (!asyncHandle.isCancelled()) {
            List<Path> rawFiles = new ArrayList<>();
            try {
               FileUtils.listFilesWithAttributes(directory, asyncHandle).forEach(fileInfo -> {
                  if (KoronaUtils.isRawFile(fileInfo)) {
                     rawFiles.add(fileInfo.file());
                  }
               });
               rawFiles.sort(null);
            } catch (IOException e) {
               asyncHandle.sleep(1000);
               continue;
            }
            if (rawFiles.isEmpty()) {
               noDataListener.accept(Optional.empty());
               asyncHandle.sleep(1000);
               continue;
            }

            Path lastRawFile = rawFiles.getLast();
            if (lastRawFile.equals(currentRawFile)) {
               asyncHandle.sleep(1000);
               continue;
            }

            int iBegin;
            if (getLastSegment() == null) {
               iBegin = Math.max(0, rawFiles.size() - filesToOpenInitially);
            } else {
               iBegin = 0;
            }

            for (int i = iBegin; i < rawFiles.size() - 1; i++) {
               if (asyncHandle.isCancelled()) {
                  break;
               }
               Path file = rawFiles.get(i);
               Segment lastSegment = getLastSegment();
               if (lastSegment == null || lastSegment.getMainFile().compareTo(file) < 0) {
                  try {
                     EK60SegmentHandle segmentHandle = new EK60SegmentHandle(new EK60FileSet(file), datagramTypeManager);
                     PingRange pingRange = segmentHandle.createSegmentInfo().getPingRange();
                     if (!pingRange.isEmpty()) {
                        add(new Segment(segmentHandle, pingRange));
                     }
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, "Error opening " + file, e);
                  }
               }
            }

            currentRawFile = lastRawFile;

            try {
               readFile(currentRawFile);
            } catch (IOException e) {
               getErrorHandler().onError("Error reading " + currentRawFile, e);
            }
         }
      }));
   }

   private void readFile(Path rawFile) throws IOException {
      EK60ExpandingSegment segment = new EK60ExpandingSegment(new EK60SegmentHandle(new EK60FileSet(rawFile), datagramTypeManager), new EndOfInputHandler() {
         private final NextFileWait nextFileWait = new NextFileWait(rawFile, asyncHandle);

         @Override
         public boolean isEndOfInput(long millisWaiting, Comparator<FileInfo> comparator) throws IOException {
            if (!hasWaited) {
               hasWaited = true;
               if (lastPingNotAdded != null) {
                  addPing(lastPingNotAdded);
                  lastPingNotAdded = null;
               } else if (!hasAddedPing) {
                  PingRange pingRange = getTotalRange();
                  if (!pingRange.isEmpty()) {
                     addPing(getClosestPingIndex(pingRange.end().getPingNumber() - 1, PingMapping.NUMBER));
                  }
               }
            }
            noDataListener.accept(Optional.empty());
            return nextFileWait.isEndOfInput(millisWaiting, comparator);
         }
      }, datagramTypeManager);
      lastPingNotAdded = segment.expand(asyncHandle); // Must call expand before adding segment to track so that ping shift can be computed
      if (lastPingNotAdded == null) {
         return;
      }
      add(segment);

      while (!asyncHandle.isCancelled()) {
         PingIndex pingIndex = segment.expand(asyncHandle);
         if (pingIndex == null) {
            break;
         }
         updateLastSegmentRange();
         if (hasWaited) {
            addPing(pingIndex);
         } else {
            lastPingNotAdded = pingIndex;
         }
      }
   }

   private void addPing(PingIndex pingIndex) throws IOException {
      hasAddedPing = true;
      Segment lastSegment = getLastSegment();
      assert lastSegment != null;
      Ping ping = lastSegment.getSegmentData().loadPing(pingIndex, asyncHandle);
      if (asyncHandle.isCancelled()) {
         return;
      }
      pingListener.accept(ping);
   }
}
