package no.imr.lsss.modules.ts;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.time.NTDate;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class TSVisualizerDialog implements ItemContainer<TSData> {
   private final TSModule tsModule;
   private final ItemVisualizer<TSData> itemVisualizer;
   private List<TSData> allItems = List.of();

   TSVisualizerDialog(TSModule tsModule, Component referenceComponent) {
      this.tsModule = tsModule;

      DecimalFormat decimalFormat = Utils.createDecimalFormat("0.00");
      List<ItemFeature<TSData>> features = List.of(
            new ItemFeature.Number<>("Alongship angle", Unit.DEGREES, TSData::alongshipAngle, decimalFormat),
            new ItemFeature.Number<>("Athwartship angle", Unit.DEGREES, TSData::athwartshipAngle, decimalFormat),
            new ItemFeature.Number<>("Range", Unit.METER, TSData::range, decimalFormat),
            ItemFeature.Time.fromMillis("Time", Unit.UTC, tsData -> NTDate.ntDateToTimeInMillis(tsData.ntDate()), Utils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss")),
            new ItemFeature.Number<>("TSC", Unit.DB, TSData::tsc, decimalFormat),
            new ItemFeature.Number<>("TSU", Unit.DB, TSData::tsu, decimalFormat)
      );

      itemVisualizer = new ItemVisualizer<>(features, this, tsModule.getLSSS().getPreferences("TSVisualizerDialog"));
      WhenShowingListening.connect(itemVisualizer.getComponent(), tsModule.getTSDetectionChangeManager(), GuiListeners.coalescingLater(this::updateAllItems));
      WhenShowingListening.connect(itemVisualizer.getComponent(), tsModule.getTSSelectionChangeManager(), GuiListeners.coalescingLater(itemVisualizer::update));
      updateAllItems();
      itemVisualizer.show(referenceComponent, "TS detections");
   }

   @Override
   public Collection<TSData> getAllItems() {
      return allItems;
   }

   @Override
   public Set<TSData> getSelectedItems() {
      return tsModule.getSelectedTSData();
   }

   @Override
   public void setSelectedItems(Set<TSData> items) {
      tsModule.setSelectedTSData(items);
   }

   private void updateAllItems() {
      int channel = tsModule.getLSSS().getInterpretationSettings().getChannel();
      allItems = tsModule.getLSSS().getRegionManager().getSelectedRegions().stream()
            .flatMap(region -> tsModule.getTSData(region).values().stream())
            .flatMap(pingCache -> pingCache.getTsData(channel).stream())
            .toList();
      itemVisualizer.update();
   }
}
