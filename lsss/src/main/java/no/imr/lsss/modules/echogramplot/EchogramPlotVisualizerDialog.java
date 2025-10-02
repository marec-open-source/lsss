package no.imr.lsss.modules.echogramplot;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.visualizer.ItemContainer;
import no.imr.tools.visualizer.ItemFeature;
import no.imr.tools.visualizer.ItemVisualizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

final class EchogramPlotVisualizerDialog implements ItemContainer<EchogramPlotVisualizerDialog.Item> {
   private final EchogramPlotModule module;
   private final List<PingFunction> allPingFunctions;
   private final ItemVisualizer<Item> itemVisualizer;
   private List<Item> allItems = List.of();
   private Set<Item> selectedItems = Set.of();

   EchogramPlotVisualizerDialog(EchogramPlotModule module) {
      this.module = module;
      allPingFunctions = List.copyOf(module.getAllPingFunctions());

      List<ItemFeature<Item>> features = new ArrayList<>();
      features.add(ItemFeature.Time.fromMillis("Time", Unit.UTC, item -> item.ping.getTimeInMillis(), Utils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss")));

      for (int i = 0; i < allPingFunctions.size(); i++) {
         PingFunction pingFunction = allPingFunctions.get(i);
         int index = i;
         features.add(new ItemFeature.Number<>(pingFunction.getName().displayName(), pingFunction.getUnit(),
               item -> item.values[index],
               item -> Utils.toString(pingFunction.getParameterExport().transform().applyAsDouble(item.values[index]))));
      }

      itemVisualizer = new ItemVisualizer<>(features, this, module.getLSSS().getPreferences("EchogramPlotVisualizerDialog"));
      InterpretationSettings interpretationSettings = module.getLSSS().getInterpretationSettings();
      WhenShowingListening.connect(itemVisualizer.getComponent(), List.of(
                  interpretationSettings.getPingSampler().getNewPingsChangeManager(),
                  interpretationSettings.getChannelChangeManager()
            ),
            GuiListeners.coalescingLater(this::updateAllItems));
      itemVisualizer.show(module.getComponent(), "Echogram plot");
   }

   @Override
   public Collection<Item> getAllItems() {
      return allItems;
   }

   @Override
   public Set<Item> getSelectedItems() {
      return selectedItems;
   }

   @Override
   public void setSelectedItems(Set<Item> items) {
      selectedItems = items;
      itemVisualizer.update();
   }

   private void updateAllItems() {
      Set<Ping> selectedPings = selectedItems.stream()
            .map(item -> item.ping)
            .collect(Collectors.toSet());

      allItems = collectAllItems();

      selectedItems = allItems.stream()
            .filter(item -> selectedPings.contains(item.ping))
            .collect(Collectors.toUnmodifiableSet());

      itemVisualizer.update();
   }

   private List<Item> collectAllItems() {
      InterpretationSettings interpretationSettings = module.getLSSS().getInterpretationSettings();
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      int channel = interpretationSettings.getChannel();
      List<Ping> pings = interpretationSettings.getPingSampler().getAvailablePings();
      long[] timeInMillis = new long[pings.size()];
      float[] bottom = new float[pings.size()];
      float[][] values = new float[allPingFunctions.size()][pings.size()];
      for (int iPing = 0; iPing < pings.size(); iPing++) {
         Ping ping = pings.get(iPing);
         timeInMillis[iPing] = ping.getTimeInMillis();
         bottom[iPing] = dataFileSet.getCoordinatedDepth(ping.getPingIndex());
         for (int iFunction = 0; iFunction < allPingFunctions.size(); iFunction++) {
            PingFunction pingFunction = allPingFunctions.get(iFunction);
            values[iFunction][iPing] = (float) pingFunction.compute(dataFileSet, ping, channel);
         }
      }
      for (int iFunction = 0; iFunction < allPingFunctions.size(); iFunction++) {
         PingFunction pingFunction = allPingFunctions.get(iFunction);
         values[iFunction] = pingFunction.postprocess(values[iFunction], timeInMillis, bottom);
      }
      return IntStream.range(0, pings.size())
            .mapToObj(iPing -> {
               float[] valuesForPing = new float[allPingFunctions.size()];
               for (int iFunction = 0; iFunction < allPingFunctions.size(); iFunction++) {
                  valuesForPing[iFunction] = values[iFunction][iPing];
               }
               return new Item(pings.get(iPing), valuesForPing);
            })
            .toList();
   }

   static final class Item {
      private final Ping ping;
      private final float[] values;

      private Item(Ping ping, float[] values) {
         this.ping = ping;
         this.values = values;
      }
   }
}
