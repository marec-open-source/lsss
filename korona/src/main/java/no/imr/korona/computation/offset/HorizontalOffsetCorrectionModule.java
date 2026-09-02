package no.imr.korona.computation.offset;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ConfigFileSettingsException;
import no.imr.korona.computation.GeneralPingModule;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Module for horizontal offset correction.
 */
public final class HorizontalOffsetCorrectionModule extends GeneralPingModule {
   public HorizontalOffsetCorrectionModule() {
   }

   @Override
   public List<Name> getRequiredConfigFileServiceNames() {
      return List.of(HorizontalTransducerOffsetsFileService.NAME);
   }

   private TransducerParameterManager getTransducerParameterManager() throws IOException {
      Path file = getRequiredConfigFile(HorizontalTransducerOffsetsFileService.NAME);
      Document document = XmlUtils.readDocument(file);
      String type = document.getRootElement().attributeValue(TransducerParameterManager.XML_TYPE);
      if (TransducerParameters.ParameterType.VERTICAL.toString().equals(type)) {
         throw new ConfigFileSettingsException(this, HorizontalTransducerOffsetsFileService.NAME, file, "Wrong type: " + type);
      }
      return new TransducerParameterManager(TransducerParameters.ParameterType.HORIZONTAL, document);
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new HorizontalOffsetCorrectionModuleComputation(this, computationContext, pingSource);
   }

   private static final class HorizontalOffsetCorrectionModuleComputation extends GeneralPingModuleComputation {
      private final Map<Integer, TransducerParameters> channelToOffsetMap;
      private final Buffer pingBuffer;

      private HorizontalOffsetCorrectionModuleComputation(HorizontalOffsetCorrectionModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
         super(module, computationContext, pingSource);

         PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
         channelToOffsetMap = OffsetCorrectionUtils.makeChannelToOffsetMap(module.getTransducerParameterManager(), pingConfiguration);

         float largestPositiveOffset = 0;
         float largestNegativeOffset = 0;
         for (TransducerParameters offsetParameters : channelToOffsetMap.values()) {
            largestPositiveOffset = Math.max(largestPositiveOffset, -offsetParameters.getDeltaX0());
            largestNegativeOffset = Math.min(largestNegativeOffset, -offsetParameters.getDeltaX0());
         }
         pingBuffer = new Buffer(largestPositiveOffset, largestNegativeOffset);
      }

      private static double getVesselDistanceInMeters(Ping ping) {
         return KoronaUtils.nmiToMeter(ping.getVesselDistance());
      }

