package no.imr.tools.visualizer;

import no.imr.tools.RandomUtils;
import no.imr.tools.ToolsPreferences;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Unit;

import javax.swing.SwingUtilities;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

final class ItemVisualizerMain implements ItemContainer<ItemVisualizerMain.Item> {
   private static final List<String> CATEGORIES = List.of("Category A", "Category B", "Category C", "Category D", "Category E");

   private final List<Item> items;
   private Set<Item> selectedItems = Set.of();
   private final ItemVisualizer<Item> itemVisualizer;

   private ItemVisualizerMain() {
      Random random = new Random();
      items = IntStream.range(0, 100_000)
            .mapToObj(i -> new Item(random))
            .toList();

      List<ItemFeature<Item>> features = List.of(
            ItemFeature.Time.fromMillis("time 1", Unit.UTC, item -> item.timeInMillis1, Utils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss")),
            ItemFeature.Time.fromMillis("time 2", Unit.UTC, item -> item.timeInMillis2, Utils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss")),
            new ItemFeature.Category<>("category", CATEGORIES, item -> item.category, true),
            new ItemFeature.Number<>("x feature", Unit.METER, item -> item.x),
            new ItemFeature.Number<>("y feature", Unit.SECONDS, item -> item.y),
            new ItemFeature.Number<>("z feature", Unit.NONE, item -> item.z, Utils.createDecimalFormat("0.00")),
            new ItemFeature.Number<>("i 10", Unit.COUNT, item -> item.i10),
            new ItemFeature.Number<>("i 100", Unit.COUNT, item -> item.i100)
      );

      itemVisualizer = new ItemVisualizer<>(features, this, ToolsPreferences.node("ItemVisualizerMain"));
      itemVisualizer.show(null, "Test");
   }

   public static void main(String[] args) {
      SwingUtilities.invokeLater(ItemVisualizerMain::new);
   }

   @Override
   public Collection<Item> getAllItems() {
      return items;
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

   static final class Item {
      private final long timeInMillis1;
      private final long timeInMillis2;
      private final String category;
      private final double x;
      private final double y;
      private final double z;
      private final int i10;
      private final int i100;

      private Item(Random random) {
         timeInMillis1 = random.nextLong(100_000_000L);
         timeInMillis2 = random.nextLong(100_000_000_000L);
         category = RandomUtils.get(random, CATEGORIES);
         x = random.nextGaussian();
         y = random.nextGaussian() / 10;
         z = random.nextDouble(100);
         i10 = random.nextInt(10);
         i100 = random.nextInt(100);
      }
   }
}
