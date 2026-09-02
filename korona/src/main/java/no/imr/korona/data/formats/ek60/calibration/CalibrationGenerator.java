package no.imr.korona.data.formats.ek60.calibration;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.logging.Log;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.DefaultRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeMap;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.IntStream;

public final class CalibrationGenerator {
   private final CalibrationContent existingContent;
   private final RangeMap<Instant, CalibrationEntry> calibrationEntries = new ArrayRangeMap<>();

   private CalibrationGenerator(CalibrationContent calibrationContent) {
      existingContent = calibrationContent;
      calibrationEntries.putAll(calibrationContent.getDefaultType().getEntries());
   }

   public static @Nullable CalibrationContent extend(CalibrationContent calibrationContent, List<SegmentHandle> segmentHandles, AsyncHandle asyncHandle) {
      CalibrationGenerator calibrationGenerator = new CalibrationGenerator(calibrationContent);
      for (SegmentHandle segmentHandle : segmentHandles) {
         if (asyncHandle.isCancelled()) {
            return null;
         }
         try (SegmentData segmentData = segmentHandle.createSegmentData(NoticeHandler.ignore(), asyncHandle)) {
            calibrationGenerator.add(segmentData, asyncHandle);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error loading " + segmentHandle.getDisplayName(), e);
         }
      }
      return asyncHandle.isCancelled() ? null : calibrationGenerator.toCalibrationContent();
   }

   private void add(SegmentData segmentData, AsyncHandle asyncHandle) {
      List<? extends PingIndex> pingIndices = segmentData.getPingIndices();
      Instant begin = pingIndices.getFirst().getInstant().truncatedTo(ChronoUnit.SECONDS); // Round down
      Instant end = pingIndices.getLast().getInstant().truncatedTo(ChronoUnit.SECONDS).plusSeconds(1); // Round up
      Range<Instant> millisRange = new DefaultRange<>(begin, end);
      long overlapMillis = calibrationEntries.stream(millisRange)
            .mapToLong(entry -> entry.range().begin().until(entry.range().end(), ChronoUnit.MILLIS))
            .sum();
      if (overlapMillis > 2000) {
         return;
      }

      RawFileConfiguration rawFileConfiguration = segmentData.getPingConfiguration().getRawFileConfiguration();

      Map<Integer, Float> channelIndexToAbsorptionCoefficient = new HashMap<>();
      Map<Integer, Float> channelIndexToSoundSpeed = new HashMap<>();
      RawFileConfiguration.Xml0Info xml0Info = rawFileConfiguration.getXml0Info();
      if (xml0Info == null) {
         int maxPings = Math.min(rawFileConfiguration.getTransducerCount(), pingIndices.size());
         for (int i = 0; i < maxPings && channelIndexToAbsorptionCoefficient.size() < rawFileConfiguration.getTransducerCount(); i++) {
            if (asyncHandle.isCancelled()) {
               return;
            }
            PingIndex pingIndex = pingIndices.get(i);
            Ping ping;
            try {
               ping = segmentData.loadPing(pingIndex, asyncHandle);
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error loading ping at " + pingIndex.getInstant() + " in " + rawFileConfiguration.getDataFile(), e);
               break;
            }
            ping.getNonNullChannelDatas().forEach(channelData -> {
               int channelIndex = channelData.getChannel() - 1;
               if (!channelIndexToAbsorptionCoefficient.containsKey(channelIndex)) {
                  channelIndexToAbsorptionCoefficient.put(channelIndex, channelData.getAbsorptionCoefficient());
                  channelIndexToSoundSpeed.put(channelIndex, channelData.getSoundVelocity());
               }
            });
         }
      }

      List<ChannelCalibration> channelCalibrations = new ArrayList<>();
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
         RawFileTransducer transducer = transducers.get(channelIndex);
         ChannelCalibrationBuilder builder = new ChannelCalibrationBuilder();
         builder.id = Optional.of(transducer.getChannelId());
         builder.channel = Optional.of(channelIndex + 1);
         builder.kHz = Optional.of(transducer.getKHz());
         builder.gain = Float.isNaN(transducer.getGain()) ? Optional.empty() : Optional.of(transducer.getGain());
         builder.equivalentBeamAngle = Optional.of(transducer.getEquivalentBeamAngle());
         builder.beamWidthAlongship = Optional.of(transducer.getBeamWidthAlongship());
         builder.beamWidthAthwartship = Optional.of(transducer.getBeamWidthAthwartship());
         builder.angleOffsetAlongship = Optional.of(transducer.getAngleOffsetAlongship());
         builder.angleOffsetAthwartship = Optional.of(transducer.getAngleOffsetAthwartship());
         float[] pulseDurationTable = transducer.getPulseDurationTable();
         float[] saCorrectionTable = transducer.getSaCorrectionTable();
         builder.saCorrections = IntStream.range(0, pulseDurationTable.length)
               .boxed()
               .collect(ImmutableMap.toImmutableMap(i -> pulseDurationTable[i], i -> saCorrectionTable[i], (a, _) -> a));

         if (xml0Info != null) {
            // No absorptionCoefficient in this case.
            builder.soundVelocity = Optional.of(rawFileConfiguration.getSoundVelocityAverage());
         } else {
            builder.absorptionCoefficient = Optional.ofNullable(channelIndexToAbsorptionCoefficient.get(channelIndex));
            builder.soundVelocity = Optional.ofNullable(channelIndexToSoundSpeed.get(channelIndex));
         }

         builder.broadbandGain = transducer.getChannelCalibration().broadbandGain;
         builder.broadbandTransducerImpedance = transducer.getChannelCalibration().broadbandTransducerImpedance;
         builder.broadbandEquivalentBeamAngle = transducer.getChannelCalibration().broadbandEquivalentBeamAngle;
         builder.broadbandAngleOffsetAlongship = transducer.getChannelCalibration().broadbandAngleOffsetAlongship;
         builder.broadbandAngleOffsetAthwartship = transducer.getChannelCalibration().broadbandAngleOffsetAthwartship;
         builder.broadbandBeamWidthAlongship = transducer.getChannelCalibration().broadbandBeamWidthAlongship;
         builder.broadbandBeamWidthAthwartship = transducer.getChannelCalibration().broadbandBeamWidthAthwartship;

         channelCalibrations.add(builder.build());
      }
      CalibrationEntry calibrationEntry = new CalibrationEntry(ChannelCalibration.EMPTY, channelCalibrations);
      calibrationEntries.put(millisRange, calibrationEntry);
   }

   private CalibrationContent toCalibrationContent() {
      CalibrationType defaultCalibrationType = new CalibrationType();
      RangeMap.Entry<Instant, CalibrationEntry> previousEntry = null;
      for (RangeMap.Entry<Instant, CalibrationEntry> entry : calibrationEntries) {
         boolean combineWithPrevious = previousEntry != null                                        // There is a previous entry.
               && previousEntry.range().end().until(entry.range().begin(), ChronoUnit.SECONDS) < 5  // The gap in time is small.
               && previousEntry.value().equals(entry.value());                                      // The values are equal.
         Instant begin = combineWithPrevious
               ? previousEntry.range().end()
               : entry.range().begin();
         defaultCalibrationType.putEntry(begin, entry.range().end(), entry.value());
         previousEntry = entry;
      }
      return new CalibrationContent(defaultCalibrationType, existingContent.getTypes());
   }
}
