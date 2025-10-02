package no.imr.lsss.framework.wizards.appsetup;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.Max;
import no.imr.tools.listening.Listener;
import no.imr.tools.swing.wizardry.Wizard;
import no.imr.tools.swing.wizardry.WizardStep;
import no.marec.lsss.api.util.observing.Subscription;
import org.dom4j.Element;

import java.util.List;

/**
 * Wizard for initial application configuration.
 */
public final class ApplicationSetupWizard {
   private final LSSS lsss;
   private final Wizard wizard;

   public ApplicationSetupWizard(LSSS lsss) {
      this.lsss = lsss;
      wizard = new Wizard(lsss.getFrame(), "LSSS setup", createWizardSteps());
   }

   public Wizard getWizard() {
      return wizard;
   }

   public void show() {
      Element backupConfiguration = lsss.getConfigurationManager().getLsssConfiguration().toXml();

      Listener listener = wizard::updateNextButton;
      List<Subscription> subscriptions = List.of(
            lsss.getDatabaseManager().getConnectionChangeManager().subscribe(listener)
      );

      UserProfile userProfile = lsss.getConfigurationManager().getUserProfile();
      lsss.getConfigurationManager().setUserProfile(Max.of(userProfile, UserProfile.ADMINISTRATOR_MODE));

      wizard.show(1000, 640);

      lsss.getConfigurationManager().setUserProfile(userProfile);

      subscriptions.forEach(Subscription::unsubscribe);

      if (wizard.succeeded()) {
         lsss.getConfigurationManager().getApplicationConfiguration().saveDefault();
      } else {
         lsss.getConfigurationManager().getLsssConfiguration().fromXml(backupConfiguration);
      }
   }

   private List<WizardStep> createWizardSteps() {
      return List.of(
            new IntroWizardStep(),
            new DirectoryWizardStep(lsss),
            new CategorizationLibraryWizardStep(lsss),
            new DatabaseWizardStep(lsss)
      );
   }
}
