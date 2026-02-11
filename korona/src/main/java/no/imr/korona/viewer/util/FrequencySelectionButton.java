package no.imr.korona.viewer.util;

import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.ViewHolder;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Insets;
import java.util.List;

public final class FrequencySelectionButton {
   private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

   private final FrequencySelectionPanel frequencySelectionPanel;
   private final int channel;
   private final int kHz;
   private boolean selected;
   private boolean highlighted;
   private final ViewHolder<View> viewHolder;

   private static final List<Color> COLORS = List.of(
         ColorUtils.RED,
         ColorUtils.BLUE,
         ColorUtils.DARKSEAGREEN,
         ColorUtils.CHOCOLATE,
         ColorUtils.DARKMAGENTA,
         ColorUtils.LIGHTSEAGREEN
         // todo: More colors. Color should correspond to frequency instead of channel
   );

   FrequencySelectionButton(FrequencySelectionPanel frequencySelectionPanel, int channel, int kHz, Color color, String tooltip, boolean selected) {
      this.frequencySelectionPanel = frequencySelectionPanel;
      this.channel = channel;
      this.kHz = kHz;
      this.selected = selected;
      viewHolder = new ViewHolder<>(() -> new View(this, color, tooltip));
   }

   public static Color channelIndexToColor(int channelIndex) {
      return COLORS.get(channelIndex % COLORS.size());
   }

   int getKHz() {
      return kHz;
   }

   boolean isSelected() {
      return selected;
   }

   void setSelected(boolean selected) {
      if (this.selected == selected) {
         return;
      }
      this.selected = selected;
      viewHolder.ifView(View::update);
      frequencySelectionPanel.listener.listen();

      if (frequencySelectionPanel.singleSelection && selected) {
         frequencySelectionPanel.map.values().forEach(button -> {
            if (button != this) {
               button.setSelected(false);
            }
         });
      }
   }

   void setHighlighted(boolean highlighted) {
      if (this.highlighted == highlighted) {
         return;
      }
      this.highlighted = highlighted;
      viewHolder.ifView(View::update);
   }

   boolean isHighlighted() {
      return highlighted;
   }

   View getView() {
      return viewHolder.getView();
   }

   static final class View implements ViewHolder.View {
      private final FrequencySelectionButton frequencySelectionButton;
      private final JPanel panel = new JPanel(new BorderLayout());
      private final JToggleButton button;

      private View(FrequencySelectionButton frequencySelectionButton, Color color, String tooltip) {
         this.frequencySelectionButton = frequencySelectionButton;
         String text = Integer.toString(frequencySelectionButton.kHz);
         button = frequencySelectionButton.frequencySelectionPanel.singleSelection
               ? new JRadioButton(text)
               : new JCheckBox(text);
         button.setMargin(EMPTY_INSETS);
         button.setBackground(Color.WHITE);
         button.setForeground(color);
         button.setToolTipText(tooltip);
         button.addItemListener(_ -> {
            if (frequencySelectionButton.frequencySelectionPanel.singleSelection && frequencySelectionButton.selected && !button.isSelected()) {
               button.setSelected(true);
            } else {
               frequencySelectionButton.setSelected(button.isSelected());
            }
         });
         if (!frequencySelectionButton.frequencySelectionPanel.singleSelection) {
            button.addMouseListener(new PopupMenuMouseListener(_ -> makePopupMenu()));
         }

         panel.add(button);

         update();
      }

      private JPopupMenu makePopupMenu() {
         JPopupMenu menu = new JPopupMenu();

         JMenuItem allOff = menu.add("All off");
         allOff.addActionListener(_ -> setAll(1, Integer.MAX_VALUE, false));

         JMenuItem allOn = menu.add("All on");
         allOn.addActionListener(_ -> setAll(1, Integer.MAX_VALUE, true));

         menu.addSeparator();

         JMenuItem lowerOff = menu.add("Lower off");
         lowerOff.addActionListener(_ -> setAll(1, frequencySelectionButton.channel, false));

         JMenuItem lowerOn = menu.add("Lower on");
         lowerOn.addActionListener(_ -> setAll(1, frequencySelectionButton.channel, true));

         menu.addSeparator();

         JMenuItem higherOff = menu.add("Higher off");
         higherOff.addActionListener(_ -> setAll(frequencySelectionButton.channel + 1, Integer.MAX_VALUE, false));

         JMenuItem higherOn = menu.add("Higher on");
         higherOn.addActionListener(_ -> setAll(frequencySelectionButton.channel + 1, Integer.MAX_VALUE, true));

         return menu;
      }

      private void setAll(int beginChannel, int endChannel, boolean selected) {
         frequencySelectionButton.frequencySelectionPanel.setAll(beginChannel, endChannel, selected);
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      private void update() {
         button.setSelected(frequencySelectionButton.selected);

         if (frequencySelectionButton.highlighted) {
            panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
            button.setBackground(ColorUtils.LIGHTGREY);
            if (!frequencySelectionButton.frequencySelectionPanel.singleSelection) {
               button.setSelected(true);
            }
         } else {
            panel.setBorder(BorderFactory.createEmptyBorder());
            button.setBackground(Color.WHITE);
         }
      }
   }
}
