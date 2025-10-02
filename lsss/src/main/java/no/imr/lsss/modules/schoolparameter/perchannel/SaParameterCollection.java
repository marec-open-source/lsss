package no.imr.lsss.modules.schoolparameter.perchannel;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;

public final class SaParameterCollection implements PerChannelParameterCollection {
   private static final SchoolParameter SA = new SchoolParameter(new Name("sA"), Unit.SA);

   public SaParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(SA);
   }

   @Override
   public PerChannelComputer createComputer() {
      return new Computer();
   }

   private static final class Computer implements PerChannelComputer {
      private double accumulatedSvIntegral;
      private double accumulatedDistance;

      private Computer() {
      }

      @Override
      public void accumulate(Ping ping, double pingWidthMeters, int channel, List<FloatRange> depthRanges) {
         PowerData powerData = ping.getPowerData(channel);
         if (powerData == null) {
            return;
         }
         float[] svArray = powerData.getSv();
         double sumSv = 0;
         for (FloatRange depthRange : depthRanges) {
            int iBegin = powerData.depthToClampedSampleIndex(depthRange.min());
            int iEnd = powerData.depthToClampedSampleIndex(depthRange.max());
            for (int iSv = iBegin; iSv < iEnd; iSv++) {
               float sv = svArray[iSv];
               sumSv += sv;
            }
         }
         double sampleDistance = powerData.getSampleDistance();
         accumulatedSvIntegral += sumSv * sampleDistance * pingWidthMeters;
         accumulatedDistance += pingWidthMeters;
      }

      @Override
      public Map<String, Float> getValues() {
         double sA = accumulatedDistance > 0 ? accumulatedSvIntegral / accumulatedDistance : 0;
         return Map.of(SA.getPersistentName(), (float) sA);
      }
   }
}
