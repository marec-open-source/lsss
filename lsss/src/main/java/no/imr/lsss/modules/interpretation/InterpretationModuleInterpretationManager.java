package no.imr.lsss.modules.interpretation;

import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.region.InterpretationContainer;

import java.util.Collection;

final class InterpretationModuleInterpretationManager extends InterpretationManager {
   private final InterpretationModule interpretationModule;

   InterpretationModuleInterpretationManager(InterpretationModule interpretationModule) {
      this.interpretationModule = interpretationModule;
   }

   @Override
   public void interpretationChanged() {
      interpretationModule.getLSSS().getRegionManager().getInterpretationChangeManager().notifyListeners(interpretationModule);
   }

   @Override
   public Collection<? extends InterpretationContainer> getInterpretationContainers() {
      return interpretationModule.getSelectedRegions();
   }

   @Override
   public float frequencyResponseFunction(int channel) {
      double f = interpretationModule.getLSSS().getInterpretationSettings().getDataFileSet().getRawFileConfiguration().getTransducers().get(channel - 1).getFrequency();
      return (float) interpretationModule.frequencyResponseFunction.getFunction().applyAsDouble(f);
   }

   @Override
   public int getTransducerCount() {
      return interpretationModule.getLSSS().getInterpretationSettings().getDataFileSet().getTransducerCount();
   }

   @Override
   public int getChannel() {
      return interpretationModule.getLSSS().getInterpretationSettings().getChannel();
   }

   @Override
   public float getRegionSa(boolean bubbleCorrected) {
      return interpretationModule.getRegionSa(bubbleCorrected);
   }

   @Override
   public float getRegionSa(ConditionalPingMask excludeMask, int channel, boolean bubbleCorrected) {
      return interpretationModule.getRegionSa(excludeMask, channel, bubbleCorrected);
   }
}
