package no.imr.lsss.framework.config.survey.preprocessing;

import no.imr.korona.config.ConfigFileSettingsContext;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.util.LsssUtils;
import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.swing.ViewHolder;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration of preprocessing.
 */
public final class PreprocessingConf extends ConfigurationUnit {
   private final DataConf dataConf;
   private final ConfigFileSettingsContext context;
   private final HelpID koronaHelpID;
   private final @Nullable OnTheFlySetup onTheFlySetup;
   private final List<PreprocessingSetup> preprocessingSetups = new ArrayList<>();
   private final PreprocessingSetup mainSetup;
   private final ViewHolder<PreprocessingConfView> viewHolder = new ViewHolder<>(() -> new PreprocessingConfView(this));

   public PreprocessingConf(FeaturePlugin plugin, Name name, DataConf dataConf, ConfigFileSettingsContext context, HelpID koronaHelpID) {
      super(plugin, name, LsssUtils.infoText("Configuration of", plugin, "preprocessing setups"));

      this.dataConf = dataConf;
      this.context = context;
      this.koronaHelpID = koronaHelpID;
      mainSetup = createPreprocessingSetup();
      preprocessingSetups.add(mainSetup);
      onTheFlySetup = dataConf.useConfigurableOnTheFlyProcessing() ? new OnTheFlySetup(this) : null;
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return UserProfile.SURVEY_SETUP;
   }

   public DataConf getDataConf() {
      return dataConf;
   }

   public ConfigFileSettingsContext getContext() {
      return context;
   }

   HelpID getKoronaHelpID() {
      return koronaHelpID;
   }

   public @Nullable OnTheFlySetup getOnTheFlySetup() {
      return onTheFlySetup;
   }

   @Override
   public void prepareForSaveDefault() {
      normalizeSetup();
      if (onTheFlySetup != null) {
         onTheFlySetup.cfsFile.setFile(null);
      }
   }

   public void normalizeSetup() {
      preprocessingSetups.clear();
      preprocessingSetups.add(mainSetup);
      mainSetup.fromXml(createPreprocessingSetup().toXml());
   }

   public PreprocessingSetup getMainSetup() {
      // Same instance since listeners are added to parameters
      assert mainSetup == preprocessingSetups.getFirst();
      return mainSetup;
   }

   public List<PreprocessingSetup> getPreprocessingSetups() {
      return preprocessingSetups;
   }

   PreprocessingSetup createPreprocessingSetup() {
      return new PreprocessingSetup(this);
   }

   void deleteProcessingSetup(PreprocessingSetup preprocessingSetup) {
      if (preprocessingSetup == mainSetup) {
         // Delete second setup and copy its settings to mainSetup.
         mainSetup.fromXml(preprocessingSetups.get(1).toXml());
         preprocessingSetups.remove(1);
      } else {
         preprocessingSetups.remove(preprocessingSetup);
      }
      getConfigurationManager().updateConfigurationPanel();
   }

   void moveProcessingSetup(PreprocessingSetup preprocessingSetup, int step) {
      int i = preprocessingSetups.indexOf(preprocessingSetup);
      PreprocessingSetup other = preprocessingSetups.get(i + step);
      Element xml = preprocessingSetup.toXml();
      preprocessingSetup.fromXml(other.toXml());
      other.fromXml(xml);
      getConfigurationManager().updateConfigurationPanel();
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      if (onTheFlySetup != null) {
         configurationElement.add(onTheFlySetup.toXml());
      }
      for (PreprocessingSetup preprocessingSetup : preprocessingSetups) {
         configurationElement.add(preprocessingSetup.toXml());
      }
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      if (onTheFlySetup != null) {
         Element onTheFlyElement = configurationElement.element(onTheFlySetup.getName().persistentName());
         if (onTheFlyElement != null) {
            onTheFlySetup.fromXml(onTheFlyElement);
         }
      }
      normalizeSetup();
      boolean firstTime = true;
      for (Element parametersElement : configurationElement.elements(ParameterCollection.XML_PARAMETERS)) {
         PreprocessingSetup preprocessingSetup;
         if (firstTime) {
            firstTime = false;
            preprocessingSetup = mainSetup;
         } else {
            preprocessingSetup = createPreprocessingSetup();
            preprocessingSetups.add(preprocessingSetup);
         }
         preprocessingSetup.fromXml(parametersElement);
      }
   }

   @Override
   public JComponent getComponent() {
      viewHolder.getView().updateSetups();
      return viewHolder.getComponent();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }
}
