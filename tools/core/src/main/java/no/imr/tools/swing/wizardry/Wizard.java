package no.imr.tools.swing.wizardry;

import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

/**
 * A wizard dialog with a number of steps the user must go through.
 *
 * @see WizardStep
 */
public final class Wizard {
   private final JDialog dialog;
   private final JLabel indexLabel = new JLabel();
   private final JPanel configurationPanel = new JPanel(new BorderLayout());
   private final JPanel bottomLeftPanel = new JPanel(new BorderLayout());
   private final JButton previousButton = MiscIcons.NAVIGATE_PREVIOUS.on(new JButton("Previous"));
   private final JButton nextButton = MiscIcons.NAVIGATE_NEXT.on(new JButton());

   private final List<WizardStep> wizardSteps;
   private int stepIndex;

   private boolean succeeded;

   public Wizard(@Nullable Window window, String title, List<WizardStep> wizardSteps) {
      this.wizardSteps = wizardSteps;
      for (WizardStep wizardStep : wizardSteps) {
         wizardStep.setWizard(this);
      }

      dialog = new JDialog(window, title, Dialog.ModalityType.DOCUMENT_MODAL);

      previousButton.addActionListener(_ -> setStepIndex(stepIndex - 1));

      nextButton.setHorizontalTextPosition(JButton.LEFT);
      nextButton.addActionListener(_ -> next());

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> cancel());
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));

      JButton helpButton = new JButton("Help");
      helpButton.addActionListener(_ -> getCurrentWizardStep().getHelpID().show());
      GuiUtils.setAccelerator(helpButton, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      buttonPanel.add(previousButton);
      buttonPanel.add(nextButton);
      buttonPanel.add(cancelButton);
      buttonPanel.add(helpButton);

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.add(bottomLeftPanel, BorderLayout.WEST);
      bottomPanel.add(buttonPanel, BorderLayout.EAST);

      indexLabel.setBackground(Color.WHITE);
      indexLabel.setVerticalAlignment(JLabel.TOP);

      JPanel indexPanel = new JPanel(new BorderLayout());
      indexPanel.setBackground(Color.WHITE);
      indexPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), BorderFactory.createEmptyBorder(10, 10, 10, 10)));
      indexPanel.add(indexLabel);

      configurationPanel.setBorder(BorderFactory.createEtchedBorder());

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(indexPanel, BorderLayout.WEST);
      mainPanel.add(bottomPanel, BorderLayout.SOUTH);
      mainPanel.add(configurationPanel);

      dialog.getContentPane().add(mainPanel);
      dialog.getRootPane().setDefaultButton(nextButton);
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            cancel();
         }
      });
   }

   public void setBottomLeft(JComponent component) {
      GuiUtils.replaceContent(bottomLeftPanel, component);
   }

   public List<WizardStep> getWizardSteps() {
      return wizardSteps;
   }

   public void show(int width, int height) {
      setStepIndex(0);
      dialog.setSize(width, height);
      dialog.setLocationRelativeTo(dialog.getParent());
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);
   }

   public JDialog getDialog() {
      return dialog;
   }

   public boolean succeeded() {
      return succeeded;
   }

   public void refreshCurrentComponent() {
      setStepIndex(stepIndex);
   }

   private void setStepIndex(int stepIndex) {
      this.stepIndex = stepIndex;

      updateIndexText();

      GuiUtils.replaceContent(configurationPanel, getCurrentWizardStep().getComponent());

      previousButton.setEnabled(stepIndex > 0);
      nextButton.setText(stepIndex == wizardSteps.size() - 1 ? "Finish" : "Next");
      updateNextButton();
   }

   private WizardStep getCurrentWizardStep() {
      return wizardSteps.get(stepIndex);
   }

   /**
    * Should be called when something has happened that may require the "Next" button to change enabled state.
    */
   public void updateNextButton() {
      nextButton.setEnabled(getCurrentWizardStep().canProceed());
   }

   private void updateIndexText() {
      HtmlStringBuilder sb = new HtmlStringBuilder()
            .html("<table cellpadding=0 cellspacing=0>");
      for (int i = 0; i < wizardSteps.size(); i++) {
         WizardStep wizardStep = wizardSteps.get(i);
         sb.html("<tr><td style='margin-right: 5px;'>").text(i == stepIndex ? "→" : "").html("</td>")
               .html(wizardStep.isIndented() ? "<td></td>" : "")
               .html("<td align=right>").text(i + 1).html(".</td>")
               .html("<td style='white-space: nowrap; padding-left: 5px'").html(wizardStep.isIndented() ? "" : " colspan=2").html(">")
               /* */ .text(wizardStep.getTitle()).html("</td>")
               .html("</tr>");
      }
      sb.html("</table>");
      indexLabel.setText(sb.toString());
   }

   public void next() {
      if (!getCurrentWizardStep().onNext()) {
         return;
      }
      if (stepIndex == wizardSteps.size() - 1) {
         finish();
      } else {
         setStepIndex(stepIndex + 1);
      }
   }

   public void finish() {
      wizardSteps.forEach(WizardStep::apply);
      done(true);
   }

   private void cancel() {
      done(false);
   }

   private void done(boolean succeeded) {
      this.succeeded = succeeded;
      dialog.dispose();
   }
}
