package no.imr.lsss.framework.wizards.appsetup;

import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.JComponent;
import javax.swing.JLabel;

final class IntroWizardStep extends WizardStep {
   IntroWizardStep() {
      super("LSSS setup", LsssHelp.LSSS_SETUP);
   }

   @Override
   public JComponent getComponent() {
      JLabel infoLabel = new JLabel("""
            <html>
            <h1>LSSS setup</h1>
            <p>Before LSSS can be used a few settings must be initialized.</p>
            <p>It is possible to change these settings at any time later.</p>
            """);

      return GuiUtils.createScrollPane(infoLabel);
   }
}
