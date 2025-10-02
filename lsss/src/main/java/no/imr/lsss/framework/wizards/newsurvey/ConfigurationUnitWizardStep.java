package no.imr.lsss.framework.wizards.newsurvey;

import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.JComponent;

/**
 * WizardStep using a ConfigurationUnit.
 */
class ConfigurationUnitWizardStep extends WizardStep {
   private final ConfigurationUnit configurationUnit;

   ConfigurationUnitWizardStep(ConfigurationUnit configurationUnit) {
      super(configurationUnit.getDisplayName(), configurationUnit.getHelpID());

      this.configurationUnit = configurationUnit;
   }

   @Override
   public JComponent getComponent() {
      return configurationUnit.getComponent();
   }

   @Override
   public void apply() {
      configurationUnit.apply();
   }
}
