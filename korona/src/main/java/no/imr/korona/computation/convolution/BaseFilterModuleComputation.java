package no.imr.korona.computation.convolution;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.ResampledBooleanArray;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.korona.data.util.TvgArray;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.range.ArrayRangeMap;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeMap;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

abstract class BaseFilterModuleComputation extends GeneralPingModuleComputation {
   private final BaseFilterModule module;
   private final Queue<Ping> outputQueue = new ArrayDeque<>();

   private final int channelsIn;
   private int currentPingIndex = -1;
   private @Nullable Ping currentPing;
   private final List<Ping> pingBuffer = new ArrayList<>();

   private float pastDistanceNmi;
   private float futureDistanceNmi;

   private final float[] transducerBlindZones;

   private final ChannelBuffer[] channelBuffers;

   BaseFilterModuleComputation(BaseFilterModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;
      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();
      RawFileConfiguration rawFileConfiguration = newPingConfiguration.getRawFileConfiguration();

      Path file = module.getRequiredConfigFile(TransducerRangesFileService.NAME);
      TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(file));

      channelsIn = rawFileConfiguration.getTransducerCount();
      channelBuffers = new ChannelBuffer[channelsIn];
      transducerBlindZones = new float[channelsIn];
      for (int channel = 1; channel <= channelsIn; channel++) {
         channelBuffers[channel - 1] = new ChannelBuffer();
         RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channel - 1);
         transducer.setChannelId(transducer.getChannelId() + " Filtered");
         transducerBlindZones[channel - 1] = transducerParameterManager.getBlindZone(transducer.getKHz()).orElse(0f);
      }

      setNewPingConfiguration(newPingConfiguration);
   }

   /**
    * Sets the horizontal extent of the datagram buffer.
    *
    * @param rangeInMeters range in meters
    */
   void setDistances(FloatRange rangeInMeters) {
      pastDistanceNmi = (float) KoronaUtils.meterToNmi(rangeInMeters.min());
      futureDistanceNmi = (float) KoronaUtils.meterToNmi(rangeInMeters.max());
   }

   /**
    * Utility function which places the positions in an array.
    *
    * @param filterInputs used for horizontal filtering
    * @return an array of the positions
    */
   static float[] getPositions(FilterInput[] filterInputs) {
      float[] positions = new float[filterInputs.length];
      for (int i = 0; i < filterInputs.length; i++) {
         positions[i] = filterInputs[i].position;
      }
      return positions;
   }

   /**
    * Specifies the array to be operated on, e.g. sv, logSv...
    *
    * @param powerData the sample datagram to get values from
    * @return an array of the values which should be used in the filtering
    */
   abstract float[] getArray(PowerData powerData);

   private PowerData filter(RawAndMask currentDatagram,
                            List<FilterInput> filterInputs,
                            int newChannel) {
      PowerData result;
      result = filterHorizontally(currentDatagram, filterInputs, newChannel);
      result = filterVertically(result, currentDatagram.mask, newChannel);
      return result;
   }

   private PowerData filterHorizontally(RawAndMask currentDatagram,
                                        List<FilterInput> filterInputs,
                                        int newChannel) {
      PowerData result = currentDatagram.powerData.makeCopyWithAnglesOnly();
      result.setChannel(newChannel);

      int startI = 0;
      float[] output = getArray(result);
      while (startI < output.length) {
         FilterInput[] filterInput = makeFilterInputArray(filterInputs, startI);
         if (filterInput.length == 0) {
            break;
         }
         int nextStartI = getFilterInputNextStartIndex(filterInputs, startI);

         doFilterHorizontally(filterInput, startI, nextStartI, output, currentDatagram);

         startI = nextStartI;
      }

      return result;
   }

   /**
    * Does horizontal filtering / smoothing over a specified vertical index range.
    *
    * @param filterInputs    the FilterInputs to use
    * @param startI          vertical start index
    * @param endI            vertical end index
    * @param output          the array to place the result in
    * @param currentDatagram the center datagram
    */
   abstract void doFilterHorizontally(FilterInput[] filterInputs,
                                      int startI, int endI,
                                      float[] output, RawAndMask currentDatagram);

   /**
    * Does vertical filtering / smoothing.
    *
    * @param rawInput   the datagram to filter
    * @param mask       indicates which values to use
    * @param newChannel the channel for the result
    * @return a new datagram with the result
    */
   abstract PowerData filterVertically(PowerData rawInput,
                                       boolean[] mask,
                                       int newChannel);

   /**
    * Makes a list with info about all sample datagrams to be used in the filtering.
    *
    * @param currentRawAndMask the center
    * @param datagramBuffer    a list of RawAndMask elements
    * @return a list of FilterInput elements to be used in horizontal filtering
    */
   private List<FilterInput> makeFilterInputs(RawAndMask currentRawAndMask, List<@Nullable RawAndMask> datagramBuffer) {
      List<FilterInput> filterInput = new ArrayList<>(datagramBuffer.size());
      for (RawAndMask rawAndMask : datagramBuffer) {
         if (rawAndMask == null) {
            continue;
         }

         ResampledFloatArray resampledFloatArray =
               ResampledFloatArray.create(getArray(rawAndMask.powerData), rawAndMask.powerData, currentRawAndMask.powerData);

         ResampledBooleanArray mask = ResampledBooleanArray.create(rawAndMask.mask, rawAndMask.powerData, currentRawAndMask.powerData);
         assert mask.offset() == resampledFloatArray.offset();

         filterInput.add(new FilterInput(
               resampledFloatArray.values(),
               mask.values(),
               (float) KoronaUtils.nmiToMeter(rawAndMask.ping.getVesselDistance() - currentRawAndMask.ping.getVesselDistance()),
               resampledFloatArray.offset()
         ));
      }
      return filterInput;
   }

   /**
    * Find next upper limit, <code>i<sub>end</sub></code>, when filtering horizontally.
    * That is, the interval <code>[i, i<sub>end</sub>)</code> is such that no array in filterInputs
    * either starts or stops within it.
    *
    * @param filterInputs the list of all filter inputs
    * @param i            the start index
    * @return the end index
    */
   private static int getFilterInputNextStartIndex(List<FilterInput> filterInputs, int i) {
      int nextIstart = Integer.MAX_VALUE;
      for (FilterInput filterInput : filterInputs) {
         int offset = filterInput.offset;
         int length = filterInput.values.length;
         if (i + offset < 0) {
            nextIstart = Math.min(nextIstart, -offset);
         } else if (i + offset < length) {
            nextIstart = Math.min(nextIstart, length - offset);
         }
      }
      return nextIstart;
   }

   /**
    * Makes an array of the filter input whose arrays are defined for i + offset.
    * This filter input is valid for indices less than
    * the upper limit returned by getFilterInputNextStartIndex.
    *
    * @param filterInputs the list of all filter inputs
    * @param i            the start index
    * @return the filter input array
    */
   private static FilterInput[] makeFilterInputArray(List<FilterInput> filterInputs, int i) {
      List<FilterInput> result = new ArrayList<>(filterInputs.size());
      for (FilterInput filterInput : filterInputs) {
         int offset = filterInput.offset;
         int length = filterInput.values.length;
         if (i + offset >= 0 && i + offset < length) {
            result.add(filterInput);
         }
      }
      return result.toArray(FilterInput[]::new);
   }

   /**
    * Make currentDatagram point to next element in pingBuffer.
    * Get new datagram if necessary.
    *
    * @throws IOException if error
    */
   private void advanceCurrentDatagram() throws IOException {
      if (currentPing == null || currentPingIndex == pingBuffer.size() - 1) {
         if (currentPing == null) {
            currentPingIndex = -1;
         }
         Ping ping = inputPing();
         if (ping == null) {
            currentPingIndex = -1;
            currentPing = null;
            return;
         }
         increaseBuffer(ping);
      }
      currentPingIndex++;
      currentPing = pingBuffer.get(currentPingIndex);
   }

   /**
    * Removes old datagrams and gets new datagrams based on the time of currentDatagram,
    * such that pingBuffer contains datagrams within a given distance.
    * If currentDatagram == null, i.e. the end is reached, the buffer is emptied.
    * If currentDatagram is not a raw datagram no adjustments are made.
    *
    * @throws IOException if error
    */
   private void adjustBuffer() throws IOException {
      if (currentPing == null) {
         while (!pingBuffer.isEmpty()) {
            reduceBuffer();
         }
         return;
      }

      removeOldPings();

      addNewPings();
   }

   private void removeOldPings() {
      while (!pingBuffer.isEmpty()) {
         if (currentPing == null) {
            break;
         }
         Ping ping = pingBuffer.getFirst();

         int pingsBehind = currentPingIndex;
         boolean tooMany = pingsBehind > module.maxPing.getIntValue();
         boolean tooFew = pingsBehind < module.minPing.getIntValue();
         boolean tooOld = ping.getVesselDistance() < currentPing.getVesselDistance() + pastDistanceNmi;
         if (tooFew || (!tooOld && !tooMany)) {
            break;
         }

         reduceBuffer();
      }
   }

   private void addNewPings() throws IOException {
      while (true) {
         if (currentPing == null) {
            break;
         }
         Ping peekedPing = peekPingSourcePing(0);
         if (peekedPing == null) {
            break;
         }
         int pingsAhead = pingBuffer.size() - currentPingIndex - 1;
         boolean tooMany = pingsAhead > module.maxPing.getIntValue();
         boolean tooFew = pingsAhead < module.minPing.getIntValue();
         boolean tooNew = peekedPing.getVesselDistance() > currentPing.getVesselDistance() + futureDistanceNmi;
         if (tooMany || (tooNew && !tooFew)) {
            break;
         }

         Ping ping = inputPing();
         assert ping != null;
         increaseBuffer(ping);
      }
   }

   private void increaseBuffer(Ping ping) {
      Dep0Datagram dep0Datagram = ping.getDep0Datagram();
      List<RegionBorderDatagram> regionBorderDatagrams = ping.getPingItems(RegionBorderDatagram.class).toList();
      List<TBR0Datagram> tbr0Datagrams = ping.getPingItems(TBR0Datagram.class).toList();

      pingBuffer.add(ping);
      for (int channel = 1; channel <= channelsIn; channel++) {
         PowerData powerData = ping.getPowerData(channel);
         RawAndMask rawAndMask;
         if (powerData != null) {
            float blindZone = transducerBlindZones[channel - 1];
            rawAndMask = new RawAndMask(module, ping, powerData, dep0Datagram, regionBorderDatagrams,
                  tbr0Datagrams, blindZone);
         } else {
            rawAndMask = null;
         }
         channelBuffers[channel - 1].buffer.add(rawAndMask);
      }
   }

   /**
    * Moves one ping from pingBuffer to the output queue.
    * Special action is taken for raw-datagrams.
    */
   private void reduceBuffer() {
      Ping ping = pingBuffer.removeFirst();
      for (ChannelBuffer channelBuffer : channelBuffers) {
         channelBuffer.buffer.removeFirst();
      }

      outputQueue.add(ping);

      if (pingBuffer.isEmpty() || currentPing == null) {
         currentPingIndex = -1;
         currentPing = null;
      } else {
         currentPingIndex--;
         currentPing = pingBuffer.get(currentPingIndex);
      }
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      while (outputQueue.isEmpty()) {
         if (getAsyncHandle().isCancelled()) {
            return null;
         }

         advanceCurrentDatagram();
         adjustBuffer();

         if (currentPing == null) {
            break;
         }

         List<PowerData> nonNullPowerDatas = currentPing.getNonNullPowerDatas().toList(); // Get list before modifying ping.
         for (PowerData powerData : nonNullPowerDatas) {
            int channel = powerData.getChannel();
            if (module.onlyLastChannel.getBooleanValue() && channel != getPingConfiguration().getRawFileConfiguration().getTransducerCount()) {
               continue;
            }

            List<@Nullable RawAndMask> buffer = channelBuffers[channel - 1].buffer;

            RawAndMask currentRaw = buffer.get(currentPingIndex);
            if (currentRaw == null) {
               continue;
            }

            List<FilterInput> filterInputs = makeFilterInputs(currentRaw, buffer);
            PowerData result = filter(currentRaw, filterInputs, channel);

            if (currentPing != null) {
               currentPing.remove(powerData);
               currentPing.add(result);
            }
         }
      }
      return outputQueue.poll();
   }

   private static final class ChannelBuffer {
      private final List<@Nullable RawAndMask> buffer = new ArrayList<>();

      private ChannelBuffer() {
      }
   }

   /**
    * Used for filtering / smoothing horizontally.
    * Sample values and masking are resampled to the resolution of the center datagram,
    * which is going to be modified.
    */
   static final class FilterInput {
      /**
       * Resampled values.
       */
      final float[] values;

      /**
       * Resampled mask.
       */
      final boolean[] mask;

      /**
       * Position relative to the center datagram.
       */
      final float position;

      /**
       * Vertical offset relative to the center datagram.
       */
      final int offset;

      private FilterInput(float[] values, boolean[] mask, float position, int offset) {
         this.values = values;
         this.mask = mask;
         this.position = position;
         this.offset = offset;
      }
   }

   /**
    * Grouping of a PowerData and a mask.
    * Masking can be done with respect to bottom and noise.
    * The samples masked away are excluded from filtering / smoothing.
    */
   static final class RawAndMask {
      private final Ping ping;
      final PowerData powerData;
      final boolean[] mask;

      /**
       * Initializes the mask.
       *
       * @param ping           the ping
       * @param powerData      the raw datagram
       * @param dep            the depth datagram to use for masking away samples below bottom
       * @param regionBorders  collection of {@link RegionBorderDatagram}
       * @param tbr0Datagrams  collection of {@link TBR0Datagram}
       * @param blindZoneRange the blind zone range
       */
      private RawAndMask(BaseFilterModule module, Ping ping, PowerData powerData, @Nullable Dep0Datagram dep,
                         Collection<RegionBorderDatagram> regionBorders, Collection<TBR0Datagram> tbr0Datagrams, float blindZoneRange) {
         this.ping = ping;
         this.powerData = powerData;

         mask = new boolean[powerData.getCount()];
         Arrays.fill(mask, true);

         if (module.maskPelagic.getBooleanValue() && dep != null) {
            maskAboveBottom(dep);
         }
         if (module.maskBottom.getBooleanValue() && dep != null) {
            maskBelowBottom(dep);
         }
         if (module.maskNoise.getBooleanValue()) {
            maskNoise();
         }
         maskBlindZone(blindZoneRange);
         if (module.maskSecondBottom.getBooleanValue() && dep != null) {
            maskBelowSecondBottom(dep);
         }
         maskRegions(regionBorders, module.maskRegion.getValue());
         maskTracks(tbr0Datagrams, module.maskTrack.getValue());
      }

      private void maskBelowSecondBottom(Dep0Datagram dep) {
         int secondBottomIndex = powerData.depthToSampleIndex(dep.getDepth() * 2 - 10); // 10 m above second bottom echo.
         secondBottomIndex = validIndex(secondBottomIndex);
         Arrays.fill(mask, secondBottomIndex, mask.length, false);
      }

      private void maskAboveBottom(Dep0Datagram dep) {
         int bottomIndex = powerData.depthToSampleIndex(dep.getDepth());
         bottomIndex = validIndex(bottomIndex);
         Arrays.fill(mask, 0, bottomIndex, false);
      }

      private void maskBelowBottom(Dep0Datagram dep) {
         int bottomIndex = powerData.depthToSampleIndex(dep.getMinimumDepth());
         bottomIndex = validIndex(bottomIndex);
         Arrays.fill(mask, bottomIndex, mask.length, false);
      }

      private void maskNoise() {
         float noiseThreshold = powerData.getNoisePowerIndexNoiseThreshold();
         float[] sv = powerData.getSv();
         TvgArray tvg = powerData.getTVGArray();
         for (int i = 0; i < mask.length; i++) {
            float noise = sv[i] / tvg.get(i);
            if (noise < noiseThreshold) {
               mask[i] = false;
            }
         }
      }

      private void maskBlindZone(float blindZoneRange) {
         int firstIndex = validIndex((int) Math.ceil(powerData.rangeToSampleIndexAsFloat(blindZoneRange)));
         Arrays.fill(mask, 0, firstIndex, false);
      }

      private void maskRegions(Collection<RegionBorderDatagram> regionBorders, BaseFilterModule.RegionMasking regionMasking) {
         if (regionMasking == BaseFilterModule.RegionMasking.none) {
            return;
         }

         RangeMap<Integer, Boolean> insideMap = new ArrayRangeMap<>();
         insideMap.put(0, mask.length, false);
         for (RegionBorderDatagram regionBorder : regionBorders) {
            for (RegionBorderDatagram.BorderInfo borderInfo : regionBorder.getBorderInfos()) {
               int begin = validIndex(powerData.depthToSampleIndex(borderInfo.startDepth()));
               int end = validIndex(powerData.depthToSampleIndex(borderInfo.endDepth()) + 1);
               insideMap.put(begin, end, true);
            }
         }

         mask(insideMap, regionMasking == BaseFilterModule.RegionMasking.inside);
      }

      private void maskTracks(Collection<TBR0Datagram> tbr0Datagrams, BaseFilterModule.RegionMasking regionMasking) {
         if (regionMasking == BaseFilterModule.RegionMasking.none) {
            return;
         }

         RangeMap<Integer, Boolean> insideMap = new ArrayRangeMap<>();
         insideMap.put(0, mask.length, false);
         for (TBR0Datagram tbr0Datagram : tbr0Datagrams) {
            if (tbr0Datagram.getChannel() != powerData.getChannel()) {
               continue;
            }
            FloatRange depthRange = tbr0Datagram.getDepthRange();
            int begin = validIndex(powerData.depthToSampleIndex(depthRange.min()));
            int end = validIndex(powerData.depthToSampleIndex(depthRange.max()) + 1);
            insideMap.put(begin, end, true);
         }

         mask(insideMap, regionMasking == BaseFilterModule.RegionMasking.inside);
      }

      private void mask(RangeMap<Integer, Boolean> insideMap, boolean maskInside) {
         for (RangeMap.Entry<Integer, Boolean> entry : insideMap) {
            if (entry.value() == maskInside) {
               Arrays.fill(mask, entry.range().begin(), entry.range().end(), false);
            }
         }
      }

      private int validIndex(int i) {
         return Math.clamp(i, 0, mask.length);
      }
   }
}
