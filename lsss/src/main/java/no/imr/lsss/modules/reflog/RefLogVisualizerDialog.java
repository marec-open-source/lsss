package no.imr.lsss.modules.reflog;

import no.imr.tools.parameter.Unit;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

final class RefLogVisualizerDialog implements ItemContainer<LogLine> {
   private final ItemVisualizer<LogLine> itemVisualizer;
   private final List<LogLine> allItems;
   private Set<LogLine> selectedItems = Set.of();

   RefLogVisualizerDialog(RefLogDataModule refLogDataModule) {
      allItems = refLogDataModule.getAllLogLines().toList();

      List<ItemFeature<LogLine>> features = new ArrayList<>();
      features.add(ItemFeature.Time.fromMillis("Time", Unit.UTC, LogLine::timeInMillis, RefLogDataModule.DATE_TIME_FORMATTER));

      List<String> stationTypes = allItems.stream()
            .map(LogLine::stationType)
            .distinct()
            .sorted()
            .toList();
      features.add(new ItemFeature.Category<>("Station type", stationTypes, LogLine::stationType, true));

      allItems.stream()
            .flatMap(logLine -> logLine.fields().stream())
            .distinct()
            .sorted(Comparator.comparing(LogLineField::name))
            .map(field -> new ItemFeature.Number<LogLine>(field.name(), new Unit(field.unit()),
                  logLine -> value(field, logLine),
                  logLine -> string(field, logLine)))
            .forEach(features::add);

      itemVisualizer = new ItemVisualizer<>(features, this, Preferences.userRoot().node("/no/marec/lsss/RefLogVisualizerDialog"));
      itemVisualizer.show(refLogDataModule.getLSSS().getFrame(), "Ref log");
   }

   @Override
   public Collection<LogLine> getAllItems() {
      return allItems;
   }

   @Override
   public Set<LogLine> getSelectedItems() {
      return selectedItems;
   }

   @Override
   public void setSelectedItems(Set<LogLine> items) {
      selectedItems = items;
      itemVisualizer.update();
   }

   private static double value(LogLineField field, LogLine logLine) {
      try {
         return Double.parseDouble(string(field, logLine));
      } catch (NumberFormatException _) {
         return Double.NaN;
      }
   }

   private static String string(LogLineField field, LogLine logLine) {
      int i = logLine.fields().indexOf(field);
      return i < 0 ? "" : logLine.fieldValues().get(i);
   }
}
