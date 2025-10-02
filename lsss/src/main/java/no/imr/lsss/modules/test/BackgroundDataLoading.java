package no.imr.lsss.modules.test;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.logging.Log;

import javax.swing.JComponent;
import javax.swing.JToggleButton;
import java.util.List;

final class BackgroundDataLoading {
   private final JToggleButton button = new JToggleButton("Background data loading");
   private final DataManager dataManager;

   BackgroundDataLoading(DataManager dataManager) {
      this.dataManager = dataManager;
      button.addActionListener(e -> {
         if (button.isSelected()) {
            Exec.CACHED_THREAD_POOL.execute(this::run);
         }
      });
   }

   JComponent getComponent() {
      return button;
   }

   private void run() {
      int counter = 0;
      while (button.isSelected()) {
         List<DataFile> dataFiles = dataManager.getDataFileSet().getDataFiles();
         if (dataFiles.isEmpty()) {
            Utils.sleep(100);
            continue;
         }
         DataFile dataFile = dataFiles.getFirst();
         Log.global.info("BackgroundDataLoading: Loading from " + dataFile);

         PingRange pingRange = dataFile.getPingRange();
         int n = pingRange.getPingCount();
         for (int i = 0; button.isSelected() && i < n; i++) {
            Utils.sleep(1);
            PingIndex pingIndex = dataFile.pingNumberToPingIndex(pingRange.begin().getPingNumber() + i);
            Ping ping = dataFile.getPing(pingIndex);
            ping.getPingData();
            counter++;
            if (counter % 1000 == 0) {
               Log.global.info("BackgroundDataLoading: Loaded " + counter + " pings");
            }
         }
      }
   }
}
