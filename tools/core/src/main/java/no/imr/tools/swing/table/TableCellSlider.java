package no.imr.tools.swing.table;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractCellEditor;
import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public final class TableCellSlider extends JSlider {
   private TableCellSlider() {
      setBackground(Color.WHITE);
   }

   /**
    * Overridden for performance reasons. See {@link javax.swing.table.DefaultTableCellRenderer}.
    */
   @Override
   public void invalidate() {
   }

   @Override
   public void validate() {
   }

   @Override
   public void revalidate() {
   }

   @Override
   public void repaint(long tm, int x, int y, int width, int height) {
   }

   @Override
   public void repaint(Rectangle r) {
   }

   @Override
   public void repaint() {
   }

   public static class SliderSetting {
      private int value;
      private int maxValue;

      public SliderSetting() {
      }

      public int getValue() {
         return value;
      }

      public void setValue(int value) {
         this.value = value;
      }

      public int getMaxValue() {
         return maxValue;
      }

      public void setMaxValue(int maxValue) {
         this.maxValue = maxValue;
      }

      private void apply(JSlider slider) {
         BoundedRangeModel m = slider.getModel();
         m.setRangeProperties(value, m.getExtent(), 0, maxValue, m.getValueIsAdjusting());
      }
   }

   public static final class FloatSliderSetting extends SliderSetting {
      private final float floatValueMin;
      private final float floatValueStep;

      public FloatSliderSetting(float floatValue, float floatValueMin, float floatValueMax, float floatValueStep) {
         this.floatValueMin = floatValueMin;
         this.floatValueStep = floatValueStep;
         setMaxValue(Math.round((floatValueMax - floatValueMin) / floatValueStep));
         setValue(Math.round((floatValue - floatValueMin) / floatValueStep));
      }

      public float getFloatValue() {
         return floatValueMin + floatValueStep * getValue();
      }

      @Override
      public String toString() {
         return getClass().getSimpleName() + " [" + getValue() + " / " + getMaxValue()
               + ", " + floatValueMin + ", " + floatValueStep + "]";
      }
   }

   public static final class Renderer implements TableCellRenderer {
      private final TableCellSlider slider = new TableCellSlider();

      public Renderer() {
      }

      @Override
      public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
         SliderSetting sliderSetting = (SliderSetting) value;
         slider.setEnabled(table.isCellEditable(row, column));
         sliderSetting.apply(slider);
         return slider;
      }
   }

   public static final class Editor extends AbstractCellEditor implements TableCellEditor {
      private final TableCellSlider slider = new TableCellSlider();
      private SliderSetting sliderSetting = new SliderSetting();
      private @Nullable JTable table;
      private int row;
      private int column;
      private boolean mouseInside;

      public Editor() {
         slider.addChangeListener(_ -> {
            if (table == null) {
               return;
            }
            sliderSetting.setValue(slider.getValue());
            table.getModel().setValueAt(sliderSetting, row, column);
            possiblyStopEditing();
         });
         slider.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               mouseInside = true;
            }

            @Override
            public void mouseExited(MouseEvent e) {
               mouseInside = false;
               possiblyStopEditing();
            }
         });
      }

      private void possiblyStopEditing() {
         if (!mouseInside && !slider.getValueIsAdjusting()) {
            fireEditingStopped();
         }
      }

      @Override
      public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
         this.table = table;
         sliderSetting = (SliderSetting) value;
         this.row = row;
         this.column = column;
         mouseInside = true;
         sliderSetting.apply(slider);
         return slider;
      }

      @Override
      public Object getCellEditorValue() {
         return sliderSetting;
      }
   }
}
