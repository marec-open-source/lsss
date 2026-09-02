package no.imr.tools.parameter.gui;

import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.VerticalScrollablePanel;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A dialog for editing a collection of parameters.
 */
public final class ConfigurableGUIDialog {
   private final @Nullable Component referenceComponent;
   private final Configurable configurable;
   private final JDialog dialog;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JButton okButton = new JButton("OK");
   private final JButton cancelButton = new JButton("Cancel");
   private final List<JButton> extraButtons = new ArrayList<>();
   private final Element backupXml;
   private @Nullable JComponent gui;
   private boolean scrollable = true;
   private @Nullable HelpID helpID;
   private Dimension minimumSize = new Dimension();
   private Dimension maximumSize = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
   private Supplier<Boolean> closeOnOk = () -> true;
   private boolean ok;

   public ConfigurableGUIDialog(@Nullable Component referenceComponent, @Nullable String title, Configurable configurable) {
      this.referenceComponent = referenceComponent;
      this.configurable = configurable;
      dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.DOCUMENT_MODAL);
      backupXml = configurable.toXml();

      okButton.setToolTipText("Accept changes and close window");
      okButton.addActionListener(_ -> ok());

      cancelButton.setToolTipText("Revert changes and close window");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> cancel());

      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            cancel();
         }
      });
      dialog.getContentPane().add(mainPanel);
      dialog.getRootPane().setDefaultButton(okButton);
   }

   public ConfigurableGUIDialog setTopText(String text) {
      JLabel textLabel = new JLabel(text);
      textLabel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      mainPanel.add(textLabel, BorderLayout.NORTH);
      return this;
   }

   public ConfigurableGUIDialog setHelpID(HelpID helpID) {
      this.helpID = helpID;
      return this;
   }

   public ConfigurableGUIDialog setCloseOnOk(Supplier<Boolean> closeOnOk) {
      this.closeOnOk = closeOnOk;
      return this;
   }

   public ConfigurableGUIDialog setModal(boolean modal) {
      dialog.setModalityType(modal ? Dialog.ModalityType.DOCUMENT_MODAL : Dialog.ModalityType.MODELESS);
      return this;
   }

   public ConfigurableGUIDialog accessOKButton(Consumer<JButton> consumer) {
      consumer.accept(okButton);
      return this;
   }

   public ConfigurableGUIDialog accessCancelButton(Consumer<JButton> consumer) {
      consumer.accept(cancelButton);
      return this;
   }

   public ConfigurableGUIDialog extraButton(JButton button) {
      extraButtons.add(button);
      return this;
   }

   public ConfigurableGUIDialog accessDialog(Consumer<JDialog> consumer) {
      consumer.accept(dialog);
      return this;
   }

   public ConfigurableGUIDialog setGUI(JComponent component) {
      gui = component;
      return this;
   }

   public ConfigurableGUIDialog setScrollable(boolean scrollable) {
      this.scrollable = scrollable;
      return this;
   }

   public ConfigurableGUIDialog setMinimumSize(int width, int height) {
      return setMinimumSize(new Dimension(width, height));
   }

   public ConfigurableGUIDialog setMinimumSize(Dimension minimumSize) {
      this.minimumSize = minimumSize;
      return this;
   }

   public ConfigurableGUIDialog setMaximumSize(int width, int height) {
      return setMaximumSize(new Dimension(width, height));
   }

   public ConfigurableGUIDialog setMaximumSize(Dimension maximumSize) {
      this.maximumSize = maximumSize;
      return this;
   }

   public boolean show() {
      if (gui != null) {
         if (scrollable) {
            VerticalScrollablePanel panel = VerticalScrollablePanel.wrap(gui);
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            mainPanel.add(new JScrollPane(panel));
         } else {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(gui);
            panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            mainPanel.add(panel);
         }
      }
      mainPanel.add(createButtonsPanel(), BorderLayout.SOUTH);

      dialog.pack();
      GuiUtils.expandSizeTo(dialog, minimumSize);
      GuiUtils.shrinkSizeTo(dialog, maximumSize);
      dialog.setLocationRelativeTo(referenceComponent);
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);
      return ok;
   }

   private JComponent createButtonsPanel() {
      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(okButton);
      panel.add(cancelButton);
      if (helpID != null) {
         JButton helpButton = new JButton("Help");
         helpID.enableHelpKeyOnButton(helpButton);
         panel.add(helpButton);
      }

      for (JButton button : extraButtons.reversed()) {
         panel.add(button, 0);
      }

      return panel;
   }

   public void ok() {
      if (!CurrentInputComponent.commitEdit() || !closeOnOk.get()) {
         return;
      }
      ok = true;
      dialog.dispose();
   }

   private void cancel() {
      configurable.fromXml(backupXml);

      ok = false;
      dialog.dispose();
   }
}
