package no.imr.korona.data.datamanager.labelling;

import com.google.common.html.HtmlEscapers;
import no.imr.tools.swing.ColorUtils;

import java.awt.Color;

public final class DataFileLabel implements Comparable<DataFileLabel> {
   public final String title;
   public final String description;
   public final Color backgroundColor;
   public final Color textColor;

   public DataFileLabel(String title, String description, Color backgroundColor) {
      this.title = title;
      this.description = description;
      this.backgroundColor = backgroundColor;
      textColor = ColorUtils.contrastingBlackOrWhite(backgroundColor);
   }

   @Override
   public String toString() {
      return title;
   }

   @Override
   public int compareTo(DataFileLabel other) {
      return title.compareToIgnoreCase(other.title);
   }

   public String toHtml() {
      return "<span style='color: " + ColorUtils.colorToHex(textColor)
            + "; background-color: " + ColorUtils.colorToHex(backgroundColor) + ";'>&nbsp;"
            + HtmlEscapers.htmlEscaper().escape(title)
            + "&nbsp;</span>";
   }
}
