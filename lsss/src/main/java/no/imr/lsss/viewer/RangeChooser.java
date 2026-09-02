package no.imr.lsss.viewer;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.Utils;
import no.imr.tools.math.MathUtils;
import no.imr.tools.swing.ComboBoxListModel;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Toolbar buttons for selecting range.
 */
final class RangeChooser {
   private static final float[] RANGES = {50, 100, 150, 200, 250, 300, 400, 500, 600, 700, 800, 900, 1000, 1250, 1500, 1750, 2000, 5000};

   private final InterpretationSettings interpretationSettings;
   private final JButton previousButton = MiscIcons.NARROW_LEFT.on(new JButton());
   private final JButton nextButton = MiscIcons.NARROW_RIGHT.on(new JButton());
   private final JComboBox<RangeItem> comboBox = new JComboBox<>();
   private boolean skip;

   RangeChooser(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;

      previousButton.setToolTipText("Show previous range");
      previousButton.addActionListener(_ -> shiftSelectedIndex(-1));
      GuiUtils.setAccelerator(previousButton, Shortcuts.PREVIOUS_RANGE);

      nextButton.setToolTipText("Show next range");
      nextButton.addActionListener(_ -> shiftSelectedIndex(1));
      GuiUtils.setAccelerator(nextButton, Shortcuts.NEXT_RANGE);

      comboBox.addActionListener(_ -> {
         if (skip) {
            return;
         }
         interpretationSettings.getPelagicZSettings().setZ(0, RANGES[comboBox.getSelectedIndex()]);
      });

      GuiListeners.coalescingLater(this::updateModel).addToAndNotify(
            interpretationSettings.getDataFileChangeManager(),
            interpretationSettings.getPelagicZSettings().maxZ
      );
   }

   private void shiftSelectedIndex(int shift) {
      int selectedIndex = MathUtils.mod(comboBox.getSelectedIndex() + shift, comboBox.getItemCount());
      comboBox.setSelectedIndex(selectedIndex);
   }

   private int findItemCount() {
      if (interpretationSettings.getDataFileSet().isEmpty()) {
         return 0;
      }

      float maxZ = interpretationSettings.getPelagicZSettings().getMaxZRange().max();
      for (int i = 0; i < RANGES.length; i++) {
         if (RANGES[i] >= maxZ) {
            return i + 1;
         }
      }
      return RANGES.length;
   }

   private void updateModel() {
      DataFileSet dataFileSet = interpretationSettings.getDataFileSet();
      boolean hasData = !dataFileSet.isEmpty();
      comboBox.setEnabled(hasData);

      int itemCount = findItemCount();
      boolean hasMultipleItems = itemCount > 1;
      nextButton.setEnabled(hasMultipleItems);
      previousButton.setEnabled(hasMultipleItems);

      List<RangeItem> rangeItems = new ArrayList<>(itemCount);
      for (int i = 0; i < itemCount; i++) {
         rangeItems.add(new RangeItem(RANGES[i]));
      }

      skip = true;
      comboBox.setModel(new ComboBoxListModel<>(null, rangeItems));
      comboBox.setSelectedIndex(itemCount - 1);
      skip = false;
   }

   List<JComponent> getToolBarComponents() {
      return List.of(previousButton, comboBox, nextButton);
   }

   /**
    * ComboBox entry representing a range.
    */
   private record RangeItem(float range) {
      @Override
      public String toString() {
         return Utils.toString(range) + " m";
      }
   }
}
