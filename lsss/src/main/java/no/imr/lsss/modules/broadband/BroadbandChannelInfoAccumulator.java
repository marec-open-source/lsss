package no.imr.lsss.modules.broadband;

import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.lsss.modules.broadband.pojo.ExportChannelInfo;
import no.imr.lsss.modules.broadband.pojo.ExportChannelInfoParameters;
import no.imr.tools.misc.JsonUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Accumulates channel info.
 */
public final class BroadbandChannelInfoAccumulator {
   private final Map<String, PerChannelAccumulator> channelIdToPerChannelAccumulator = new LinkedHashMap<>();

   public BroadbandChannelInfoAccumulator() {
   }

   public List<ExportChannelInfo> getChannelInfos() {
      List<ExportChannelInfo> channelInfos = new ArrayList<>();
      channelIdToPerChannelAccumulator.forEach((channelId, accumulator) -> {
         channelInfos.add(new ExportChannelInfo(channelId, accumulator.toChannelInfos()));
      });
      return channelInfos;
   }

   public void accumulate(BroadbandData broadbandData) {
      String channelId = broadbandData.getTransducer().getChannelId();
      PerChannelAccumulator perChannelAccumulator = channelIdToPerChannelAccumulator.get(channelId);
      if (perChannelAccumulator == null) {
         perChannelAccumulator = new PerChannelAccumulator();
         channelIdToPerChannelAccumulator.put(channelId, perChannelAccumulator);
      }
      perChannelAccumulator.accumulate(broadbandData);
   }

   public static void addUnits(Map<String, String> units) {
      // units.put("startTime", "");
      // units.put("endTime", "");
      units.put("nominalFrequency", "Hz");
      units.put("minFrequency", "Hz");
      units.put("maxFrequency", "Hz");
      units.put("transmitPower", "W");
      // units.put("pulseType", "");
      units.put("pulseDuration", "s");
      units.put("bandWidth", "Hz");
      units.put("sampleInterval", "s");
      units.put("slope", "1");
   }

   private static final class PerChannelAccumulator {
      private final NavigableMap<Long, ExportChannelInfoParameters> map = new TreeMap<>();
      private final Map<ExportChannelInfoParameters, ExportChannelInfoParameters> canonicalChannelParametersMap = new HashMap<>();

      private PerChannelAccumulator() {
      }

      private List<Map<String, Object>> toChannelInfos() {
         List<Map<String, Object>> channelInfos = new ArrayList<>();
         ExportChannelInfoParameters currentParameters = null;
         long minTime = 0;
         long maxTime = 0;
         for (Map.Entry<Long, ExportChannelInfoParameters> entry : map.entrySet()) {
            long time = entry.getKey();
            ExportChannelInfoParameters parameters = entry.getValue();
            if (!Objects.equals(currentParameters, parameters)) {
               if (currentParameters != null) {
                  channelInfos.add(toParameterMap(minTime, maxTime, currentParameters));
               }
               currentParameters = parameters;
               minTime = time;
               maxTime = time;
            } else {
               maxTime = time;
            }
         }
         if (currentParameters != null) {
            channelInfos.add(toParameterMap(minTime, maxTime, currentParameters));
         }
         return channelInfos;
      }

      private static Map<String, Object> toParameterMap(long minTime, long maxTime, ExportChannelInfoParameters currentParameters) {
         Map<String, Object> map = new LinkedHashMap<>();
         map.put("startTime", Instant.ofEpochMilli(minTime).toString());
         map.put("endTime", Instant.ofEpochMilli(maxTime).toString());
         map.putAll(JsonUtils.convertToMap(currentParameters));
         return map;
      }

      private void accumulate(BroadbandData broadbandData) {
         // Broadband data can be accumulated in any order and possibly repeatedly

         map.computeIfAbsent(broadbandData.getTimeInMillis(), t -> {
            ExportChannelInfoParameters channelParameters = rawToChannelParameters(broadbandData);
            return canonicalChannelParametersMap.computeIfAbsent(channelParameters, key -> channelParameters);
         });
      }

      private static ExportChannelInfoParameters rawToChannelParameters(BroadbandData broadbandData) {
         RawFileTransducer transducer = broadbandData.getTransducer();
         RawFileTransducer.Xml0Info transducerXml0Info = transducer.getXml0Info();
         PulseCompression pulseCompression = broadbandData.getPulseCompression();
         ExportChannelInfoParameters channelParameters = new ExportChannelInfoParameters();
         channelParameters.nominalFrequency = transducer.getFrequency();
         channelParameters.minFrequency = pulseCompression.getConfig().frequencyRange().min();
         channelParameters.maxFrequency = pulseCompression.getConfig().frequencyRange().max();
         channelParameters.transmitPower = broadbandData.getTransmitPower();
         channelParameters.pulseType = "linearFrequencyModulation";
         channelParameters.pulseDuration = broadbandData.getPulseDuration();
         channelParameters.bandWidth = broadbandData.getBandWidth();
         channelParameters.sampleInterval = broadbandData.getSampleInterval();
         channelParameters.slope = broadbandData.getSlope();
         if (transducerXml0Info != null) {
            channelParameters.serialNumber = transducerXml0Info.getTransducerSerialNumber();
         }
         return channelParameters;
      }
   }
}
