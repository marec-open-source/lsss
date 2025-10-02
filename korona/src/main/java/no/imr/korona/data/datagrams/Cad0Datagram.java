package no.imr.korona.data.datagrams;

import no.imr.korona.computation.categorization.Category;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.viewer.variables.CategoryRemapping;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Categorization data.
 */
public final class Cad0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = new DatagramType.Simple("CAD0", Cad0Datagram::new);

   private final byte[][] categories;
   private final byte[][] discriminants;
   private final byte[][] probabilities;

   private final int pixelCount;
   private final float sampleDistance;
   private final float firstDepth;

   private @Nullable Cac0Datagram cac0Datagram;

   public Cad0Datagram(long ntDate, int categoryCount, int pixelCount, float sampleDistance, float firstDepth) {
      super(ntDate);

      if (categoryCount < 1) {
         throw new IllegalArgumentException("count: " + categoryCount);
      }

      this.pixelCount = pixelCount;
      this.sampleDistance = sampleDistance;
      this.firstDepth = firstDepth;

      categories = new byte[categoryCount][pixelCount];
      discriminants = new byte[categoryCount][pixelCount];
      probabilities = new byte[categoryCount][pixelCount];
   }

   public Cad0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      int categoryCount = byteBuffer.get();
      if (categoryCount < 1) {
         throw new DatagramFormatException("categories: " + categoryCount);
      }
      pixelCount = ByteBufferUtils.readCount(byteBuffer, 3 * categoryCount);
      sampleDistance = byteBuffer.getFloat();
      firstDepth = byteBuffer.getFloat();
      categories = new byte[categoryCount][pixelCount];
      readBytes(byteBuffer, categories);
      discriminants = new byte[categoryCount][pixelCount];
      readBytes(byteBuffer, discriminants);
      probabilities = new byte[categoryCount][pixelCount];
      readBytes(byteBuffer, probabilities);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put((byte) categories.length);
      byteBuffer.putInt(pixelCount);
      byteBuffer.putFloat(sampleDistance);
      byteBuffer.putFloat(firstDepth);
      writeBytes(byteBuffer, categories);
      writeBytes(byteBuffer, discriminants);
      writeBytes(byteBuffer, probabilities);
   }

   @Override
   public void setPingConfiguration(PingConfiguration pingConfiguration) {
      cac0Datagram = pingConfiguration.getConfigurationItem(Cac0Datagram.class);
   }

   /**
    * Sets a category with corresponding discriminant function and
    * a posteriori probability in a pixel.
    *
    * @param pixelIndex       the index of the pixel
    * @param categoryPriority the priority of the category (0 being the best)
    * @param category         the category number
    * @param discriminant     the discriminant function
    * @param probability      the probability
    */
   public void setPixel(int pixelIndex, int categoryPriority,
                        byte category, float discriminant,
                        float probability) {
      categories[categoryPriority][pixelIndex] = category;
      discriminants[categoryPriority][pixelIndex] = floatToByte(discriminant);
      probabilities[categoryPriority][pixelIndex] = floatToByte(probability);
   }

   static byte floatToByte(float floatValue) {
      return (byte) (Math.clamp(floatValue, 0, 1) * 255);
   }

   static float byteToFloat(byte byteValue) {
      return (0xff & byteValue) / 255f;
   }

   public float indexToDepth(int index) {
      return index * sampleDistance + firstDepth;
   }

   /**
    * Converts a depth to a valid pixel index.
    *
    * @param depth a depth
    * @return a pixel index clamped to the valid range
    */
   public int depthToIndex(float depth) {
      int index = Math.round((depth - firstDepth) / sampleDistance);
      return Math.clamp(index, 0, pixelCount - 1);
   }

   public FloatRange getDepthRange() {
      return FloatRange.ofMinAndSize(firstDepth, pixelCount * sampleDistance);
   }

   public int getPixelCount() {
      return pixelCount;
   }

   public int getCategoryCount() {
      return categories.length;
   }

   /**
    * Returns the category, given a category priority and a pixel index.
    *
    * @param categoryPriority the category priority, 0 is best
    * @param pixelIndex       the index of the pixel
    * @return the category
    */
   public byte getCategory(int categoryPriority, int pixelIndex) {
      return categories[categoryPriority][pixelIndex];
   }

   public float getDiscriminant(int categoryPriority, int pixelIndex) {
      return byteToFloat(discriminants[categoryPriority][pixelIndex]);
   }

   /**
    * Returns the probability, given a category priority and a pixel index.
    *
    * @param categoryPriority the category priority, 0 is best
    * @param pixelIndex       the index of the pixel
    * @return the probability between 0 and 1
    */
   public float getProbability(int categoryPriority, int pixelIndex) {
      return byteToFloat(probabilities[categoryPriority][pixelIndex]);
   }

   /**
    * Returns the best category, given a pixel index.
    *
    * @param pixelIndex the index of the pixel
    * @return the best category
    */
   public byte getBestCategory(int pixelIndex) {
      return categories[0][pixelIndex];
   }

   public byte[] getBestCategories(CategoryVariable categoryVariable, byte defaultCategory) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      byte[] bestCategories = new byte[pixelCount];
      for (int i = 0; i < bestCategories.length; i++) {
         bestCategories[i] = getBestCategory(i, categoryVariable, defaultCategory, categoryRemapping);
      }
      return bestCategories;
   }

   public byte getBestCategory(int pixelIndex, CategoryVariable categoryVariable, byte defaultCategory) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      return getBestCategory(pixelIndex, categoryVariable, defaultCategory, categoryRemapping);
   }

   public byte getBestCategory(int pixelIndex, CategoryVariable categoryVariable, byte defaultCategory, CategoryRemapping categoryRemapping) {
      boolean[] categoryNumberPlottable = categoryVariable.getSettings().getCategoryNumberPlottable();
      float probabilityThreshold = categoryVariable.getProbabilityThreshold();
      for (int i = 0; i < categories.length; i++) {
         byte category = categories[i][pixelIndex];
         category = categoryRemapping.remap(category);
         if (categoryNumberPlottable[category] && byteToFloat(probabilities[i][pixelIndex]) >= probabilityThreshold) {
            return category;
         }
      }
      return defaultCategory;
   }

   /**
    * Returns an array of probabilities for best categories.
    * Note that the best category does not necessarily have the highest probability,
    * depending on type of discriminant, contextual corrections, etc.
    *
    * @param categoryVariable the category variable
    * @return an array of probabilities for best categories
    */
   public float[] getBestProbabilities(CategoryVariable categoryVariable) {
      float[] result = new float[pixelCount];
      for (int i = 0; i < result.length; i++) {
         result[i] = getBestProbability(i, categoryVariable);
      }
      return result;
   }

   private float getBestProbability(int pixelIndex, CategoryVariable categoryVariable) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      boolean[] categoryNumberPlottable = categoryVariable.getSettings().getCategoryNumberPlottable();
      for (int i = 0; i < categories.length; i++) {
         byte category = categories[i][pixelIndex];
         category = categoryRemapping.remap(category);
         if (categoryNumberPlottable[category]) {
            return byteToFloat(probabilities[i][pixelIndex]);
         }
      }
      return 0;
   }

   public float getProbability(Category category, int pixelIndex, CategoryVariable categoryVariable) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      byte categoryNumber = category.getNumber();
      for (int i = 0; i < categories.length; i++) {
         byte cat = categories[i][pixelIndex];
         cat = categoryRemapping.remap(cat);
         if (cat == categoryNumber) {
            return byteToFloat(probabilities[i][pixelIndex]);
         }
      }
      return 0;
   }

   public float[] getBestDiscriminants(CategoryVariable categoryVariable) {
      float[] result = new float[pixelCount];
      for (int i = 0; i < result.length; i++) {
         result[i] = getBestDiscriminant(i, categoryVariable);
      }
      return result;
   }

   private float getBestDiscriminant(int pixelIndex, CategoryVariable categoryVariable) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      boolean[] categoryNumberPlottable = categoryVariable.getSettings().getCategoryNumberPlottable();
      for (int i = 0; i < categories.length; i++) {
         byte category = categories[i][pixelIndex];
         category = categoryRemapping.remap(category);
         if (categoryNumberPlottable[category]) {
            return byteToFloat(discriminants[i][pixelIndex]);
         }
      }
      return 0;
   }

   /**
    * Returns an array of the absolute value of the differences in discriminant
    * between the two best categories.
    * This difference being small implies categorization doubt.
    *
    * @return an array of discriminant differences
    */
   public float[] getDiscriminantDifference() {
      float[] result = new float[pixelCount];
      for (int i = 0; i < result.length; i++) {
         result[i] = getDiscriminantDifference(i);
      }
      return result;
   }

   /**
    * Returns the discriminant difference between the two best categories, given a pixel index.
    *
    * @param pixelIndex the index of the pixel
    * @return the discriminant difference
    */
   public float getDiscriminantDifference(int pixelIndex) {
      float result = byteToFloat(discriminants[0][pixelIndex]);
      if (discriminants.length > 1) {
         result -= byteToFloat(discriminants[1][pixelIndex]);
      }
      return result;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   private static void writeBytes(ByteBuffer byteBuffer, byte[][] bytes) {
      for (byte[] array : bytes) {
         byteBuffer.put(array);
      }
   }

   private static void readBytes(ByteBuffer byteBuffer, byte[][] bytes) {
      for (byte[] array : bytes) {
         byteBuffer.get(array);
      }
   }
}
