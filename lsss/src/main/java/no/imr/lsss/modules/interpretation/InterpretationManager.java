package no.imr.lsss.modules.interpretation;

import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.region.InterpretationContainer;

import java.util.Collection;

/**
 * Interface for classes managing interpretable objects.
 * It can return interpretations and the sa for the regions
 * the interpretation covers.
 */
public abstract class InterpretationManager {
   protected InterpretationManager() {
   }

   public void interpretationChanged() {
   }

   public abstract Collection<? extends InterpretationContainer> getInterpretationContainers();

   public float frequencyResponseFunction(int channel) {
      return 1;
   }

   public abstract int getTransducerCount();

   public abstract int getChannel();

   /**
    * {@return sa for the current region}
    *
    * @param bubbleCorrected bubble corrected or not
    */
   public abstract float getRegionSa(boolean bubbleCorrected);

   /**
    * {@return sa for the current masked region}
    *
    * @param excludeMask     a mask for excluding samples
    * @param channel         a channel
    * @param bubbleCorrected bubble corrected or not
    */
   public abstract float getRegionSa(ConditionalPingMask excludeMask, int channel, boolean bubbleCorrected);
}
