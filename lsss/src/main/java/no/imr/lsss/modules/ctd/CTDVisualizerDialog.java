package no.imr.lsss.modules.ctd;

import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;

import javax.swing.JComponent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class CTDVisualizerDialog implements ItemContainer<CTDVisualizerDialog.CtdItem> {
   private final CTDDataModule ctdDataModule;
   private final ItemVisualizer<CtdItem> itemVisualizer;
   private List<CtdItem> allItems = List.of();
   private Set<CtdItem> selectedItems = Set.of();

   CTDVisualizerDialog(CTDDataModule ctdDataModule, JComponent referenceComponent) {
      this.ctdDataModule = ctdDataModule;

      List<ItemFeature<CtdItem>> features = new ArrayList<>();
      features.add(ItemFeature.Time.fromMillis("Station time", Unit.UTC, item -> item.ctdDataInfo.ctdData.timeInMillis(), CTDViewModule.DATE_TIME_FORMATTER));
      DecimalFormat geoPosFormat = Utils.createDecimalFormat("0.000000");
      features.add(new ItemFeature.Number<>("Longitude", Unit.DEGREES, item -> item.ctdDataInfo.ctdData.geographicalPosition().getLongitude(), geoPosFormat));
      features.add(new ItemFeature.Number<>("Latitude", Unit.DEGREES, item -> item.ctdDataInfo.ctdData.geographicalPosition().getLatitude(), geoPosFormat));
      DecimalFormat format = Utils.createDecimalFormat("0.0000");
      ctdDataModule.getCTDDatas().stream()
            .flatMap(ctdData -> ctdData.columnNames().stream())
            .distinct()
            .sorted()
            .forEach(columnName -> {
               features.add(new ItemFeature.Number<>(columnName, Unit.NONE, item -> item.getValue(columnName), format));
            });

      itemVisualizer = new ItemVisualizer<>(features, this, ctdDataModule.getLSSS().getPreferences("CTDVisualizerDialog"));
      WhenShowingListening.connect(itemVisualizer.getComponent(), ctdDataModule.getChangeManager(), GuiListeners.coalescingLater(this::updateAllItems));
      updateAllItems();
      itemVisualizer.show(referenceComponent, "CTD measurements");
   }

   @Override
   public Collection<CtdItem> getAllItems() {
      return allItems;
   }

   @Override
   public Set<CtdItem> getSelectedItems() {
      return selectedItems;
   }

   @Override
   public void setSelectedItems(Set<CtdItem> items) {
      selectedItems = items;
      itemVisualizer.update();
   }

   private void updateAllItems() {
      List<CtdItem> items = new ArrayList<>();
      for (CTDData ctdData : ctdDataModule.getCTDDatas()) {
         CtdDataInfo ctdDataInfo = new CtdDataInfo(ctdData);
         int rowCount = ctdData.rows().size();
         for (int rowIndex = 0; rowIndex < rowCount; rowIndex++) {
            items.add(new CtdItem(ctdDataInfo, rowIndex));
         }
      }
      allItems = items;
      itemVisualizer.update();
   }

   private static final class CtdDataInfo {
      private final CTDData ctdData;
      private final Map<String, Integer> columnNameToIndex;

      private CtdDataInfo(CTDData ctdData) {
         this.ctdData = ctdData;
         List<String> columnNames = ctdData.columnNames();
         columnNameToIndex = IntStream.range(0, columnNames.size())
               .boxed()
               .collect(Collectors.toUnmodifiableMap(columnNames::get, Function.identity()));
      }
   }

   static final class CtdItem {
      private final CtdDataInfo ctdDataInfo;
      private final int rowIndex;

      private CtdItem(CtdDataInfo ctdDataInfo, int rowIndex) {
         this.ctdDataInfo = ctdDataInfo;
         this.rowIndex = rowIndex;
      }

      private double getValue(String columnName) {
         Integer columnIndex = ctdDataInfo.columnNameToIndex.get(columnName);
         return columnIndex != null ? ctdDataInfo.ctdData.rows().get(rowIndex)[columnIndex] : Double.NaN;
      }
   }
}
