package no.imr.korona.data.datamanager.labelling;

import no.imr.tools.misc.HtmlStringBuilder;

import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.util.Collection;

public final class DataFileLabelUtils {
   private DataFileLabelUtils() {
   }

   public static JMenuItem menuItem(DataFileLabel label, ActionListener actionListener) {
      JMenuItem item = new JMenuItem("<html>" + label.toHtml());
      if (!label.description.isEmpty()) {
         item.setToolTipText(label.description);
      }
      item.addActionListener(actionListener);
      return item;
   }

   public static String addLabelsText(String text, Collection<DataFileLabel> labels) {
      if (labels.isEmpty()) {
         return text;
      }
      HtmlStringBuilder textBuilder = new HtmlStringBuilder()
            .html("<span style='white-space: nowrap;'>")
            .text(text);
      for (DataFileLabel label : labels) {
         textBuilder.html("&ensp;").html(label.toHtml());
      }
      return textBuilder
            .html("</span>")
            .build();
   }

   public static void addLabelsTooltip(HtmlStringBuilder tooltip, Collection<DataFileLabel> labels) {
      if (labels.isEmpty()) {
         return;
      }
      tooltip.html("<br>Labels:");
      for (DataFileLabel label : labels) {
         tooltip.html("<br> • ").html(label.toHtml());
         if (!label.description.isEmpty()) {
            tooltip.text(" - ").text(label.description);
         }
      }
   }
}