      private Ping processPing(Ping ping) {
         //if a PowerData is resampled, make a copy

         Ping newPing = new DefaultPing(ping.getPingConfiguration(), ping.getPingIndex(), ping.getBot0Datagram());

         double currentVesselDistanceInMeters = getVesselDistanceInMeters(ping);

         for (PingItem pingItem : ping.getPingItems()) {
            if (pingItem instanceof ChannelData channelData) {
               PowerData powerData = channelData.getPowerData();
               powerData.throwExceptionIfReadOnly();

               //process...
               int channel = powerData.getChannel();

               TransducerParameters transducerParameters = channelToOffsetMap.get(channel);
               float deltaX0 = transducerParameters != null ? -transducerParameters.getDeltaX0() : 0;
               if (deltaX0 == 0) {
                  // No interpolation.
                  newPing.add(powerData);
                  continue;
               }
               //get the 2 closest pings, given the time offset.
               double vesselDistanceInMeters = currentVesselDistanceInMeters + deltaX0;
               Ping pingBefore = pingBuffer.getNearestPingBelow(vesselDistanceInMeters, channel);
               Ping pingAfter = pingBuffer.getNearestPingAbove(vesselDistanceInMeters, channel);
               PowerData powerDataBefore = pingBefore != null ? pingBefore.getPowerData(channel) : null;
               PowerData powerDataAfter = pingAfter != null ? pingAfter.getPowerData(channel) : null;

               if (powerDataBefore == null || powerDataAfter == null) {
                  Log.global.info("Unable to interpolate channel " + channel + " at vesselDistance " + currentVesselDistanceInMeters);
                  // No interpolation.
                  newPing.add(powerData);
                  continue;
               }

               double vesselDistanceInMetersBefore = getVesselDistanceInMeters(pingBefore);
               double vesselDistanceInMetersAfter = getVesselDistanceInMeters(pingAfter);
               double pingDist = vesselDistanceInMetersAfter - vesselDistanceInMetersBefore;
               if (pingDist == 0) {
                  // No interpolation.
                  newPing.add(powerData);
                  continue;
               }

               double beforeDist = vesselDistanceInMeters - vesselDistanceInMetersBefore;
               double afterDist = vesselDistanceInMetersAfter - vesselDistanceInMeters;
               boolean beforeIsClosest = beforeDist <= afterDist;

               PowerData closestDatagram = beforeIsClosest ? powerDataBefore : powerDataAfter;
               PowerData farthestDatagram = beforeIsClosest ? powerDataAfter : powerDataBefore;

               PowerData newDatagram = powerData.makeCopy();

               double closestWeight = beforeIsClosest
                     ? afterDist / pingDist
                     : beforeDist / pingDist;
               double farthestWeight = 1 - closestWeight;

               // Interpolation in linear Sv space.
               float[] sv = newDatagram.getSv();
               float[] closestDatagramSv = closestDatagram.getSv();
               float[] farthestDatagramSv = farthestDatagram.getSv();
               for (int i = 0; i < sv.length; i++) {
                  float depth = newDatagram.getSampleDepth(i);
                  int closestSampleIndex = closestDatagram.depthToSampleIndex(depth);
                  if (closestSampleIndex < 0 || closestSampleIndex >= closestDatagramSv.length) {
                     sv[i] = 0;
                     continue;
                  }
                  int farthestSampleIndex = farthestDatagram.depthToSampleIndex(depth);
                  if (farthestSampleIndex < 0 || farthestSampleIndex >= farthestDatagramSv.length) {
                     sv[i] = 0;
                     continue;
                  }
                  sv[i] = (float) (closestWeight * closestDatagramSv[closestSampleIndex]
                        + farthestWeight * farthestDatagramSv[farthestSampleIndex]);
               }
               newDatagram.setSv(sv);

               newPing.add(newDatagram);
            } else {
               newPing.add(pingItem);
            }
         }

         return newPing;
      }

      @Override
      protected @Nullable Ping generateOutput() throws IOException {
         Ping input = pingBuffer.getCurrentPing();
         if (input == null) {
            return null;
         }

         // Process current ping
         input = processPing(input);

         return input;
      }

      /**
       * Class for handling the output queues. The current ping should as far as possible be
       * centered so that a ping corresponding to the max/min horizontal offset is contained in
       * the buffer, given the current speed.
       */
      private final class Buffer {
         // Buffers
         private final Deque<Ping> inputBuffer = new ArrayDeque<>();
         private final Deque<Ping> outputBuffer = new ArrayDeque<>();

         private static final int MAX_BUFFER_SIZE = 5;

         //Input to the buffers
         private final float maxPositiveHorizontalOffset; //in meters
         private final float maxNegativeHorizontalOffset; //in meters

         private Buffer(float maxPositiveHorizontalOffset, float maxNegativeHorizontalOffset) {
            this.maxPositiveHorizontalOffset = maxPositiveHorizontalOffset;
            this.maxNegativeHorizontalOffset = maxNegativeHorizontalOffset;
         }

         private @Nullable Ping getNearestPingAbove(double vesselDistanceInMeters, int channel) {
            // If first ping in output buffer has a lower vessel distance than vesselDistanceInMeters,
            // we only need to search the input buffer.
            Ping result = null;
            if (!outputBuffer.isEmpty() && getVesselDistanceInMeters(outputBuffer.getFirst()) >= vesselDistanceInMeters) {
               result = searchBackwards(vesselDistanceInMeters, outputBuffer, channel);
            }
            if (result == null || getVesselDistanceInMeters(result) < vesselDistanceInMeters) {
               Ping result2 = searchBackwards(vesselDistanceInMeters, inputBuffer, channel);
               if (result2 != null) {
                  result = result2;
               }
            }
            if (result == null) {
               // Last option: return the closest in output buffer.
               result = searchBackwards(vesselDistanceInMeters, outputBuffer, channel);
            }
            return result;
         }

