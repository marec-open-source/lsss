package no.imr.lsss.framework;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.depth.ChannelBottomDepthTransform;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.data.util.geometry.depth.PelagicDepthTransform;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.config.survey.GridConf;
import no.imr.lsss.modules.echogram.BottomBoundaryDepthTransform;

public abstract sealed class InterpretationZSettings extends EchogramZSettings {
   public static final double DEFAULT_MAX_DEPTH_FACTOR = 1.05;

   final InterpretationSettings interpretationSettings;

   private InterpretationZSettings(InterpretationSettings interpretationSettings) {
      this.interpretationSettings = interpretationSettings;
      getZoomedChangeManager().addListener(interpretationSettings.getNavigationHistory()::addCheckPoint);
   }

   public abstract boolean isPelagic();

   public abstract DepthTransform getDepthTransform(int channel);

   public static final class Pelagic extends InterpretationZSettings {
      private final DepthTransform depthTransform;

      Pelagic(InterpretationSettings interpretationSettings, DataManager dataManager) {
         super(interpretationSettings);

         depthTransform = new PelagicDepthTransform(dataManager.getDataConfiguration());

         minZ.setDescription("Minimum depth");
         maxZ.setDescription("Maximum depth");
      }

      @Override
      public boolean isPelagic() {
         return true;
      }

      @Override
      public DepthTransform getDepthTransform() {
         return depthTransform;
      }

      @Override
      public DepthTransform getDepthTransform(int channel) {
         return depthTransform;
      }

      void reset() {
         resetZLimits();
      }

      private void resetZLimits() {
         setMaxZ(0, getDefaultMaxZ());
         zoomOut();
      }

      private float getDefaultMaxZ() {
         float maxZ;
         DataConfiguration dataConfiguration = interpretationSettings.getDataFileSet().getDataConfiguration();
         if (dataConfiguration.isSeabedMounted()) {
            maxZ = dataConfiguration.getSeabedMountedSeabedPhysicalDepth();
         } else {
            maxZ = interpretationSettings.getDataFileSet().getMaxDepth();
         }
         return (float) Math.ceil(DEFAULT_MAX_DEPTH_FACTOR * maxZ);
      }
   }

   public static final class Bottom extends InterpretationZSettings {
      private final DataManager dataManager;
      private DepthTransform[] depthTransformsPerChannel = new DepthTransform[0];

      Bottom(InterpretationSettings interpretationSettings, DataManager dataManager) {
         super(interpretationSettings);

         this.dataManager = dataManager;

         minZ.setDescription("Minimum distance from the bottom (in the downwards direction)");
         maxZ.setDescription("Maximum distance from the bottom (in the downwards direction)");
      }

      @Override
      public boolean isPelagic() {
         return false;
      }

      @Override
      public DepthTransform getDepthTransform() {
         int channelIndex = interpretationSettings.getChannel() - 1;
         if (channelIndex < 0 || channelIndex >= depthTransformsPerChannel.length) {
            return IdentityDepthTransform.INSTANCE;
         }
         return depthTransformsPerChannel[channelIndex];
      }

      @Override
      public DepthTransform getDepthTransform(int channel) {
         return depthTransformsPerChannel[channel - 1];
      }

      public void setDepthTransform(DepthTransform depthTransform, int channel) {
         int channelIndex = channel - 1;
         depthTransformsPerChannel[channelIndex] = depthTransform;
         getZoomedChangeManager().notifyListeners();
      }

      public void setDepthTransformForCurrentChannel(DepthTransform depthTransform) {
         setDepthTransform(depthTransform, interpretationSettings.getChannel());
      }

      public void freezeBottomBoundaryDepthTransform() {
         DepthTransform depthTransform = getDepthTransform();
         if (depthTransform instanceof BottomBoundaryDepthTransform bottomBoundaryDepthTransform) {
            bottomBoundaryDepthTransform.freeze(interpretationSettings.getPingRange());
         }
      }

      public void resetBottomBoundaryDepthTransform() {
         DepthTransform depthTransform = getDepthTransform();
         if (depthTransform instanceof BottomBoundaryDepthTransform bottomBoundaryDepthTransform) {
            bottomBoundaryDepthTransform.freeze(PingRange.EMPTY_RANGE);
            getZoomedChangeManager().notifyListeners();
         }
      }

      void reset(GridConf gridConf) {
         resetDepthTransforms();
         resetZLimits(gridConf);
      }

      private void resetDepthTransforms() {
         int channelCount = dataManager.getDataFileSet().getTransducerCount();
         DepthTransform[] newDepthTransformsPerChannel = new DepthTransform[channelCount];
         for (int channel = 1; channel <= channelCount; channel++) {
            newDepthTransformsPerChannel[channel - 1] = new ChannelBottomDepthTransform(dataManager, channel);
         }
         depthTransformsPerChannel = newDepthTransformsPerChannel;
      }

      private void resetZLimits(GridConf gridConf) {
         setMaxZ(-gridConf.verticalExtentBottom.getFloatValue(), 5);
         zoomOut();
      }
   }
}
