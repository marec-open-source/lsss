package no.imr.tools.swing;

import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ListEditor<E> {
   private final JPanel panel = new JPanel(new BorderLayout());
   private final List<E> items;
   private final JList<E> list;
   private final JButton upButton = MiscIcons.ARROW_UP.on(new JButton("Up"));
   private final JButton downButton = MiscIcons.ARROW_DOWN.on(new JButton("Down"));
   private final JButton removeButton = MiscIcons.DELETE.on(new JButton("Remove"));

   public ListEditor(JList<E> list, List<E> items) {
      this.list = list;
      this.items = items;

      panel.add(new JScrollPane(list));

      JPanel buttonsPanel = new JPanel(new WrappingFlowLayout(FlowLayout.LEFT));
      panel.add(buttonsPanel, BorderLayout.SOUTH);

      buttonsPanel.add(removeButton);
      removeButton.setToolTipText("Remove selected items");
      removeButton.addActionListener(e -> removeHighlightedItems());

      buttonsPanel.add(upButton);
      upButton.setToolTipText("Move selected items up");
      upButton.addActionListener(e -> shiftUp());

      buttonsPanel.add(downButton);
      downButton.setToolTipText("Move selected items down");
      downButton.addActionListener(e -> shiftDown());
   }

   public JComponent getComponent() {
      return panel;
   }

   public List<E> getItems() {
      return items;
   }

   public List<E> getHighlightedItems() {
      return list.getSelectedValuesList();
   }

   public void shiftUp() {
      List<E> highlightedItems = getHighlightedItems();
      int iMin = -1;
      for (int selectedIndex : list.getSelectedIndices()) {
         iMin = Math.max(iMin + 1, selectedIndex - 1);
         Collections.swap(items, selectedIndex, iMin);
      }
      setHighlightedItems(highlightedItems);
   }

   public void shiftDown() {
      List<E> highlightedItems = getHighlightedItems();
      int iMax = items.size();
      int[] selectedIndices = list.getSelectedIndices();
      for (int i = selectedIndices.length - 1; i >= 0; i--) {
         int selectedIndex = selectedIndices[i];
         iMax = Math.min(iMax - 1, selectedIndex + 1);
         Collections.swap(items, selectedIndex, iMax);
      }
      setHighlightedItems(highlightedItems);
   }

   public void addItem(E item) {
      if (items.contains(item)) {
         return;
      }
      List<E> highlightedItems = getHighlightedItems();
      items.add(item);
      setHighlightedItems(highlightedItems);
   }

   public void removeHighlightedItems() {
      items.removeAll(getHighlightedItems());
      setHighlightedItems(List.of());
   }

   public void clear() {
      items.clear();
      setHighlightedItems(List.of());
   }

   public void setHighlightedItems(Collection<E> highlightedItems) {
      list.setModel(new ListListModel<>(items));
      list.getSelectionModel().clearSelection();
      for (int i = 0; i < items.size(); i++) {
         if (highlightedItems.contains(items.get(i))) {
            list.getSelectionModel().addSelectionInterval(i, i);
         }
      }
   }

   public void setEnabled(boolean enabled) {
      list.setEnabled(enabled);
      upButton.setEnabled(enabled);
      downButton.setEnabled(enabled);
      removeButton.setEnabled(enabled);
   }
}
