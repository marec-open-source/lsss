package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.computation.IgnoreModuleComputationException;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.categorization.netcdf.pojo.AnnotationConfig;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cad0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ColorUtils;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class CategorizationNetcdfReaderModuleComputation extends ConcurrentPingModuleComputation {
   private static final List<Color> COLORS = List.of(
         ColorUtils.WHITE,
         ColorUtils.AQUA,
         ColorUtils.BLUEVIOLET,
         ColorUtils.BROWN,
         ColorUtils.CADETBLUE,
         ColorUtils.CHARTREUSE,
         ColorUtils.CORAL,
         ColorUtils.DARKOLIVEGREEN,
         ColorUtils.DEEPPINK,
         ColorUtils.LIGHTSKYBLUE,
         ColorUtils.MIDNIGHTBLUE,
         ColorUtils.BLUE,
         ColorUtils.GREEN,
         ColorUtils.YELLOW,
         ColorUtils.RED
   );

   private final CategorizationNetcdfDataset dataset;
   private final int referenceChannel;
   private final byte unknownCategoryNumber;
   private boolean closed;

   CategorizationNetcdfReaderModuleComputation(CategorizationNetcdfReaderModule module, ComputationContext computationContext, PingSource pingSource) throws IOException, IgnoreModuleComputationException {
      super(module, computationContext, pingSource);

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();

      Path ncFile = module.inputFile.getFile();
      if (ncFile == null) {
         throw new IgnoreModuleComputationException();
      }
      if (!Files.exists(ncFile)) {
         Log.global.warning("Input file does not exist: " + ncFile);
         throw new IgnoreModuleComputationException();
      }

      Integer referenceKHz = module.mainFrequency.getValue().orElse(null);
      referenceChannel = referenceKHz != null
            ? pingConfiguration.getRawFileConfiguration().lastChannelWithKHz(referenceKHz)
            : 1;
      if (referenceChannel <= 0) {
         throw new ModuleConfigurationException(module, "Cannot find channel with " + referenceKHz + " kHz");
      }

      dataset = CategorizationNetcdfDataset.open(ncFile);

      try {
         AnnotationConfig annotationConfig = CategorizationNetcdfWriterModuleComputation.readAnnotationConfig(ncFile.getParent());
         Map<Integer, AnnotationConfig.KoronaCategory> annotationIdToKoronaCategory = annotationConfig.categories().stream()
               .collect(Collectors.toUnmodifiableMap(
                     AnnotationConfig.AnnotationCategory::id,
                     AnnotationConfig.AnnotationCategory::koronaCategory
               ));

         Cac0Datagram cac0Datagram = new Cac0Datagram(pingConfiguration.getRawFileConfiguration().getNTDate());
         for (int i = 0; i < dataset.categories.length; i++) {
            int category = dataset.categories[i];
            AnnotationConfig.KoronaCategory koronaCategory = annotationIdToKoronaCategory.get(category);
            if (koronaCategory == null) {
               String name = Integer.toString(category);
               cac0Datagram.addCategory((byte) i, name, name, COLORS.get(i % COLORS.size()));
            } else {
               Color color = ColorUtils.parseColor(koronaCategory.color());
               if (color == null) {
                  color = COLORS.get(i % COLORS.size());
               }
               cac0Datagram.addCategory((byte) i, koronaCategory.name(), koronaCategory.legend(), color);
            }
         }

         unknownCategoryNumber = cac0Datagram.getCategories().stream()
               .filter(Cac0Datagram.Category::isUnknown)
               .map(Cac0Datagram.Category::getNumber)
               .findFirst()
               .orElseGet(() -> {
                  byte number = (byte) cac0Datagram.getCategories().size();
                  cac0Datagram.addCategory(number, Configurator.UNKNOWN_CATEGORY_NAME, Configurator.UNKNOWN_CATEGORY_NAME, Color.WHITE);
                  return number;
               });
         PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
         newPingConfiguration.getConfigurationItems().removeIf(Cac0Datagram.class::isInstance);
         newPingConfiguration.getConfigurationItems().add(cac0Datagram);
         setNewPingConfiguration(newPingConfiguration);

      } catch (Exception e) {
         try {
            dataset.close();
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   @Override
   protected void processPing(Ping ping) throws IOException {
      int pingTimeIndex = dataset.ncTimeVariable.timeToIndex(ping.getInstant());
      if (pingTimeIndex < 0) {
         return;
      }
      ChannelData referenceChannelData = ping.getChannelData(referenceChannel);
      if (referenceChannelData == null) {
         return;
      }
      try {
         int[] categories = dataset.categories;
         double[] ranges = dataset.ranges;

         float[][] annotationData = new float[categories.length][];
         for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
            if (getAsyncHandle().isCancelled()) {
               return;
            }
            Array annotationArray;
            synchronized (dataset) {
               if (closed) {
                  return;
               }
               annotationArray = dataset.annotationVar.read(new int[]{categoryIndex, pingTimeIndex, 0}, new int[]{1, 1, ranges.length});
            }
            annotationData[categoryIndex] = (float[]) annotationArray.get1DJavaArray(DataType.FLOAT);
         }
         Cad0Datagram cad0Datagram = new Cad0Datagram(ping.getNTDate(), categories.length, ranges.length,
               (float) (ranges[1] - ranges[0]), (float) (referenceChannelData.getHeaveCorrectedTransducerDepth() + ranges[0]));
         CategoryAndValue[] categoryAndValues = new CategoryAndValue[categories.length];
         for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
            categoryAndValues[categoryIndex] = new CategoryAndValue();
         }
         for (int pixelIndex = 0; pixelIndex < ranges.length; pixelIndex++) {
            for (int categoryIndex = 0; categoryIndex < categories.length; categoryIndex++) {
               float annotationValue = annotationData[categoryIndex][pixelIndex];
               categoryAndValues[categoryIndex].setCategoryIndex(categoryIndex, annotationValue);
            }
            Arrays.sort(categoryAndValues, CategoryAndValue.ANNOTATION_VALUE_COMPARATOR);
            for (int categoryPrority = 0; categoryPrority < categories.length; categoryPrority++) {
               CategoryAndValue categoryAndValue = categoryAndValues[categoryPrority];
               float value = categoryAndValue.annotationValue;
               if (value > 0) {
                  cad0Datagram.setPixel(pixelIndex, categoryPrority, (byte) categoryAndValue.categoryIndex, value, value);
               } else {
                  cad0Datagram.setPixel(pixelIndex, categoryPrority, unknownCategoryNumber, 0, 0);
               }
            }
         }
         ping.removeAll(Cad0Datagram.class);
         ping.add(cad0Datagram);
      } catch (InvalidRangeException e) {
         throw new IOException(e);
      }
   }

   @Override
   public void close() throws IOException {
      synchronized (dataset) {
         closed = true;
         dataset.close();
      }
   }

   private static final class CategoryAndValue {
      private static final Comparator<CategoryAndValue> ANNOTATION_VALUE_COMPARATOR = (o1, o2) -> {
         return Float.compare(o2.annotationValue, o1.annotationValue); // Decreasing order.
      };

      private int categoryIndex;
      private float annotationValue;

      private CategoryAndValue() {
      }

      private void setCategoryIndex(int categoryIndex, float annotationValue) {
         this.categoryIndex = categoryIndex;
         this.annotationValue = annotationValue;
      }
   }
}
