package no.imr.korona.viewer.coloring;

import java.awt.Color;

/**
 * Utilities for classes that converts values (0..1) to color.
 */
public final class ValueColor {
   /**
    * Color for no data.
    */
   static final int NO_DATA_RGB = Color.BLACK.getRGB();

   /**
    * No data as byte.
    */
   public static final byte NO_DATA_BYTE = Byte.MIN_VALUE;

   /**
    * No data as float.
    */
   public static final float NO_DATA_FLOAT = Float.NaN;

   private ValueColor() {
   }

   /**
    * Test for equality with {@link #NO_DATA_FLOAT}.
    * NB: cannot use == operator with {@link Float#NaN}.
    *
    * @param value the value to test
    * @return {@code true} if equal
    */
   public static boolean isNoDataFloat(float value) {
      return Float.isNaN(value);
   }
}
