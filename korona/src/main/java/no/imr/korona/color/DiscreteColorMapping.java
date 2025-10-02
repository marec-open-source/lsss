package no.imr.korona.color;

import no.imr.korona.data.datagrams.DiscreteCategory;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.util.Collection;
import java.util.List;

/**
 * Maps discrete values to color.
 */
public final class DiscreteColorMapping {
   public static final DiscreteColor INVALID_VALUE = new DiscreteColor(-1, Color.BLACK, "Invalid value");

   private final List<DiscreteColor> discreteColors;
   private final @Nullable DiscreteColor[] valueToDiscreteColor;

   public DiscreteColorMapping(List<DiscreteColor> discreteColors) {
      this.discreteColors = discreteColors;
      int maxValue = discreteColors.stream().mapToInt(DiscreteColor::getValue).max().orElse(-1);
      valueToDiscreteColor = new DiscreteColor[maxValue + 1];
      for (DiscreteColor discreteColor : discreteColors) {
         int value = discreteColor.getValue();
         if (valueToDiscreteColor[value] != null) {
            throw new IllegalArgumentException("Duplicate value: " + value);
         }
         valueToDiscreteColor[value] = discreteColor;
      }
   }

   public DiscreteColor valueToDiscreteColor(int value) {
      if (value >= 0 && value < valueToDiscreteColor.length) {
         DiscreteColor discreteColor = valueToDiscreteColor[value];
         if (discreteColor != null) {
            return discreteColor;
         }
      }
      return INVALID_VALUE;
   }

   public List<DiscreteColor> getDiscreteColors() {
      return discreteColors;
   }

   public int getRGB(int value) {
      return valueToDiscreteColor(value).getRGB();
   }

   public static DiscreteColorMapping makeDiscreteColors(Collection<? extends DiscreteCategory> categories) {
      List<DiscreteColor> discreteColors = categories.stream()
            .map(DiscreteColor::new)
            .toList();
      return new DiscreteColorMapping(discreteColors);
   }
}
