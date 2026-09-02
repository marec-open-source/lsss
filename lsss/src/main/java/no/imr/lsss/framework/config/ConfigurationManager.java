package no.imr.lsss.framework.config;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.util.LanguageUtils;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.application.AppMiscConf;
import no.imr.lsss.framework.config.application.ApplicationConfiguration;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.lsss.framework.config.survey.SurveyConfiguration;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.config.survey.misc.SurveyMiscConf;
import no.imr.lsss.framework.config.survey.survey.SurveyConf;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.LateInit;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.dom4j.Element;

import javax.swing.JDialog;
import java.util.prefs.Preferences;

/**
 * The main class for handling configuration.
 * This includes configuring the database connection and survey registration.
 */
public final class ConfigurationManager {
   private final LSSS lsss;
   private final LanguageUtils languageUtils = new LanguageUtils(this);
   private final ListenableProperty<UserProfile> userProfile = new ListenableProperty<>(UserProfile.NORMAL_USE);
   private final LsssConfiguration lsssConfiguration;
   private final LateInit<Element> blankSurveyXml = new LateInit<>();
   private final ViewHolder<ConfigurationManagerView> viewHolder = new ViewHolder<>(() -> new ConfigurationManagerView(this));
   private final ChangeManager applyChangeManager = new ChangeManager();

   public ConfigurationManager(LSSS lsss) {
      this.lsss = lsss;
      BaseSystemFeaturePlugin plugin = lsss.getPluginManager().getFeaturePlugin(BaseSystemFeaturePlugin.class);
      lsssConfiguration = new LsssConfiguration(plugin);
      for (FeaturePlugin featurePlugin : lsss.getPluginManager().getFeaturePlugins()) {
         featurePlugin.addConfiguration(lsssConfiguration);
      }
      getDataConf().afterPluginsAddedConfiguration();
   }

   public void setup() {
      lsssConfiguration.setup();
   }

   public void defineBlankSurveyXml() {
      blankSurveyXml.init(getSurveyConfiguration().toXml());
   }

   public void installBlankSurveyXml() {
      getSurveyConfiguration().fromXml(blankSurveyXml.get());
      apply();
   }

   public LanguageUtils getLanguageUtils() {
      return languageUtils;
   }

   public ObservableValue<UserProfile> userProfile() {
      return userProfile;
   }

   public UserProfile getUserProfile() {
      return userProfile.getValue();
   }

   public void setUserProfile(UserProfile userProfile) {
      if (this.userProfile.getValue() == userProfile) {
         return;
      }
      this.userProfile.setValue(userProfile);
      Log.global.info("New access level: " + userProfile);
      viewHolder.ifView(ConfigurationManagerView::updateUserProfile);
   }

   /**
    * Test for sufficient user profile privilege.
    *
    * @param minimumUserProfile the minimum profile required
    * @return {@code true} if the actual user profile is sufficient
    */
   public boolean canEdit(UserProfile minimumUserProfile) {
      return getUserProfile().ordinal() >= minimumUserProfile.ordinal();
   }

   public LsssConfiguration getLsssConfiguration() {
      return lsssConfiguration;
   }

   public ApplicationConfiguration getApplicationConfiguration() {
      return lsssConfiguration.getApplicationConfiguration();
   }

   public SurveyConfiguration getSurveyConfiguration() {
      return lsssConfiguration.getSurveyConfiguration();
   }

   public SurveyConf getSurveyConf() {
      return getSurveyConfiguration().getSurveyConf();
   }

   public DataConfLSSS getDataConf() {
      return getSurveyConfiguration().getDataConf();
   }

   public GridConf getGridConf() {
      return getSurveyConfiguration().getGridConf();
   }

   public AppMiscConf getAppMiscConf() {
      return getApplicationConfiguration().getAppMiscConf();
   }

   public SurveyMiscConf getSurveyMiscConf() {
      return getSurveyConfiguration().getSurveyMiscConf();
   }

   public boolean isShowing() {
      return viewHolder.hasView() && viewHolder.getView().getDialog().isShowing();
   }

   public JDialog getDialog() {
      return viewHolder.getView().getDialog();
   }

   public void showDialog(BaseLsssModule module) {
      getSurveyConfiguration().getModuleConf().moduleToConfigurationUnit(module).ifPresent(viewHolder.getView()::showDialog);
   }

   public void showDialog(ConfigurationUnit configurationUnit) {
      viewHolder.getView().showDialog(configurationUnit);
   }

   public void showDialog() {
      viewHolder.getView().showDialog();
   }

   public Preferences getPreferences() {
      return lsss.getPreferences("config");
   }

   public void updateConfigurationPanel() {
      viewHolder.ifView(ConfigurationManagerView::updateConfigurationPanel);
   }

   public ChangeManager getApplyChangeManager() {
      return applyChangeManager;
   }

   private boolean apply() {
      boolean ok = lsssConfiguration.getAllUnitsRecursively().allMatch(ConfigurationUnit::prepareApply) &&
            lsssConfiguration.getAllUnitsRecursively().allMatch(ConfigurationUnit::apply);
      if (ok) {
         applyChangeManager.notifyListeners();
      }
      return ok;
   }

   public void ok() {
      if (apply()) {
         viewHolder.ifView(ConfigurationManagerView::closeDialog);
      }
   }
}
