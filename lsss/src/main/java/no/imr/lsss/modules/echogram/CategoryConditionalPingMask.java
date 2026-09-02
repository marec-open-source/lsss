package no.imr.lsss.modules.echogram;

import com.google.common.collect.Multimap;
import no.imr.korona.color.DiscreteColor;
import no.imr.korona.color.DiscreteColorMapping;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.korona.region.Mask;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.DiscreteVariableResult;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionLSSS;
import no.imr.lsss.modules.korona.region.KoronaRegionModule;
import no.imr.lsss.modules.korona.tracking.TrackId;
import no.imr.lsss.modules.korona.tracking.TrackInfo;
import no.imr.lsss.modules.korona.tracking.TrackInfoModule;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CategoryConditionalPingMask implements ConditionalPingMask {
   private final KoronaRegionModule koronaRegionModule;
   private final TrackInfoModule trackInfoModule;
   private final BooleanParameter useSchoolCategorization;
   private final BooleanParameter useTrackCategorization;
   private final Map<DiscreteVariable, boolean[]> discreteVariableToMask;
   private final boolean invert;
   private final CategoryVariable categoryVariable;
   private final boolean[] categoryVariableMask;

   private CategoryConditionalPingMask(LSSS lsss, Map<DiscreteVariable, boolean[]> discreteVariableToMask, boolean invert) {
      koronaRegionModule = lsss.getModuleManager().getModule(KoronaRegionModule.class);
      trackInfoModule = lsss.getModuleManager().getModule(TrackInfoModule.class);
      useSchoolCategorization = lsss.getConfigurationManager().getSurveyMiscConf().useSchoolCategorization;
      useTrackCategorization = lsss.getConfigurationManager().getSurveyMiscConf().useTrackCategorization;
      this.discreteVariableToMask = discreteVariableToMask;
      this.invert = invert;
      categoryVariable = lsss.getInterpretationSettings().getColorConverterContainer().getDiscreteVariable(CategoryVariable.class);
      categoryVariableMask = discreteVariableToMask.get(categoryVariable);
   }

   public static ConditionalPingMask make(LSSS lsss, Multimap<DiscreteVariable, String> maskedVariables, boolean invert) {
      if (maskedVariables.isEmpty() && !invert) {
         return EMPTY;
      }
      Map<DiscreteVariable, boolean[]> newMasking = new HashMap<>();
      for (DiscreteVariable discreteVariable : lsss.getInterpretationSettings().getColorConverterContainer().getDiscreteVariables()) {
         boolean[] array = new boolean[Byte.MAX_VALUE + 1];
         newMasking.put(discreteVariable, array);
         DiscreteColorMapping mapping = discreteVariable.getSettings().getDiscreteColorMapping();
         for (DiscreteColor discreteColor : mapping.getDiscreteColors()) {
            String name = discreteColor.getName();
            int number = discreteColor.getValue();
            array[number] = maskedVariables.containsEntry(discreteVariable, name);
         }
      }
      return new CategoryConditionalPingMask(lsss, newMasking, invert);
   }

   @Override
   public FloatRangeSet getMask(Ping ping) {
      List<FloatRange> doneRanges = new ArrayList<>();
      List<FloatRange> maskRanges = new ArrayList<>();

      if (useSchoolCategorization.getBooleanValue()) {
         evaluateForSchools(ping.getPingIndex(), doneRanges, maskRanges);
      }

      if (useTrackCategorization.getBooleanValue()) {
         evaluateForTracks(ping, doneRanges, maskRanges);
      }

      for (Map.Entry<DiscreteVariable, boolean[]> entry : discreteVariableToMask.entrySet()) {
         evaluateForSamples(entry.getKey().evaluate(ping), entry.getValue(), doneRanges, maskRanges);
      }

      if (invert) {
         return FloatRangeSet.of(Mask.ENTIRE_PING_DEPTH_RANGE).subtract(FloatRangeSet.of(maskRanges));
      } else {
         return FloatRangeSet.of(maskRanges);
      }
   }

   private void evaluateForSchools(PingIndex pingIndex, List<FloatRange> doneRanges, List<FloatRange> maskRanges) {
      for (KoronaRegionLSSS region : koronaRegionModule.getKoronaRegions()) {
         // Test first for most probable cause to continue loop
         FloatRangeSet floatRangeSet = region.getMask().get(pingIndex);
         if (floatRangeSet == null) {
            continue;
         }
         Cas0Datagram cas0Datagram = region.getCas0Datagram();
         if (cas0Datagram == null) {
            continue;
         }
         int category = cas0Datagram.getBestCategory(categoryVariable, (byte) -1);
         if (category >= 0 && categoryVariableMask[category]) {
            maskRanges.addAll(floatRangeSet.getFloatRanges());
         }
         doneRanges.addAll(floatRangeSet.getFloatRanges());
      }
   }

   private void evaluateForTracks(Ping ping, List<FloatRange> doneRanges, List<FloatRange> maskRanges) {
      Map<TrackId, TrackInfo> trackInfos = trackInfoModule.getTrackInfos();
      trackInfoModule.getTrackEditing().getTrackBorders(ping).forEach(trackBorder -> {
         TrackInfo trackInfo = trackInfos.get(trackBorder.trackId());
         if (trackInfo == null) {
            return;
         }
         Cat0Datagram cat0Datagram = trackInfo.cat0Datagram();
         if (cat0Datagram == null) {
            return;
         }
         int category = cat0Datagram.getBestCategory(categoryVariable, (byte) -1);
         if (category >= 0 && categoryVariableMask[category]) {
            maskRanges.add(trackBorder.depthRange());
         }
         doneRanges.add(trackBorder.depthRange());
      });
   }

   private static void evaluateForSamples(@Nullable DiscreteVariableResult evaluation, boolean[] categoriesToMask, List<FloatRange> doneRanges, List<FloatRange> maskRanges) {
      if (evaluation == null) {
         return;
      }
      byte[] byteData = evaluation.byteData;
      float meterPerSample = evaluation.depthRange.getSize() / byteData.length;
      float minDepth = evaluation.depthRange.min();

      FloatRangeSet perSampleRangeSet = FloatRangeSet.of(evaluation.depthRange).subtract(FloatRangeSet.of(doneRanges));

      for (FloatRange floatRange : perSampleRangeSet) {
         boolean masked = false;
         int maskBeginIndex = -1;
         int iBegin = (int) ((floatRange.min() - minDepth) / meterPerSample);
         int iEnd = (int) ((floatRange.max() - minDepth) / meterPerSample);
         for (int i = iBegin; i < iEnd; i++) {
            boolean iMasked = categoriesToMask[byteData[i]];
            if (masked != iMasked) {
               if (masked) {
                  float maskBeginDepth = minDepth + maskBeginIndex * meterPerSample;
                  float maskEndDepth = minDepth + i * meterPerSample;
                  maskRanges.add(FloatRange.of(maskBeginDepth, maskEndDepth));
               } else {
                  maskBeginIndex = i;
               }
               masked = iMasked;
            }
         }
         if (masked) {
            float maskBeginDepth = minDepth + maskBeginIndex * meterPerSample;
            float maskEndDepth = minDepth + iEnd * meterPerSample;
            maskRanges.add(FloatRange.of(maskBeginDepth, maskEndDepth));
         }
      }
   }
}
