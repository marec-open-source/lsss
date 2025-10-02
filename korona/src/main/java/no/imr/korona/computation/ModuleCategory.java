package no.imr.korona.computation;

import no.imr.tools.swing.ColorUtils;

import java.awt.Color;

public enum ModuleCategory {
   MODIFIES_CHANNELS("Modifies channels", ColorUtils.VIOLET),
   MODIFIES_DATA("Modifies data", ColorUtils.LIGHTSKYBLUE),
   ADDS_DATAGRAM("Adds datagram", ColorUtils.LIGHTGREEN),
   REMOVES_DATAGRAM("Removes datagram", ColorUtils.LIGHTCORAL),
   NO_MODIFICATION("No modifications to data", ColorUtils.LIGHTGREY),
   TEMPORARY_COMPUTATIONS("Temporary computations", ColorUtils.KHAKI);

   private final String label;
   private final Color color;

   ModuleCategory(String label, Color color) {
      this.label = label;
      this.color = color;
   }

   public String getLabel() {
      return label;
   }

   public Color getColor() {
      return color;
   }
}
