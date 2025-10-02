package no.imr.tools.swing.table;

import org.jspecify.annotations.Nullable;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;

/**
 * For controlling the appearance of a string in a table cell.
 */
public final class TableCellString {
   private TableCellString() {
   }

   public record RenderSettings(
         @Nullable String text,
         @Nullable String toolTipText,
         @Nullable Color foreground
   ) {
   }

   public static final class Renderer extends DefaultTableCellRenderer {
      private final RenderSettings defaultRenderSettings;

      public Renderer(RenderSettings defaultRenderSettings) {
         this.defaultRenderSettings = defaultRenderSettings;
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
         RenderSettings renderSettings;
         if (value instanceof RenderSettings settings) {
            renderSettings = settings;
            setText(renderSettings.text);
         } else {
            renderSettings = defaultRenderSettings;
            setText(value != null ? value.toString() : "");
         }

         setToolTipText(renderSettings.toolTipText);
         if (renderSettings.foreground != null) {
            setForeground(renderSettings.foreground);
         }

         return this;
      }
   }
}
