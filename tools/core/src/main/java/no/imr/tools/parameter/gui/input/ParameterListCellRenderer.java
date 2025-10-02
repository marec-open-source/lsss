package no.imr.tools.parameter.gui.input;

import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import java.awt.Component;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ParameterListCellRenderer<V> implements ListCellRenderer<String> {
   private final ValueParameter<V> parameter;
   private final Map<String, V> stringToValue;
   private V renderedValue;
   private final DefaultListCellRenderer renderer = new DefaultListCellRenderer() {
      @Override
      public @Nullable String getToolTipText() {
         return parameter.toTooltip(renderedValue);
      }
   };

   public ParameterListCellRenderer(ValueParameter<V> parameter, List<V> values, List<String> stringValues) {
      this.parameter = parameter;
      stringToValue = HashMap.newHashMap(values.size());
      for (int i = 0; i < values.size(); i++) {
         stringToValue.put(stringValues.get(i), values.get(i));
      }
      renderedValue = parameter.getValue();
   }

   @Override
   public Component getListCellRendererComponent(JList<? extends String> list, String value, int index, boolean isSelected, boolean cellHasFocus) {
      V v = stringToValue.get(value);
      renderedValue = v != null ? v : parameter.stringToValue(value);
      String displayString = parameter.toDisplayString(renderedValue);
      if (displayString.isEmpty()) {
         displayString = " ";
      }

      renderer.getListCellRendererComponent(list, displayString, index, isSelected, cellHasFocus);

      SvgIcon icon = parameter.toIcon(renderedValue);
      if (icon != null) {
         icon.on(renderer);
      } else {
         renderer.setIcon(null);
      }

      return renderer;
   }
}
