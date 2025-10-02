package no.imr.lsss.modules.trawl;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.prefs.Preferences;

final class TrawlVisualizerDialog implements ItemContainer<TrawlVisualizerDialog.TrawlItem> {
   private final TrawlGui trawlGui;
   private final ItemVisualizer<TrawlItem> itemVisualizer;
   private final ItemFeature.Category<TrawlItem> speciesFeature = new ItemFeature.Category<>("Catch species", List.of(), item -> item.target.speciesName, true);
   private final ItemFeature.Category<TrawlItem> sexFeature = new ItemFeature.Category<>("Individual sex", List.of(), item -> item.individual.sex().string, false);
   private List<TrawlItem> allItems = List.of();
   private Set<TrawlItem> selectedItems = Set.of();

   TrawlVisualizerDialog(TrawlGui trawlGui) {
      this.trawlGui = trawlGui;

      List<ItemFeature<TrawlItem>> features = List.of(
            ItemFeature.Time.fromInstant("Station time", Unit.UTC, item -> item.station.startTime, FishStation.DATE_TIME_FORMATTER),
            new ItemFeature.Number<>("Station number", Unit.NONE, item -> item.station.stationNumber, Utils.createDecimalFormat("0")),
            new ItemFeature.Number<>("Station tow distance", Unit.NAUTICAL_MILES, item -> item.station.logDistance, Utils.createDecimalFormat("0.000")),
            new ItemFeature.Number<>("Station fishing depth min", Unit.METER, item -> item.station.fishingDepth.min(), Utils.createDecimalFormat("0.00")),
            new ItemFeature.Number<>("Station fishing depth max", Unit.METER, item -> item.station.fishingDepth.max(), Utils.createDecimalFormat("0.00")),
            new ItemFeature.Number<>("Catch weight", Unit.KILOGRAM, item -> item.target.getWeight(), Utils.createDecimalFormat("0.000")),
            speciesFeature,
            new ItemFeature.Number<>("Catch number of individuals", Unit.COUNT, item -> item.target.getIndividuals().size(), Utils.createDecimalFormat("0")),
            new ItemFeature.Number<>("Individual length", Unit.CENTIMETER, item -> item.individual.lengthCm(), Utils.createDecimalFormat("0.0")),
            new ItemFeature.Number<>("Individual weight", Unit.KILOGRAM, item -> item.individual.weight(), Utils.createDecimalFormat("0.000")),
            sexFeature
      );

      itemVisualizer = new ItemVisualizer<>(features, this, Preferences.userRoot().node("/no/marec/lsss/TrawlVisualizerDialog"));
      WhenShowingListening.connect(itemVisualizer.getComponent(), trawlGui.getFileChangeManager(), GuiListeners.coalescingLater(this::updateAllItems));
      updateAllItems();
      itemVisualizer.show(trawlGui.getComponent(), "Trawl catch individuals");
   }

   @Override
   public Collection<TrawlItem> getAllItems() {
      return allItems;
   }

   @Override
   public Set<TrawlItem> getSelectedItems() {
      return selectedItems;
   }

   @Override
   public void setSelectedItems(Set<TrawlItem> items) {
      selectedItems = items;
      itemVisualizer.update();
   }

   private void updateAllItems() {
      Set<String> allSpeciesNames = new TreeSet<>();
      Set<FishSex> allSexes = EnumSet.noneOf(FishSex.class);
      List<TrawlItem> items = new ArrayList<>();
      for (FishStation station : trawlGui.getStations()) {
         for (FishTarget target : station.getTargets()) {
            if (target.getIndividuals().isEmpty()) {
               continue;
            }
            allSpeciesNames.add(target.speciesName);
            for (FishIndividual individual : target.getIndividuals()) {
               allSexes.add(individual.sex());
               items.add(new TrawlItem(station, target, individual));
            }
         }
      }
      allItems = items;
      speciesFeature.setCategories(List.copyOf(allSpeciesNames));
      sexFeature.setCategories(allSexes.stream().map(sex -> sex.string).toList());
      itemVisualizer.update();
   }

   static final class TrawlItem {
      private final FishStation station;
      private final FishTarget target;
      private final FishIndividual individual;

      private TrawlItem(FishStation station, FishTarget target, FishIndividual individual) {
         this.station = station;
         this.target = target;
         this.individual = individual;
      }
   }
}
