package no.imr.lsss.modules.korona.tracking;

import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.lsss.framework.WorkFileExtra;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuUtils;
import no.imr.tools.swing.SimpleInputDialog;
import no.imr.tools.swing.icons.MiscIcons;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenu;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class TrackLabelling {
   private final TrackInfoModule trackInfoModule;
   private final Map<TrackId, ImmutableSet<String>> trackIdToLabels = new ConcurrentHashMap<>();
   private final ChangeManager changeManager = new ChangeManager();
   private final WorkFileExtra workFileExtra = new TrackLabellingWorkFileExtra();

   TrackLabelling(TrackInfoModule trackInfoModule) {
      this.trackInfoModule = trackInfoModule;

      trackInfoModule.getValidIdsChangeManager().addListener(this::removeUnreferencedLabels);
   }

   private void removeUnreferencedLabels(Set<TrackId> trackIds) {
      if (trackIds.isEmpty()) {
         return; // This happens when viewing original data not containing any tracks
      }
      trackIdToLabels.keySet().retainAll(trackIds);
   }

   WorkFileExtra getWorkFileExtra() {
      return workFileExtra;
   }

   public ImmutableSet<String> getLabels(TrackId trackId) {
      return trackIdToLabels.getOrDefault(trackId, ImmutableSet.of());
   }

   public void setLabels(TrackId trackId, ImmutableSet<String> labels) {
      if (trackInfoModule.isTrackReadOnly(trackId)) {
         return;
      }
      if (labels.isEmpty()) {
         trackIdToLabels.remove(trackId);
      } else {
         trackIdToLabels.put(trackId, labels);
      }
      changeManager.notifyListeners();
   }

   public boolean hasLabel(TrackId trackId, String label) {
      return getLabels(trackId).contains(label);
   }

   public void addLabel(TrackId trackId, String label) {
      setLabels(trackId, ImmutableUtils.add(getLabels(trackId), label));
   }

   public void addLabels(TrackId trackId, Collection<String> labels) {
      setLabels(trackId, ImmutableUtils.addAll(getLabels(trackId), labels));
   }

   public void removeLabel(TrackId trackId, String label) {
      setLabels(trackId, ImmutableUtils.remove(getLabels(trackId), label));
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public JMenu createLabelsMenu() {
      JMenu menu = MiscIcons.LABEL.on(new JMenu("Track labels"));
      menu.setMnemonic(KeyEvent.VK_L);

      GuiUtils.autoCreateContentMenu(menu, () -> {
         Set<TrackId> selectedTrackIds = trackInfoModule.getTrackSelection().getSelectedTrackIds();

         Set<String> selectedLabels = selectedTrackIds.stream()
               .flatMap(id -> getLabels(id).stream())
               .collect(Collectors.toCollection(TreeSet::new));
         Set<String> allLabels = trackIdToLabels.values().stream()
               .flatMap(Collection::stream)
               .collect(Collectors.toCollection(TreeSet::new));

         JMenu addMenu = MenuUtils.multiColumn(MiscIcons.ADD.on(MenuUtils.addMenu(menu, "Add label to selected", KeyEvent.VK_A)));
         JMenu removeMenu = MenuUtils.multiColumn(MiscIcons.DELETE.on(MenuUtils.addMenu(menu, "Remove label from selected", KeyEvent.VK_R)));
         menu.addSeparator();
         JMenu selectMenu = MenuUtils.multiColumn(MenuUtils.addMenu(menu, "Select by label", KeyEvent.VK_S));
         JMenu deselectMenu = MenuUtils.multiColumn(MenuUtils.addMenu(menu, "Deselect by label", KeyEvent.VK_D));
         JMenu retainMenu = MenuUtils.multiColumn(MenuUtils.addMenu(menu, "Retain by label", KeyEvent.VK_T));

         addMenu.setEnabled(!selectedTrackIds.isEmpty());
         removeMenu.setEnabled(!selectedLabels.isEmpty());
         selectMenu.setEnabled(!allLabels.isEmpty());
         deselectMenu.setEnabled(!selectedLabels.isEmpty());
         retainMenu.setEnabled(!selectedLabels.isEmpty());

         if (!allLabels.isEmpty()) {
            allLabels.forEach(label -> {
               MenuUtils.addItem(addMenu, label, e -> {
                  selectedTrackIds.forEach(trackId -> addLabel(trackId, label));
               });
               MenuUtils.addItem(selectMenu, label, e -> {
                  trackInfoModule.getTrackSelection().selectByPredicate(trackId -> hasLabel(trackId, label));
               });
            });
            addMenu.addSeparator();
         }

         MenuUtils.addItem(addMenu, "New label...", KeyEvent.VK_N, e -> {
            new SimpleInputDialog<>("Add new label", "Label", "", Function.identity())
                  .show(trackInfoModule.getLSSS().getFrame())
                  .ifPresent(label -> {
                     selectedTrackIds.forEach(trackId -> addLabel(trackId, label));
                  });
         });

         selectedLabels.forEach(label -> {
            MenuUtils.addItem(removeMenu, label, e -> {
               selectedTrackIds.forEach(trackId -> removeLabel(trackId, label));
            });
            MenuUtils.addItem(deselectMenu, label, e -> {
               trackInfoModule.getTrackSelection().deselectByPredicate(trackId -> hasLabel(trackId, label));
            });
            MenuUtils.addItem(retainMenu, label, e -> {
               trackInfoModule.getTrackSelection().deselectByPredicate(trackId -> !hasLabel(trackId, label));
            });
         });

         removeMenu.addSeparator();
         MenuUtils.addItem(removeMenu, "All labels", KeyEvent.VK_A, e -> {
            selectedTrackIds.forEach(trackId -> setLabels(trackId, ImmutableSet.of()));
         });
      });

      return menu;
   }

   private final class TrackLabellingWorkFileExtra extends WorkFileExtra {
      private static final String XML_TRACK = "track";
      private static final String XML_ID = "id";
      private static final String XML_LABEL = "label";

      private TrackLabellingWorkFileExtra() {
         super("trackLabels");
      }

      @Override
      public void toXml(DataFile dataFile, Element element) {
         trackIdToLabels.keySet().stream()
               .filter(trackId -> {
                  TrackInfo editedTrackInfo = trackInfoModule.getTrackEditing().getNewTracks().get(trackId);
                  return editedTrackInfo != null
                        ? editedTrackInfo.pingRange().intersects(dataFile.getPingRange())
                        : trackId.rawFileConfigurationNTDate() == dataFile.getRawFileConfiguration().getNTDate();
               })
               .sorted() // Consistent ordering to avoid changes in work file
               .forEach(trackId -> {
                  Element trackElement = element.addElement(XML_TRACK)
                        .addAttribute(XML_ID, trackId.toIdString(dataFile.getRawFileConfiguration()));
                  trackIdToLabels.get(trackId).forEach(label -> {
                     trackElement.addElement(XML_LABEL)
                           .addText(label);
                  });
               });
      }

      @Override
      public void beginFromXml() {
         trackIdToLabels.clear();
      }

      @Override
      public void fromXml(DataFile dataFile, @Nullable Element element, int originalVersion) {
         if (element == null) {
            return;
         }
         element.elements().forEach(trackElement -> {
            TrackId trackId = TrackId.fromIdString(dataFile.getRawFileConfiguration(), trackElement.attributeValue(XML_ID));
            ImmutableSet<String> labels = trackElement.elements().stream()
                  .map(labelElement -> labelElement.getText().intern())
                  .collect(ImmutableSet.toImmutableSet());
            trackIdToLabels.put(trackId, labels);
         });
      }

      @Override
      public void endFromXml() {
         changeManager.notifyListeners();
      }
   }
}
