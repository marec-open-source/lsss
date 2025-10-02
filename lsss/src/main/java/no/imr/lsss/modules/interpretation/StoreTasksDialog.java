package no.imr.lsss.modules.interpretation;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

final class StoreTasksDialog {
   private final List<StoreTask> tasks;
   private final boolean store;
   private final JDialog dialog;
   private final JButton okButton;
   private boolean hasSomethingToShow;

   StoreTasksDialog(InterpretationModule interpretationModule, boolean store, Runnable onChange) {
      tasks = interpretationModule.getStoreTasks();
      this.store = store;

      String text = store ? "Store" : "Delete";
      okButton = new JButton(text);

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(createTasksPanel(onChange));
      panel.add(createButtonsPanel(), BorderLayout.SOUTH);

      dialog = new JDialog(GuiUtils.windowForComponent(interpretationModule.getComponent()), text, Dialog.ModalityType.DOCUMENT_MODAL);
      if (store && interpretationModule.showQualityOptions.getBooleanValue() && !hasSomethingToShow) {
         apply();
         return;
      }
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.getContentPane().add(panel);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.pack();
      GuiUtils.moveToMouse(okButton);
      dialog.setVisible(true);
   }

   private JComponent createTasksPanel(Runnable onChange) {
      GridBag mainGridBag = new GridBag();
      mainGridBag.activateHorizontalFill();

      for (StoreTask task : tasks) {
         GridBag innerGridBag = new GridBag();
         innerGridBag.activateHorizontalFill();

         FeaturePlugin plugin = task.getPlugin();
         if (!(plugin instanceof BaseSystemFeaturePlugin)) {
            innerGridBag.addWithLineBreak(Box.createVerticalStrut(10));
            innerGridBag.addWithLineBreak(new JSeparator());
            innerGridBag.addWithLineBreak(Box.createVerticalStrut(5));
            JLabel label = plugin.getIconOrEmpty().on(new JLabel(plugin.getName().displayName()));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            innerGridBag.addWithLineBreak(label);
         }

         JCheckBox checkBox = new JCheckBox(task.getLongLabel(), task.isActive());
         checkBox.addItemListener(e -> {
            task.setActive(checkBox.isSelected());
            onChange.run();
         });
         innerGridBag.addWithLineBreak(checkBox);

         JComponent taskComponent = store ? task.getStoreComponent() : task.getDeleteComponent();
         if (taskComponent != null) {
            hasSomethingToShow = true;
            if (tasks.size() == 1) {
               return GuiUtils.createScrollPane(taskComponent);
            }
            innerGridBag.addWithLineBreak(taskComponent);
         }

         mainGridBag.addWithLineBreak(innerGridBag.getPanel());
      }
      return GuiUtils.createScrollPane(mainGridBag.getPanel());
   }

   private JComponent createButtonsPanel() {
      okButton.addActionListener(e -> {
         dialog.dispose();
         apply();
      });

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(e -> dialog.dispose());
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(okButton, BorderLayout.WEST);
      panel.add(cancelButton, BorderLayout.EAST);
      return panel;
   }

   private void apply() {
      for (StoreTask task : tasks) {
         if (task.isActive()) {
            if (store) {
               boolean ok = task.store();
               if (!ok) {
                  break;
               }
            } else {
               task.delete();
            }
         }
      }
   }
}
