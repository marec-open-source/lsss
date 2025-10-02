package no.imr.tools.swing.wizardry;

import no.imr.tools.LateInit;
import no.imr.tools.help.HelpID;

import javax.swing.JComponent;

/**
 * One step in a wizard sequence.
 *
 * @see Wizard
 */
public abstract class WizardStep {
   private final String title;
   private final HelpID helpID;
   private final LateInit<Wizard> wizard = new LateInit<>();
   private boolean indented;

   protected WizardStep(String title, HelpID helpID) {
      this.title = title;
      this.helpID = helpID;
   }

   public String getTitle() {
      return title;
   }

   public HelpID getHelpID() {
      return helpID;
   }

   public abstract JComponent getComponent();

   /**
    * Test if the user can proceed to the next step.
    *
    * @return {@code true} if the "Next" button should be enabled
    */
   public boolean canProceed() {
      return true;
   }

   /**
    * Called when the user clicks the "Next" button.
    *
    * @return {@code true} if the wizard should proceed to the next step
    */
   public boolean onNext() {
      return true;
   }

   /**
    * Called when the wizard has successfully finished all steps.
    */
   public void apply() {
   }

   public Wizard getWizard() {
      return wizard.get();
   }

   void setWizard(Wizard wizard) {
      this.wizard.init(wizard);
   }

   public boolean isIndented() {
      return indented;
   }

   public void setIndented(boolean indented) {
      this.indented = indented;
   }

   @Override
   public String toString() {
      return getTitle();
   }
}
