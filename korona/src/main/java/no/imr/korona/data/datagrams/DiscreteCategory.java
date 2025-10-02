package no.imr.korona.data.datagrams;

import java.awt.Color;

/**
 * Interface for discrete categories.
 */
public interface DiscreteCategory {
   /**
    * Returns the name of this category.
    *
    * @return the name of this category
    */
   String getName();

   /**
    * Returns the legend of this category.
    *
    * @return the legend of this category
    */
   String getLegend();

   /**
    * Returns the number of this category.
    *
    * @return the number of this category
    */
   byte getNumber();

   /**
    * Returns the color of this category.
    *
    * @return the color of this category
    */
   Color getColor();
}
