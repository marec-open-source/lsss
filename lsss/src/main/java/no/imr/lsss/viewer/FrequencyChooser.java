package no.imr.lsss.viewer;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.Utils;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Toolbar buttons for selecting frequency.
 */
final class FrequencyChooser {
   private final InterpretationSettings interpretationSettings;
   private final JButton previousButton = MiscIcons.NARROW_LEFT.on(new JButton());
   private final JButton nextButton = MiscIcons.NARROW_RIGHT.on(new JButton());
   private final JComboBox<FrequencyItem> comboBox = new JComboBox<>();

   FrequencyChooser(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;

      previousButton.setToolTipText("Show previous frequency");
      previousButton.addActionListener(e -> interpretationSettings.shiftChannel(-1));

      nextButton.setToolTipText("Show next frequency");
      nextButton.addActionListener(e -> interpretationSettings.shiftChannel(1));

      comboBox.addActionListener(e -> {
         FrequencyItem frequencyItem = (FrequencyItem) comboBox.getSelectedItem();
         if (frequencyItem != null) {
            interpretationSettings.setChannel(frequencyItem.channel);
         }
      });

      interpretationSettings.getDataFileChangeManager().addListener(GuiListeners.coalescingLater(this::updateModel));
      interpretationSettings.getChannelChangeManager().addListener(GuiListeners.coalescingLater(this::updateSelectedItem));

      updateModel();
   }

   private void updateSelectedItem() {
      int i = interpretationSettings.getChannel() - 1;
      FrequencyItem item = i < comboBox.getItemCount() ? comboBox.getItemAt(i) : null;
      comboBox.setSelectedItem(item);
   }

   private void updateModel() {
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      boolean hasData = !dataFileSet.isEmpty();
      comboBox.setEnabled(hasData);

      List<RawFileTransducer> transducers = dataFileSet.getRawFileConfiguration().getTransducers();
      boolean hasMultipleItems = hasData && transducers.size() > 1;
      nextButton.setEnabled(hasMultipleItems);
      previousButton.setEnabled(hasMultipleItems);

      List<FrequencyItem> frequencyItems = IntStream.rangeClosed(1, transducers.size())
            .mapToObj(channel -> new FrequencyItem(channel, transducers.get(channel - 1).getFrequency()))
            .toList();

      comboBox.setModel(new ComboBoxListModel<>(null, frequencyItems));
      updateSelectedItem();
   }

   List<JComponent> getToolBarComponents() {
      return List.of(previousButton, comboBox, nextButton);
   }

   /**
    * ComboBox entry representing a frequency.
    */
   private record FrequencyItem(int channel, float frequency) {
      @Override
      public String toString() {
         return Utils.hzToKHz(frequency) + " kHz";
      }
   }
}