         private static @Nullable Ping searchBackwards(double vesselDistanceInMeters, Deque<Ping> buffer, int channel) {
            // Iterate backwards through buffer.
            Ping lastPossible = null;
            for (Iterator<Ping> iterator = buffer.descendingIterator(); iterator.hasNext(); ) {
               Ping ping = iterator.next();
               if (ping.getPowerData(channel) != null) {
                  lastPossible = ping;
                  if (getVesselDistanceInMeters(ping) >= vesselDistanceInMeters) {
                     return ping;
                  }
               }
            }
            return lastPossible;
         }

         private @Nullable Ping getNearestPingBelow(double vesselDistanceInMeters, int channel) {
            Ping result = null;
            if (!inputBuffer.isEmpty() && getVesselDistanceInMeters(inputBuffer.getLast()) <= vesselDistanceInMeters) {
               result = searchForwards(vesselDistanceInMeters, inputBuffer, channel);
            }
            if (result == null || getVesselDistanceInMeters(result) > vesselDistanceInMeters) {
               Ping result2 = searchForwards(vesselDistanceInMeters, outputBuffer, channel);
               if (result2 != null) {
                  result = result2;
               }
            }
            if (result == null) {
               // Last option: return the closest in input buffer.
               result = searchForwards(vesselDistanceInMeters, inputBuffer, channel);
            }
            return result;
         }

         private static @Nullable Ping searchForwards(double vesselDistanceInMeters, Deque<Ping> buffer, int channel) {
            // Iterate forwards output buffer.
            Ping lastPossible = null;
            for (Ping ping : buffer) {
               if (ping.getPowerData(channel) != null) {
                  lastPossible = ping;
                  if (getVesselDistanceInMeters(ping) <= vesselDistanceInMeters) {
                     return ping;
                  }
               }
            }
            return lastPossible;
         }

         private @Nullable Ping getCurrentPing() throws IOException {
            // If the input buffer is empty, read a datagram.
            if (inputBuffer.isEmpty()) {
               Ping input = inputPing();
               if (input == null) {
                  return null;
               }
               inputBuffer.addFirst(input);
            }

            Ping currentPing = inputBuffer.removeLast();

            outputBuffer.addFirst(currentPing);

            updateBuffersBasedOnIdx0(currentPing);
            //decreaseBuffersBasedOnSpeed(currentPing);

            return currentPing;
         }

         private void updateBuffersBasedOnIdx0(Ping currentPing) throws IOException {
            // Check that the input buffer is long enough to contain all relevant pings.

            double currentVesselDistance = getVesselDistanceInMeters(currentPing);

            if (inputBuffer.isEmpty() || getVesselDistanceInMeters(inputBuffer.getFirst()) < currentVesselDistance + maxPositiveHorizontalOffset) {
               //boolean notEnoughRawGreaterThanMax = true;
               boolean notEnoughRawGreaterThanMax = inputBuffer.size() < MAX_BUFFER_SIZE;
               //read new datagrams until enough in buffer
               while (notEnoughRawGreaterThanMax) {
                  Ping input = inputPing();
                  if (input == null) {
                     break;
                  }
                  inputBuffer.addFirst(input);
                  notEnoughRawGreaterThanMax = getVesselDistanceInMeters(input) < currentVesselDistance + maxPositiveHorizontalOffset && inputBuffer.size() < MAX_BUFFER_SIZE;
               }
            }

            // Decrease output queue.
            Ping lastNeededPing = null;
            int index = 0;
            for (Ping ping : outputBuffer) {
               double vesselDistanceInMeters = getVesselDistanceInMeters(ping);
               if (vesselDistanceInMeters <= currentVesselDistance + maxNegativeHorizontalOffset || index >= MAX_BUFFER_SIZE) {
                  lastNeededPing = ping;
                  break;
               }
               index++;
            }
            if (lastNeededPing != null) {
               for (Iterator<Ping> iterator = outputBuffer.descendingIterator(); iterator.hasNext(); ) {
                  Ping ping = iterator.next();
                  if (!ping.equals(lastNeededPing)) {
                     outputBuffer.removeLast();
                  } else {
                     break;
                  }
               }
            }
         }
      }
   }
}
