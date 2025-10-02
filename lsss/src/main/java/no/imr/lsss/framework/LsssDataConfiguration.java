package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.lsss.LSSS;

/**
 * The data configuration for LSSS.
 */
public final class LsssDataConfiguration extends DataConfiguration {
   private final LSSS lsss;

   public LsssDataConfiguration(LSSS lsss) {
      this.lsss = lsss;
   }

   @Override
   public boolean isDataLoadingCancelled() {
      return lsss.getInterpretationSettings().isCancelled();
   }

   @Override
   public boolean isSeabedMounted() {
      return lsss.getConfigurationManager().getSurveyMiscConf().seabedMounted.getBooleanValue();
   }

   @Override
   public float getSeabedMountedDistanceToSeabed() {
      return lsss.getConfigurationManager().getSurveyMiscConf().seabedMountedDistanceToSeabed.getFloatValue();
   }

   @Override
   public float getSeabedMountedDistanceToSurface() {
      return lsss.getConfigurationManager().getSurveyMiscConf().seabedMountedDistanceToSurface.getFloatValue();
   }

   @Override
   public float getPreferredFrequencyForBottom() {
      return lsss.getConfigurationManager().getSurveyMiscConf().mainFrequency.getFloatValue();
   }

   @Override
   public float getMinFrequencyForBottom() {
      return lsss.getConfigurationManager().getSurveyMiscConf().minFrequencyForBottom.getFloatValue();
   }

   @Override
   public float getMaxFrequencyForBottom() {
      return lsss.getConfigurationManager().getSurveyMiscConf().maxFrequencyForBottom.getFloatValue();
   }

   @Override
   public float getMinimumDepthThresholdFactor() {
      return lsss.getConfigurationManager().getSurveyMiscConf().minimumDepthThresholdFactor.getFloatValue();
   }
}
