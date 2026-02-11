package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

final class OptionalWizardStep extends WizardStep {
   private final JPanel panel;

   OptionalWizardStep() {
      super("Optional steps", LsssHelp.NEW_SURVEY);

      GridBag gridBag = new GridBag()
            .configureVerticalBox();

      gridBag.add(new JLabel("""
            <html>
            <h1>Optional steps</h1>
            <p>The remaining steps are optional and the corresponding configuration may be changed later in the configuration dialog.</p>
            """));

      gridBag.addVerticalFiller();

      JButton finishButton = MiscIcons.FAST_FORWARD.on(new JButton("Finish with selected settings"));
      finishButton.addActionListener(_ -> getWizard().finish());

      JButton continueButton = MiscIcons.EDIT.on(new JButton("Continue with optional steps"));
      continueButton.addActionListener(_ -> getWizard().next());

      JPanel buttonPanel = new JPanel(new FlowLayout());
      buttonPanel.add(finishButton);
      buttonPanel.add(Box.createHorizontalStrut(30));
      buttonPanel.add(continueButton);
      gridBag.add(buttonPanel);

      gridBag.addVerticalFiller();

      panel = gridBag.getPanel();
      panel.setBorder(GuiUtils.DEFAULT_MARGIN);
   }

   @Override
   public JComponent getComponent() {
      return panel;
   }
}
