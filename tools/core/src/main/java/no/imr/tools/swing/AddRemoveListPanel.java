package no.imr.tools.swing;

import no.imr.tools.listening.ChangeManager;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ContainerListener;
import java.util.List;

/**
 * Panel consisting of two JList with add and remove buttons in between.
 * Allows the user to add elements from the right list to the left,
 * and to remove them from the left list.
 */
public class AddRemoveListPanel<T extends Comparable<? super T>> {
   protected final List<T> allOptionsList;
   protected final JList<T> allOptionsJList;

   protected final List<T> selectedOptionsList;
   protected final JList<T> selectedOptionsJList;

   private final JPanel panel = new JPanel(new GridBagLayout());

   private final JLabel allLabel = new JLabel();
   private final JLabel selectedLabel = new JLabel();

   protected final JButton addButton = MiscIcons.ARROW_LEFT.on(new JButton("Add"));
   private final JButton deleteButton = MiscIcons.DELETE.on(new JButton("Remove"));

   private final ChangeManager changeManager = new ChangeManager();

   /**
    * Constructor allowing for using a custom cell renderer in the JList.
    *
    * @param allOptionsList      the right-hand list to select from
    * @param selectedOptionsList the left-hand list, where selected items are shown
    * @param text                text to display in the panel
    */
   public AddRemoveListPanel(List<T> allOptionsList, List<T> selectedOptionsList, String text) {
      this(allOptionsList, selectedOptionsList, text, new JList<>(), new JList<>());
   }

   /**
    * Constructor for creating two JLists with buttons in between.
    *
    * @param allOptionsList       the right-hand list to select items from
    * @param selectedOptionsList  the left-hand list, where the selected items are shown
    * @param text                 text to display in the panel
    * @param allOptionsJList      all options
    * @param selectedOptionsJList selected options
    */
   public AddRemoveListPanel(List<T> allOptionsList, List<T> selectedOptionsList, String text, JList<T> allOptionsJList, JList<T> selectedOptionsJList) {

      this.allOptionsList = allOptionsList;
      this.selectedOptionsList = selectedOptionsList;

      this.allOptionsJList = allOptionsJList;
      this.selectedOptionsJList = selectedOptionsJList;

      allOptionsJList.setModel(new ListListModel<>(allOptionsList));
      selectedOptionsJList.setModel(new ListListModel<>(selectedOptionsList));

      GridBagConstraints gc = new GridBagConstraints();
      gc.insets = new Insets(3, 3, 3, 3);

      JScrollPane selector = new JScrollPane(selectedOptionsJList);
      gc.gridx = 0;
      gc.fill = GridBagConstraints.BOTH;
      gc.weighty = 1.0;
      gc.weightx = 1.0;
      JPanel selectorPanel = new JPanel(new BorderLayout());
      selectorPanel.setPreferredSize(new Dimension(10, 10));
      selectorPanel.add(selector);
      selectorPanel.add(selectedLabel, BorderLayout.NORTH);
      panel.add(selectorPanel, gc);

      gc.gridx = 1;
      gc.fill = GridBagConstraints.NONE;
      gc.weighty = 0.0;
      gc.weightx = 0.0;
      JPanel buttonsPanel = new JPanel(new BorderLayout());
      buttonsPanel.add(createAddAndRemoveButtons());
      buttonsPanel.add(new JLabel(" "), BorderLayout.NORTH);
      panel.add(buttonsPanel, gc);

      JScrollPane all = new JScrollPane(allOptionsJList);
      gc.gridx = 2;
      gc.fill = GridBagConstraints.BOTH;
      gc.weighty = 1.0;
      gc.weightx = 1.0;
      JPanel allPanel = new JPanel(new BorderLayout());
      allPanel.setPreferredSize(new Dimension(10, 10));
      allPanel.add(all);
      allPanel.add(allLabel, BorderLayout.NORTH);
      panel.add(allPanel, gc);

      panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(text),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
   }

   public JPanel getPanel() {
      return panel;
   }

   public void addSelectedListContainerListener(ContainerListener containerListener) {
      selectedOptionsJList.addContainerListener(containerListener);
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   private Box createAddAndRemoveButtons() {
      initAddButton();
      initDeleteButton();

      Box buttonBox = Box.createVerticalBox();
      buttonBox.add(Box.createVerticalGlue());
      JPanel buttonPanel = new JPanel(new GridBagLayout());
      GridBagConstraints gc = new GridBagConstraints();
      gc.gridx = 1;
      gc.gridy = 1;
      gc.fill = GridBagConstraints.HORIZONTAL;
      gc.insets = new Insets(5, 5, 5, 5);
      buttonPanel.add(addButton, gc);
      gc.gridx = 1;
      gc.gridy = 2;
      gc.fill = GridBagConstraints.HORIZONTAL;
      gc.insets = new Insets(5, 5, 5, 5);
      buttonPanel.add(deleteButton, gc);
      buttonBox.add(buttonPanel);
      buttonBox.add(Box.createVerticalGlue());
      buttonBox.setBorder(GuiUtils.DEFAULT_MARGIN);
      return buttonBox;
   }

   private void initDeleteButton() {
      deleteButton.addActionListener(_ -> {
         selectedOptionsList.removeAll(selectedOptionsJList.getSelectedValuesList());
         selectedOptionsJList.setModel(new ListListModel<>(selectedOptionsList));
         changeManager.notifyListeners();
      });
   }

   protected void initAddButton() {
      addButton.addActionListener(_ -> {
         for (T iter : allOptionsJList.getSelectedValuesList()) {
            if (!selectedOptionsList.contains(iter)) {
               selectedOptionsList.add(iter);
            }
         }
         selectedOptionsJList.setModel(new ListListModel<>(selectedOptionsList));
         changeManager.notifyListeners();
      });
   }

   public List<T> getAllOptionsList() {
      return allOptionsList;
   }

   public JList<T> getAllOptionsJList() {
      return allOptionsJList;
   }

   public List<T> getSelectedOptionsList() {
      return selectedOptionsList;
   }

   public JList<T> getSelectedOptionsJList() {
      return selectedOptionsJList;
   }

   public void setAllLabel(String t) {
      allLabel.setText(t);
   }

   public void setSelectedLabel(String t) {
      selectedLabel.setText(t);
   }

   public JButton getAddButton() {
      return addButton;
   }

   public JButton getDeleteButton() {
      return deleteButton;
   }
}
