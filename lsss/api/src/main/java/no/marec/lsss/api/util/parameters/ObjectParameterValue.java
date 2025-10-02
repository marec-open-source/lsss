package no.marec.lsss.api.util.parameters;

import org.jspecify.annotations.Nullable;

/**
 * Used when displaying a list of allowed values for a parameter.
 */
public interface ObjectParameterValue {
   /**
    * {@return the string representation of this value}
    */
   @Override
   String toString();

   /**
    * {@return the label}
    */
   default String getDisplayLabel() {
      return toString();
   }

   /**
    * {@return the tooltip}
    */
   default @Nullable String getTooltip() {
      return null;
   }
}
