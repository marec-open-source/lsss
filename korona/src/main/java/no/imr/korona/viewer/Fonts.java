package no.imr.korona.viewer;

import no.imr.tools.swing.UiUtils;

import java.awt.Font;

public class Fonts {
   public static final Fonts DEFAULT = new Fonts(UiUtils.labelFont());

   private final Font normal;

   protected Fonts(Font normal) {
      this.normal = normal;
   }

   public Font getNormal() {
      return normal;
   }

   public Font getCategorizationLegendFont() {
      return normal;
   }
}
