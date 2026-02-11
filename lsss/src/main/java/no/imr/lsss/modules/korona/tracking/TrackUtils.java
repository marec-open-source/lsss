package no.imr.lsss.modules.korona.tracking;

import javax.swing.JMenuItem;
import java.util.HashSet;
import java.util.Set;

public final class TrackUtils {
   private TrackUtils() {
   }

   public static JMenuItem menuItemCreateRegionsForSelected(TrackInfoModule trackInfoModule) {
      JMenuItem item = new JMenuItem("Create LSSS regions for selected tracks");
      item.addActionListener(_ -> {
         trackInfoModule.createSchoolsAndSelect(trackInfoModule.getTrackSelection().getSelectedTrackIds().stream());
      });
      return item;
   }

   public static JMenuItem createMenuItemRegionsForPingRange(TrackInfoModule trackInfoModule) {
      JMenuItem item = new JMenuItem("Create LSSS regions for all tracks in displayed ping range");
      item.addActionListener(_ -> {
         int channel = trackInfoModule.getLSSS().getInterpretationSettings().getChannel();
         Set<TrackId> trackIds = new HashSet<>();
         trackInfoModule.getLSSS().getInterpretationSettings().getPingSampler().getAvailablePings().forEach(ping -> {
            trackInfoModule.getTrackEditing().getTrackBorders(ping, channel).forEach(trackBorder -> {
               TrackId trackId = trackBorder.trackId();
               if (trackInfoModule.getValidIds().contains(trackId)) {
                  trackIds.add(trackId);
               }
            });
         });
         trackInfoModule.createSchoolsAndSelect(trackIds.stream());
      });
      return item;
   }
}
