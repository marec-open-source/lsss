package no.imr.korona.viewer.variables;

/**
 * Maps from the categories in the Cac0Datagram on file to the categories in the {@link DiscreteVariable} actually used.
 */
@FunctionalInterface
public interface CategoryRemapping {
   byte remap(byte category);

   static CategoryRemapping identity() {
      return category -> category;
   }
}
