package no.imr.korona.data.datagrams;

import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.tools.swing.ColorUtils;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Categorization configuration.
 */
public final class Cac0Datagram extends DatagramPingItem {
   public static final DatagramType TYPE = new DatagramType.Simple("CAC0", Cac0Datagram::new);

   private final Map<Byte, Category> numberToCategory = new HashMap<>();
   private final Map<String, Category> nameToCategory = new HashMap<>();
   private final List<Category> categories = new ArrayList<>();

   public Cac0Datagram(long ntDate) {
      super(ntDate);
   }

   public Cac0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      int categoryCount = byteBuffer.get();
      for (int i = 0; i < categoryCount; i++) {
         addCategory(new Category(byteBuffer));
      }
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      byteBuffer.put((byte) categories.size());
      for (Category category : categories) {
         category.putBuffer(byteBuffer);
      }
   }

   /**
    * Adds a new category.
    *
    * @param number the number
    * @param name   the name
    * @param legend the legend
    * @param color  the color
    */
   public void addCategory(byte number, String name, String legend, Color color) {
      if (number < 0) {
         throw new IllegalArgumentException("Illegal category number for category " + name +
               ". Categories should have a number between 0 and 127");
      }
      addCategory(new Category(name, legend, number, color));
   }

   public void addCategory(DiscreteCategory category) {
      addCategory(category.getNumber(), category.getName(), category.getLegend(), category.getColor());
   }

   public void addCategory(Category category) {
      categories.add(category);
      numberToCategory.put(category.getNumber(), category);
      nameToCategory.put(category.getName(), category);
   }

   /**
    * Returns the Category object for a specified category number.
    *
    * @param categoryNumber the number
    * @return the Category object
    */
   public Category numberToCategory(byte categoryNumber) {
      Category category = numberToCategory.get(categoryNumber);
      if (category == null) {
         throw new IllegalArgumentException("Category number: " + categoryNumber);
      }
      return category;
   }

   public @Nullable Category nameToCategory(String name) {
      return nameToCategory.get(name);
   }

   public Category getUnknownCategory() {
      Category unknownCategory = nameToCategory(Configurator.UNKNOWN_CATEGORY_NAME);
      if (unknownCategory == null) {
         throw new IllegalStateException("No category with name " + Configurator.UNKNOWN_CATEGORY_NAME);
      }
      return unknownCategory;
   }

   public List<Category> getCategories() {
      return categories;
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   @Override
   public @Nullable String getIncompatibility(PingConfiguration pingConfiguration) {
      Cac0Datagram cac0Datagram = pingConfiguration.getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram != null && toByteBufferExcludingHeader().equals(cac0Datagram.toByteBufferExcludingHeader())) {
         return null;
      } else {
         return "Different categorization configuration";
      }
   }

   public static final class Category implements DiscreteCategory {
      private final String name;
      private final String legend;
      private final byte number;
      private final Color color;

      private Category(String name, String legend, byte number, Color color) {
         this.name = name;
         this.legend = legend;
         this.number = number;
         this.color = color;
      }

      private Category(ByteBuffer byteBuffer) throws DatagramFormatException {
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
         return number + ", " + name + ", " + legend + ", " + ColorUtils.colorToNameOrHex(color);
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
       * Tests whether this is the unknown category.
       *
       * @return {@code true} if this is the unknown category
       */
      public boolean isUnknown() {
         return name.equals(Configurator.UNKNOWN_CATEGORY_NAME);
      }
   }
}
