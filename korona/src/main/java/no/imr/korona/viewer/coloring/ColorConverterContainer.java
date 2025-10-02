package no.imr.korona.viewer.coloring;

import no.imr.korona.color.Colormaps;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.incubator.KoronaIncubatorFeatureToggles;
import no.imr.korona.viewer.variables.BaseVariable;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.VariableCollection;
import no.imr.korona.viewer.variables.VariableFactory;
import no.imr.korona.viewer.variables.adcp.AdcpVariableFactory;
import no.imr.korona.viewer.variables.categorization.CategorizationVariableFactory;
import no.imr.korona.viewer.variables.plankton.PlanktonVariableFactory;
import no.imr.korona.viewer.variables.raw.RawVariableFactory;
import no.imr.korona.viewer.variables.raw.SvVariable;
import no.imr.tools.Utils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

/**
 * A Container which hold color converters.
 */
public final class ColorConverterContainer implements PingToColor {
   private ColorConverter colorConverter;

   private final List<DiscreteVariable> discreteVariables = new ArrayList<>();
   private final List<ContinuousVariable> continuousVariables = new ArrayList<>();

   private final ContinuousVariable sv;

   private final ChangeManager changeManager = new ChangeManager();
   private final Listener updateColorConverterListener = this::updateColorConverter;

   public ColorConverterContainer() {
      addVariableCollection(new RawVariableFactory());
      addVariableCollection(new CategorizationVariableFactory());
      addVariableCollection(new PlanktonVariableFactory());
      if (KoronaIncubatorFeatureToggles.ADCP_NETCDF) {
         addVariableCollection(new AdcpVariableFactory());
      }

      sv = getContinuousVariable(SvVariable.class);
      colorConverter = new SingleValueColorConverter(sv, Colormaps.COMBINED);
      updateColorConverter();
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public void addVariableCollection(VariableFactory variableFactory) {
      VariableCollection variableCollection = variableFactory.createVariableCollection();
      discreteVariables.addAll(variableCollection.discreteVariables());
      continuousVariables.addAll(variableCollection.continuousVariables());

      for (DiscreteVariable discreteVariable : variableCollection.discreteVariables()) {
         discreteVariable.getSettings().getChangeManager().addListener(updateColorConverterListener);
      }
      for (ContinuousVariable continuousVariable : variableCollection.continuousVariables()) {
         continuousVariable.getSettings().getChangeManager().addListener(updateColorConverterListener);
      }
   }

   public List<ContinuousVariable> getContinuousVariables() {
      return continuousVariables;
   }

   public List<DiscreteVariable> getDiscreteVariables() {
      return discreteVariables;
   }

   public <T extends DiscreteVariable> T getDiscreteVariable(Class<T> clazz) {
      return Utils.getFirstOrThrow(discreteVariables, clazz);
   }

   public <T extends ContinuousVariable> T getContinuousVariable(Class<T> clazz) {
      return Utils.getFirstOrThrow(continuousVariables, clazz);
   }

   public ContinuousVariable getSV() {
      return sv;
   }

   @Override
   public void convertToColor(Ping ping, int channel, int[] rgbs, FloatRange depthRange) {
      colorConverter.convertToColor(ping, channel, rgbs, depthRange);
   }

   public ColorConverter getColorConverter() {
      return colorConverter;
   }

   public void setColorConverter(ColorConverter colorConverter) {
      this.colorConverter = colorConverter;
      updateColorConverter();
   }

   private void updateColorConverter() {
      colorConverter.update();
      changeManager.notifyListeners();
   }

   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      updateEvaluationContext(configurationItems, configFileSettings, discreteVariables);
      updateEvaluationContext(configurationItems, configFileSettings, continuousVariables);
      updateColorConverter();
   }

   private static void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings, List<? extends BaseVariable> variables) {
      for (BaseVariable variable : variables) {
         variable.updateEvaluationContext(configurationItems, configFileSettings);
      }
   }
}
