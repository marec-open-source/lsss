package no.imr.lsss.modules.echogram;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ListenableProperty;

final class ConditionalMaskingManager {
   private final LSSS lsss;
   private final Multimap<DiscreteVariable, String> maskedVariables = HashMultimap.create();
   final ListenableProperty<Boolean> invert = new ListenableProperty<>(false);

   ConditionalMaskingManager(LSSS lsss) {
      this.lsss = lsss;
      invert.subscribe(__ -> update());
   }

   Multimap<DiscreteVariable, String> getMaskedVariables() {
      return maskedVariables;
   }

   void update() {
      lsss.getRegionManager().setConditionalPingMask(toConditionalPingMask());
   }

   private ConditionalPingMask toConditionalPingMask() {
      if (!hasContent()) {
         return ConditionalPingMask.EMPTY;
      }
      return CategoryConditionalPingMask.make(lsss, maskedVariables, invert.getValue());
   }

   boolean hasContent() {
      for (DiscreteVariable discreteVariable : lsss.getInterpretationSettings().getColorConverterContainer().getDiscreteVariables()) {
         if (!discreteVariable.getSettings().getDiscreteColorMapping().getDiscreteColors().isEmpty()) {
            return true;
         }
      }
      return false;
   }
}
