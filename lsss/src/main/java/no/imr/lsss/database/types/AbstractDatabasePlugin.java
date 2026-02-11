package no.imr.lsss.database.types;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.PasswordParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.GuiUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;

/**
 * Implements {@link DatabasePlugin#getConfigurationGUI()} using parameter objects.
 */
public abstract class AbstractDatabasePlugin extends DatabasePlugin {
   private final LSSS lsss;
   private @Nullable ParameterEditor parameterEditor;
   private boolean enabled = true;
   private boolean askingForPassword;
   private boolean passwordInitialized;

   final PasswordParameter password = new PasswordParameter(new Name("Password")) {
      @Override
      public boolean isPersistable() {
         return savePassword.getBooleanValue() || getValue().isEmpty();
      }

      @Override
      public void fromXml(Element element) {
         super.fromXml(element);
         passwordInitialized = true;
      }
   };

   final BooleanParameter savePassword = new BooleanParameter(
         new Name("SavePassword", "Save password"),
         false,
         "Save encrypted in configuration file");

   AbstractDatabasePlugin(Name name, LSSS lsss) {
      super(name);

      this.lsss = lsss;

      password.subscribe(_ -> passwordInitialized = true);
   }

   public abstract String getDescription();

   @Override
   public JComponent getConfigurationGUI() {
      JPanel panel = new JPanel(new BorderLayout());

      JLabel descriptionLabel = new JLabel("<html><body style='margin-bottom: 1cm;'>" + getDescription());
      panel.add(descriptionLabel, BorderLayout.NORTH);

      panel.add(getParameterEditor().getEditorComponent());

      return panel;
   }

   @Override
   public void setGUIEnabled(boolean enabled) {
      this.enabled = enabled;
      password.notifyListeners();
   }

   private ParameterEditor getParameterEditor() {
      ParameterEditor parameterEditor = this.parameterEditor;
      if (parameterEditor == null) {
         parameterEditor = new ParameterEditor(getParameters());
         parameterEditor.getGUIConfig().setHorizontalFill(true);
         parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
         parameterEditor.getGUIConfig().setParameterEnabledDecider(parameter -> {
            if (askingForPassword && (parameter == password || parameter == savePassword)) {
               return true;
            }
            if (lsss.getConfigurationManager().canEdit(UserProfile.ADMINISTRATOR_MODE) && parameter == savePassword) {
               return true;
            }
            return enabled;
         });
         this.parameterEditor = parameterEditor;
      }
      return parameterEditor;
   }

   public void setPasswordInitialized(boolean passwordInitialized) {
      this.passwordInitialized = passwordInitialized;
   }

   @Override
   public void askForPasswordIfNecessary() {
      GuiUtils.invokeNowOrWait(this::askForPasswordIfNecessaryInternal);
   }

   private void askForPasswordIfNecessaryInternal() {
      if (passwordInitialized || getParameterEditor().getEditorComponent().isShowing()) {
         return;
      }

      askingForPassword = true;
      password.notifyListeners();

      JDialog dialog = new JDialog(lsss.getFrame(),
            "Password for " + getName().displayName(), Dialog.ModalityType.DOCUMENT_MODAL);

      JPanel editorPanel = new JPanel(new BorderLayout());
      editorPanel.setBorder(GuiUtils.DEFAULT_MARGIN);
      editorPanel.add(getParameterEditor().getEditorComponent());

      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> dialog.dispose());

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.setBorder(BorderFactory.createEtchedBorder());
      buttonPanel.add(okButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.setBorder(BorderFactory.createEtchedBorder());
      mainPanel.add(new JScrollPane(editorPanel));
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      SwingUtilities.invokeLater(getParameterEditor().getInputComponent(password)::requestFocusInWindow);

      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.getContentPane().add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(lsss.getFrame());
      dialog.setVisible(true);

      askingForPassword = false;
   }

   @Override
   public Element toXml() {
      return new ParameterCollection(this).toXml();
   }

   @Override
   public void fromXml(Element element) {
      new ParameterCollection(this).fromXml(element);
   }
}
