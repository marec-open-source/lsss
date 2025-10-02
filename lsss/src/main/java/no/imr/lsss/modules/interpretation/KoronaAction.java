package no.imr.lsss.modules.interpretation;

import no.imr.tools.Utils;

enum KoronaAction {
   CURRENT_FREQUENCY("<", "Apply categorization to %s on current frequency"),
   ALL_FREQUENCIES("∗", "Apply categorization to %s on all frequencies"),
   FREQUENCY_RESPONSE_FUNCTION("F", "Apply frequency response function to %s on other frequencies");

   private final String symbol;
   private final String tooltip;

   KoronaAction(String symbol, String tooltip) {
      this.symbol = symbol;
      this.tooltip = tooltip;
   }

   String getSymbol() {
      return symbol;
   }

   String getToolTip(String species) {
      return Utils.format(tooltip, species);
   }
}
