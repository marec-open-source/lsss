package no.imr.lsss.framework.config;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.application.ApplicationConfiguration;
import no.imr.lsss.framework.config.survey.SurveyConfiguration;
import no.imr.lsss.resources.LsssResource;
import no.imr.tools.parameter.Name;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * All LSSS configuration, both application configuration and survey configuration.
 */
public final class LsssConfiguration extends ConfigurationUnit {
   private final ApplicationConfiguration applicationConfiguration;
   private final SurveyConfiguration surveyConfiguration;

   LsssConfiguration(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("LSSSConfiguration", "LSSS configuration"),
            "Application level settings and survey level settings");

      applicationConfiguration = addSubConfigurationUnit(new ApplicationConfiguration(plugin));
      surveyConfiguration = addSubConfigurationUnit(new SurveyConfiguration(plugin));
   }

   public ApplicationConfiguration getApplicationConfiguration() {
      return applicationConfiguration;
   }

   public SurveyConfiguration getSurveyConfiguration() {
      return surveyConfiguration;
   }

   @Override
   public JComponent getComponent() {
      JPanel panel = new JPanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.add(new JLabel("<html><h1>Configuration</h1>", new ImageIcon(LsssResource.LSSS_64), JLabel.CENTER));
      return panel;
   }
}
