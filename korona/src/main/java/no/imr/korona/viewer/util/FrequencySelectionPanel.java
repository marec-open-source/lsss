package no.imr.korona.viewer.util;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.listening.Listener;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WrappingFlowLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class FrequencySelectionPanel {
   private static final Dimension MINIMUM_SIZE = new Dimension(10, 10);

   final Listener listener;
   final boolean singleSelection;
   ImmutableMap<Integer, FrequencySelectionButton> map = ImmutableMap.of();
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   public FrequencySelectionPanel(Listener listener) {
      this(listener, false);
   }

   public FrequencySelectionPanel(Listener listener, boolean singleSelection) {
      this.listener = listener;
      this.singleSelection = singleSelection;
   }

   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   public Collection<Integer> getChannels() {
      return map.keySet();
   }

   public boolean isChannelSelected(int channel) {
      FrequencySelectionButton frequencySelectionButton = map.get(channel);
      return frequencySelectionButton != null && frequencySelectionButton.isSelected();
   }

   public void setChannelSelected(int channel, boolean selected) {
      FrequencySelectionButton frequencySelectionButton = map.get(channel);
      if (frequencySelectionButton != null) {
         frequencySelectionButton.setSelected(selected);
      }
   }

   public void update(RawFileConfiguration rawFileConfiguration, int highlightedChannel) {
      boolean hasSelected = false;
      ImmutableMap.Builder<Integer, FrequencySelectionButton> mapBuilder = ImmutableMap.builder();
      Set<Integer> deselectedKHz = map.values().stream()
            .filter(Predicate.not(FrequencySelectionButton::isSelected))
            .map(FrequencySelectionButton::getKHz)
            .collect(Collectors.toSet());
      for (int channelIndex = 0; channelIndex < rawFileConfiguration.getTransducerCount(); channelIndex++) {
         int channel = channelIndex + 1;
         RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channelIndex);
         String tooltip = new HtmlStringBuilder()
               .text(transducer.getChannelId())
               .html("<br>").text(Math.round(transducer.getFrequency())).text(" Hz")
               .build();
         boolean selected = !deselectedKHz.contains(transducer.getKHz());
         if (singleSelection && hasSelected) {
            selected = false;
         }
         hasSelected |= selected;
         Color color = FrequencySelectionButton.channelIndexToColor(channelIndex);
         FrequencySelectionButton frequencySelectionButton = new FrequencySelectionButton(this,
               channel, transducer.getKHz(), color, tooltip, selected);
         if (highlightedChannel == channel) {
            frequencySelectionButton.setHighlighted(true);
         }
         mapBuilder.put(channel, frequencySelectionButton);
      }
      map = mapBuilder.build();
      if (singleSelection && !hasSelected && !map.isEmpty()) {
         map.values().iterator().next().setSelected(true);
      }
      viewHolder.ifViewDelayed(viewHolder, View::update);
   }

   public void setHighlighted(int highlightedChannel) {
      map.forEach((channel, frequencySelectionButton) -> {
         frequencySelectionButton.setHighlighted(channel == highlightedChannel);
      });
   }

   void setAll(int beginChannel, int endChannel, boolean selected) {
      endChannel = Math.min(endChannel, map.size() + 1);
      for (int channel = beginChannel; channel < endChannel; channel++) {
         FrequencySelectionButton frequencySelectionButton = map.get(channel);
         if (frequencySelectionButton != null && !frequencySelectionButton.isHighlighted()) {
            frequencySelectionButton.setSelected(selected);
         }
      }
   }

   private static final class View implements ViewHolder.View {
      private final FrequencySelectionPanel frequencySelectionPanel;
      private final JPanel panel = new JPanel(new WrappingFlowLayout(FlowLayout.CENTER, 0, 0));

      private View(FrequencySelectionPanel frequencySelectionPanel) {
         this.frequencySelectionPanel = frequencySelectionPanel;
         panel.setBackground(Color.WHITE);
         panel.setMinimumSize(MINIMUM_SIZE);
         update();
      }

      @Override
      public JComponent getComponent() {
         return panel;
      }

      private void update() {
         panel.removeAll();
         frequencySelectionPanel.map.forEach((channel, frequencySelectionButton) -> {
            panel.add(frequencySelectionButton.getView().getComponent());
         });
         panel.revalidate();
         panel.repaint();
      }
   }
}
