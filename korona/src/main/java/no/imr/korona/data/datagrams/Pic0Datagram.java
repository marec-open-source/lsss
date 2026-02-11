package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.PingConfiguration;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Plankton inversion configuration.
 */
public final class Pic0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = DatagramType.simple("PIC0", Pic0Datagram::new);

   private static final PlanktonCategory UNCATEGORIZED = new PlanktonCategory("Uncategorized", "Uncat", 0, Color.WHITE);
   private static final PlanktonCategory NO_CATEGORY = new PlanktonCategory("Other", "Other", 1, Color.CYAN);

   private final NavigableMap<Byte, PlanktonCategory> idToCategoryMap = new TreeMap<>();

   public Pic0Datagram(long ntDate) {
      super(ntDate);

      addCategory(UNCATEGORIZED);
      addCategory(NO_CATEGORY);
   }

   public Pic0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      this(ntDate);

      int numberOfCategories = byteBuffer.getInt();
      for (int i = 0; i < numberOfCategories; i++) {
         addCategory(new PlanktonCategory(byteBuffer));
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.putInt(idToCategoryMap.size() - 2); //no need to write uncat and nocat
      for (PlanktonCategory planktonCategory : idToCategoryMap.values()) {
         if (!planktonCategory.isSpecial()) {
            planktonCategory.putBuffer(byteBuffer);
         }
      }
   }

   public Collection<PlanktonCategory> getPlanktonCategories() {
      return idToCategoryMap.values();
   }

   public Map<Byte, PlanktonCategory> getIdToCategoryMap() {
      return idToCategoryMap;
   }

   /**
    * Return the category to be used as filler category. Return the specified filler category,
    * or the category with number 0 if no filler category is specified.
    *
    * @return the filler category
    */
   public static PlanktonCategory getFillerCategory() {
      return UNCATEGORIZED;
   }

   public static PlanktonCategory getNotPlanktonCategory() {
      return NO_CATEGORY;
   }

   /**
    * Add a new plankton category to the list of categories. The user is responsible for assigning unique id numbers to
    * the categories.
    *
    * @param category a plankton category to be added
    */
   public void addCategory(PlanktonCategory category) {
      idToCategoryMap.put(category.getNumber(), category);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public @Nullable String getIncompatibility(PingConfiguration pingConfiguration) {
      Pic0Datagram pic0Datagram = pingConfiguration.getConfigurationItem(Pic0Datagram.class);
      if (pic0Datagram != null && toByteBufferExcludingHeader().equals(pic0Datagram.toByteBufferExcludingHeader())) {
         return null;
      } else {
         return "Different plankton configuration";
      }
   }

   public static final class PlanktonCategory implements DiscreteCategory, Comparable<PlanktonCategory> {
      private final String name;
      private final String legend;
      private final byte number;
      private final Color color;

      public PlanktonCategory(String name, String legend, int number, Color color) {
         if (number > 127 || number < 0) {
            throw new IllegalArgumentException("number = " + number);
         }

         this.name = name;
         this.legend = legend;
         this.number = (byte) number;
         this.color = color;
      }

      private PlanktonCategory(ByteBuffer byteBuffer) throws DatagramFormatException {
         name = ByteBufferUtils.readCString(byteBuffer);
         legend = ByteBufferUtils.readCString(byteBuffer);
         number = byteBuffer.get();
         if (number < 0) {
            throw new DatagramFormatException("number: " + number);
         }
         int red = 0xff & byteBuffer.get();
         int green = 0xff & byteBuffer.get();
         int blue = 0xff & byteBuffer.get();
         color = new Color(red, green, blue);
      }

      private void putBuffer(ByteBuffer byteBuffer) {
         ByteBufferUtils.writeCString(byteBuffer, name);
         ByteBufferUtils.writeCString(byteBuffer, legend);
         byteBuffer.put(number);
         byteBuffer.put((byte) color.getRed());
         byteBuffer.put((byte) color.getGreen());
         byteBuffer.put((byte) color.getBlue());
      }

      @Override
      public String toString() {
         return name;
      }

      @Override
      public String getName() {
         return name;
      }

      @Override
      public String getLegend() {
         return legend;
      }

      @Override
      public byte getNumber() {
         return number;
      }

      @Override
      public Color getColor() {
         return color;
      }

      /**
       * Tests if this category represents the unknown category or the not categorised.
       *
       * @return true if unknown, false if represents an actual plankton category
       */
      public boolean isSpecial() {
         return number <= 1;
      }

      @Override
      public boolean equals(@Nullable Object obj) {
         if (this == obj) {
            return true;
         }
         return obj instanceof PlanktonCategory that
               && number == that.number;
      }

      @Override
      public int hashCode() {
         return number;
      }

      @Override
      public int compareTo(PlanktonCategory o) {
         return Byte.compare(number, o.number);
      }
   }
}
