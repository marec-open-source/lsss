package no.imr.tools.swing;

import com.google.common.html.HtmlEscapers;
import org.jspecify.annotations.Nullable;

public final class TableToolTipBuilder {
   private final StringBuilder stringBuilder = new StringBuilder(1024);

   public TableToolTipBuilder() {
      stringBuilder.append("""
            <html>
            <style>
               td { white-space: nowrap; }
               td.unavailable { font-style: italic; color: gray; }
            </style>
            <table cellpadding=0 cellspacing=0>
            """);
   }

   public TableToolTipBuilder addHtml(String html) {
      stringBuilder.append(html);
      return this;
   }

   public TableToolTipBuilder addLine(String line) {
      stringBuilder.append("<tr><td colspan=2>").append(htmlEscape(line)).append("</td></tr>");
      return this;
   }

   public TableToolTipBuilder addVerticalSpace() {
      stringBuilder.append("<tr style='font-size: 0.5em'></tr>");
      return this;
   }

   public TableToolTipBuilder addRow(String name, @Nullable String... values) {
      stringBuilder.append("<tr><td style='margin-right: 10px;'");
      if (values.length == 0 || values[0] == null) {
         stringBuilder.append(" class=unavailable");
      }
      stringBuilder.append('>').append(htmlEscape(name)).append("</td>");
      for (String value : values) {
         stringBuilder.append("<td align=right>").append(value != null ? htmlEscape(value) : "").append("</td>");
      }
      stringBuilder.append("</tr>");
      return this;
   }

   public String build() {
      stringBuilder.append("</table>");

      return stringBuilder.toString();
   }

   private static String htmlEscape(String text) {
      return HtmlEscapers.htmlEscaper().escape(text).replace(" ", "&nbsp;");
   }
}
