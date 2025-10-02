package no.imr.korona.color;

import no.imr.korona.data.datagrams.DiscreteCategory;
import org.jspecify.annotations.Nullable;

import java.awt.Color;

/**
 * Association between a discrete value and a color.
 */
public final class DiscreteColor implements Comparable<DiscreteColor> {
   private final int value;
   private final Color color;
   private final float hue;
   private final float saturation;
   private final String name;

   public DiscreteColor(DiscreteCategory category) {
      this(category.getNumber(), category.getColor(), category.getName());
   }

   public DiscreteColor(int value, Color color, String name) {
      this.value = value;
      this.color = color;
      float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
      hue = hsb[0];
      saturation = hsb[1];
      this.name = name;
   }

   public int getValue() {
      return value;
   }

   public String getName() {
      return name;
   }

   public int getRGB() {
      return color.getRGB();
   }

   public Color getColor() {
      return color;
   }

   public float getHue() {
      return hue;
   }

   public float getSaturation() {
      return saturation;
   }

   @Override
   public String toString() {
      return name;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof DiscreteColor that
            && value == that.value;
   }

   @Override
   public int hashCode() {
      return value;
   }

   @Override
   public int compareTo(DiscreteColor o) {
      return Integer.compare(value, o.value);
   }
}
