package no.imr.korona.data.datagrams;

import no.imr.korona.computation.categorization.Category;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.viewer.variables.CategoryRemapping;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import org.jspecify.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Result of school categorization.
 */
public class Cas0Datagram extends DatagramPingItem {
   private final int regionId;
   private final byte[] categories;
   private final byte[] discriminants;
   private final byte[] probabilities;

   private @Nullable Cac0Datagram cac0Datagram;

   /**
    * Constructor.
    *
    * @param ntDate        date in NT format
    * @param categoryCount number of categories to include
    * @param regionId      the region id this datagram is created for
    */
   public Cas0Datagram(long ntDate, int categoryCount, int regionId) {
      super(ntDate);

      this.regionId = regionId;
      categories = new byte[categoryCount];
      discriminants = new byte[categoryCount];
      probabilities = new byte[categoryCount];
   }

   public Cas0Datagram(long ntDate, ByteBuffer byteBuffer) {
      super(ntDate);

      int categoryCount = 0xff & byteBuffer.get();
      regionId = byteBuffer.getInt();
      categories = new byte[categoryCount];
      discriminants = new byte[categoryCount];
      probabilities = new byte[categoryCount];
      byteBuffer.get(categories);
      byteBuffer.get(discriminants);
      byteBuffer.get(probabilities);
   }

   public void setCategory(int categoryPriority, byte category, float discriminant, float probability) {
      categories[categoryPriority] = category;
      discriminants[categoryPriority] = Cad0Datagram.floatToByte(discriminant);
      probabilities[categoryPriority] = Cad0Datagram.floatToByte(probability);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put((byte) categories.length);
      byteBuffer.putInt(regionId);
      byteBuffer.put(categories);
      byteBuffer.put(discriminants);
      byteBuffer.put(probabilities);
   }

   @Override
   public DatagramType getDatagramType() {
      return KoronaDatagramPlugin.CAS0;
   }

   @Override
   public void setPingConfiguration(PingConfiguration pingConfiguration) {
      cac0Datagram = pingConfiguration.getConfigurationItem(Cac0Datagram.class);
   }

   public int getCategoryCount() {
      return categories.length;
   }

   /**
    * Gets the category type based on ranking.
    *
    * @param categoryPriority the rank of the category, 0 is the best fit
    * @return the category type
    */
   public byte getCategory(int categoryPriority) {
      return categories[categoryPriority];
   }

   /**
    * Get the probability of a category based on ranking.
    *
    * @param categoryPriority the rank of the category, 0 is the best fit
    * @return the probability
    */
   public float getProbability(int categoryPriority) {
      return Cad0Datagram.byteToFloat(probabilities[categoryPriority]);
   }

   public float getDiscriminant(int categoryPriority) {
      return Cad0Datagram.byteToFloat(discriminants[categoryPriority]);
   }

   public byte getBestCategory() {
      return categories[0];
   }

   public byte getBestCategory(CategoryVariable categoryVariable, byte defaultCategory) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      boolean[] categoryNumberPlottable = categoryVariable.getSettings().getCategoryNumberPlottable();
      float probabilityThreshold = categoryVariable.getProbabilityThreshold();
      for (int i = 0; i < categories.length; i++) {
         byte category = categories[i];
         category = categoryRemapping.remap(category);
         if (categoryNumberPlottable[category] && Cad0Datagram.byteToFloat(probabilities[i]) >= probabilityThreshold) {
            return category;
         }
      }
      return defaultCategory;
   }

   public float getBestProbability() {
      return Cad0Datagram.byteToFloat(probabilities[0]);
   }

   public float getBestDiscriminant() {
      return Cad0Datagram.byteToFloat(discriminants[0]);
   }

   /**
    * {@return the region ID of the region this datagram was made from (in the same ping)}
    */
   public int getRegionId() {
      return regionId;
   }

   public float getProbability(Category category, CategoryVariable categoryVariable) {
      CategoryRemapping categoryRemapping = categoryVariable.getSettings().getCategoryRemapping(cac0Datagram);
      byte categoryNumber = category.getNumber();
      for (int i = 0; i < categories.length; i++) {
         byte cat = categories[i];
         cat = categoryRemapping.remap(cat);
         if (cat == categoryNumber) {
            return Cad0Datagram.byteToFloat(probabilities[i]);
         }
      }
      return 0;
   }
}
