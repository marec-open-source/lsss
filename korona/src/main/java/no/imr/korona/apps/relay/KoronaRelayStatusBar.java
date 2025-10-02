package no.imr.korona.apps.relay;

import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.statusbar.DiskUsageStatus;
import no.imr.tools.swing.statusbar.MemoryUsageStatus;
import no.imr.tools.swing.statusbar.StatusBar;
import no.imr.tools.time.RemainingTimeEstimator;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.Timer;

final class KoronaRelayStatusBar {
   private KoronaRelayStatusBar() {
   }

   static StatusBar create(FileParameter destinationDirectory, ObservableValue<Integer> remainingFiles,
                           ObservableValue<Float> remainingWork, ObservableValue<Boolean> processing) {
      return new StatusBar()
            .addFiller()
            .addSeparator()
            .add("Processing: ", new RemainingProcessingStatus(remainingFiles, remainingWork, processing).label)
            .addSeparator()
            .add("Disk: ", new DiskUsageStatus(destinationDirectory).getComponent())
            .addSeparator()
            .add("Memory: ", new MemoryUsageStatus().getComponent());
   }

   private static final class RemainingProcessingStatus {
      private final ObservableValue<Integer> remainingFiles;
      private final ObservableValue<Float> remainingWork;
      private final ObservableValue<Boolean> processing;
      private final JLabel label = new JLabel("");
      private @Nullable RemainingTimeEstimator remainingTimeEstimator;
      private final Timer timer = new Timer(1000, e -> update());

      private RemainingProcessingStatus(ObservableValue<Integer> remainingFiles,
                                        ObservableValue<Float> remainingWork,
                                        ObservableValue<Boolean> processing) {
         this.remainingFiles = remainingFiles;
         this.remainingWork = remainingWork;
         this.processing = processing;
         GuiListeners.coalescingLater(this::update).addToAndNotify(
               remainingWork,
               processing
         );
      }

      private void update() {
         float remainingWork = this.remainingWork.getValue();
         String text;
         if (processing.getValue() && remainingWork > 0) {
            if (remainingTimeEstimator == null) {
               timer.start();
               remainingTimeEstimator = new RemainingTimeEstimator(remainingWork);
            }
            remainingTimeEstimator.setRemainingWork(remainingWork);
            text = "Total: " + remainingTimeEstimator.getTotalTimeString()
                  + ", Remaining: " + remainingTimeEstimator.getRemainingTimeString()
                  + ", " + remainingFiles.getValue() + " files";
         } else {
            timer.stop();
            remainingTimeEstimator = null;
            text = processing.getValue() ? "Completed" : "Not running";
         }
         label.setText(text);
      }
   }
}
