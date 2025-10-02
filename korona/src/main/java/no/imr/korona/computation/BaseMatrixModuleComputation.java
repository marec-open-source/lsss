package no.imr.korona.computation;

import no.imr.korona.computation.filters.FillMissingDataModule;
import no.imr.korona.data.datagrams.BaseDepDatagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public abstract class BaseMatrixModuleComputation extends GeneralPingModuleComputation {
   /**
    * Value to describe missing pingdata.
    */
   private static final float UNDEFINED = -200000000f;

   private final BaseMatrixModule module;
   private final Queue<Ping> outputQueue = new ArrayDeque<>();

   /**
    * Index of the middle ping in raw0Array.
    */
   private final int center;

   /**
    * ValueType enum, specifies what values that are being accessed and manipulated.
    */
   protected final BaseMatrixModule.ValueType valueType;

   private final int transducerCount;
   private final int firstChannelIndexToProcess;

   /**
    * A buffer(queue) storing all sorts of pings so that nonraw0 pings come in correct order out of module.
    */
   private final Deque<Ping> allPingQueue = new LinkedList<>();

   /**
    * A buffer(queue) with pings only of type PowerData.
    */
   private final LinkedList<Ping> raw0PingQueue = new LinkedList<>();

   /**
    * Quick reference into raw0PingQueue's raw0 datagram arrays for time 0 to center-1. For time=center to end it consists of deepcopies of the raw0 datagram values.
    */
   private final float[][][] raw0Array;

   /**
    * Quick reference into mid ping raw0 datagram arrays.
    */
   private final float[][] resultArray;

   /**
    * The maximum heave in pixels over all channels since rewind was called.
    */
   private int maxPixelHeave = 0;

   protected BaseMatrixModuleComputation(BaseMatrixModule module, ComputationContext computationContext, PingSource pingSource) {
      super(module, computationContext, pingSource);

      this.module = module;
      center = module.getCenter();
      valueType = module.logarithmicValues.getBooleanValue() ? BaseMatrixModule.ValueType.LOG_SV : BaseMatrixModule.ValueType.SV;

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      transducerCount = pingConfiguration.getRawFileConfiguration().getTransducerCount();
      resultArray = new float[transducerCount][];
      raw0Array = new float[transducerCount][2 * center + 1][];

      for (int chan = 0; chan < transducerCount; chan++) {
         resultArray[chan] = Utils.EMPTY_FLOAT_ARRAY;
         for (int time = 0; time <= center; time++) {
            raw0Array[chan][time] = Utils.EMPTY_FLOAT_ARRAY;
         }
      }

      if (module.onlyLast.getBooleanValue()) {
         firstChannelIndexToProcess = transducerCount - 1;
      } else {
         int n = module.channelsToProcess.getIntValue();
         firstChannelIndexToProcess = n < 0
               ? 0                                  // Process all channels.
               : Math.max(0, transducerCount - n);  // Process the n last channels.
      }
   }

   /**
    * Overload this function to get a per-ping callback.
    * If GUI parameters are set so that all pingdata is to be processed, then
    * startDepth starts one index after first index and endDepth starts one index before last index
    * of the ping.
    * if onlyLast is true, the function will only be called with channelIndex=last channel,
    * else it will be called for each channelIndex.
    *
    * @param startDepth   as defined in the GUI
    * @param endDepth     as defined in the GUI
    * @param channelIndex current channel, starts at 0
    */
   protected abstract void doPing(int startDepth, int endDepth, int channelIndex);

   /**
    * returns meter length of one sample(pixel) depthwise.
    *
    * @param channelIndex current channel, starts at 0
    * @return meter length of one sample(pixel) depthwise
    */
   protected final float getMeterPerSampleDepth(int channelIndex) {
      return getMidRaw0(channelIndex).getSampleDistance();
   }

   /**
    * {@return a list of which channels exist for the middle (center) ping}
    * Channel index starts at 0.
    */
   protected final List<Integer> getCurrentChannelIndexArray() {
      List<Integer> res = new ArrayList<>(0);
      for (int f = 0; f < transducerCount; f++) {
         if (resultArray[f].length != 0) {
            res.add(f);
         }
      }
      return res;
   }

   /**
    * {@return a ping}
    *
    * @param time in range [0, center], where 0 is the current ping, and higher times are newer pings
    */
   protected final Ping getPing(int time) {
      return raw0PingQueue.get(time);
   }

   /**
    * {@return the number of samples(pixels) for a given time and channel}
    *
    * @param time         in range [0, center], where 0 is the current ping, and higher times are newer pings
    * @param channelIndex current channel, starts at 0
    */
   protected final int getSampleCount(int time, int channelIndex) {
      Ping ping = raw0PingQueue.get(time);
      PowerData powerData = ping.getPowerData(channelIndex + 1);
      if (powerData == null) {
         return 0;
      }
      return powerData.getCount();
   }

   /**
    * Given depth in meters corresponding depth in pixels is returned.
    *
    * @param depth        in meters
    * @param channelIndex current channel, starts at 0
    * @return depth in pixels
    */
   protected final int depthMeterToSampleIndex(float depth, int channelIndex) {
      return getMidRaw0(channelIndex).depthToSampleIndex(depth);
   }

   protected final int getPixelHeave(int channelIndex) {
      PowerData mid = getMidRaw0(channelIndex);
      return Math.round(mid.getHeave() / mid.getSampleDistance());
   }

   /**
    * Returns unprocessed data given a channel, sampleindex and time offset.
    *
    * @param channelIndex which channel
    * @param sampleIndex  depth index
    * @param offset       how many pings to left or right of middle ping.
    *                     Positive offset returns newer data, negative offset returns older data.
    * @return value of the specified samplepoint. {@link #UNDEFINED} is returned if undefined.
    */
   public final float getRawDataFromBuffer(int channelIndex, int sampleIndex, int offset) {
      float[][] raw0ArrayForChannel = raw0Array[channelIndex];
      int i = center + offset;
      if (i < 0 || i >= raw0ArrayForChannel.length) {
         return valueType.getValueFromLogSv(UNDEFINED);
      }
      float[] r0 = raw0ArrayForChannel[i];
      if (sampleIndex < 0 || sampleIndex >= r0.length) {
         return valueType.getValueFromLogSv(UNDEFINED);
      }
      return r0[sampleIndex];
   }

   public final int getRawDataFromBuffer(int channelIndex, int sampleIndex, int offset, float[] destination, int destinationIndex, int count) {
      float[][] raw0ArrayForChannel = raw0Array[channelIndex];
      int i = center + offset;
      if (i < 0 || i >= raw0ArrayForChannel.length) {
         return 0;
      }
      float[] r0 = raw0ArrayForChannel[i];
      int beginIndex = Math.max(0, sampleIndex);
      int endIndex = Math.min(r0.length, sampleIndex + count);
      int length = endIndex - beginIndex;
      if (length <= 0) {
         return 0;
      }
      System.arraycopy(r0, beginIndex, destination, destinationIndex, length);
      return length;
   }

   /**
    * Sets data in mid ping.
    *
    * @param channelIndex which channel
    * @param sampleIndex  depth index
    * @param newValue     what value to give the specified samplepoint.
    *                     Note, this value will not be seen by getRawDataFromBuffer, but the datagram
    *                     will be altered.
    */
   public final void setData(int channelIndex, int sampleIndex, float newValue) {
      if (resultArray[channelIndex].length <= sampleIndex) {
         return;
      }
      resultArray[channelIndex][sampleIndex] = newValue;
   }

   /**
    * Returns array of logSv or sv values.
    *
    * @param valueType    what kind of data in the array
    * @param channelIndex current channel, starts at 0
    * @param time         in range [0, center]
    * @return float array
    */
   public float[] getArray(BaseMatrixModule.ValueType valueType, int channelIndex, int time) {
      Ping ping = raw0PingQueue.get(time);
      PowerData powerData = ping.getPowerData(channelIndex + 1);
      if (powerData == null) {
         return Utils.EMPTY_FLOAT_ARRAY;
      }
      return valueType.getArray(powerData);
   }

   /**
    * Buffers up data, makes deepcopies and manages resultArray, raw0PingQueue and raw0Array.
    */
   private void bufferUpData() {
      for (int chan = 0; chan < transducerCount; chan++) {
         resultArray[chan] = getArray(valueType, chan, 0); // let resultArray point directly to data arrays of mid powerData
      }

      // Elements in raw0Array[chan][ range < center,size()]  ] are shifted to the left and refer to
      // arrays in datagrams. A fresh datagram is put into Element raw0Array[chan][ size()]

      // Elements in raw0Array[chan][ range[0, center]]  ] are deepcopies of the datagram arrays
      // and are also shifted to the left

      int size = raw0PingQueue.size();
      if (size > center) {// then there is enough buffered up data.
         int chanStart = 0;
         // if only last channel is being processed, then only update the datastructures for the last channel.
         //     if (onlyLast.getBooleanValue()==true)  chanStart= getChannelCount()-1;

         for (int chan = chanStart; chan < transducerCount; chan++) {
            // shift left with wraparound for data in range [0, center]
            float[] tmp = raw0Array[chan][0];
            for (int time = 0; time < center; time++) {
               raw0Array[chan][time] = raw0Array[chan][time + 1];  // shift left
            }
            raw0Array[chan][center] = tmp;                         // wrap around

            float[] ar = getArray(valueType, chan, 0);   // get reference to datagram that will be copied into (m)Center
            if (ar.length != raw0Array[chan][center].length) {
               raw0Array[chan][center] = new float[ar.length];
            }
            System.arraycopy(ar, 0, raw0Array[chan][center], 0, ar.length);
         }
      }

      for (int chan = 0; chan < transducerCount; chan++) {
         for (int time = 1; time < size; time++) {
            raw0Array[chan][time + center] = getArray(valueType, chan, time);
            if (raw0Array[chan][time + center].length == 0) {
               Log.global.finer("missing ping at channel " + chan + " time " + (time + center));
            }
         }
      }
   }

   /**
    * Function is called at end of datagram stream. Subclasses should overload it
    * to do whatever they need to do at the end of a stream.
    *
    * @param ping the last ping
    */
   protected void doFinalOperations(Ping ping) {
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      while (outputQueue.isEmpty()) {
         if (getAsyncHandle().isCancelled()) {
            return null;
         }

         Ping ping = inputPing();

         if (ping == null) {
            emptyPingQueues();
            break;
         }

         boolean raw0Added = false;
         allPingQueue.addLast(ping);   // add ping to queue

         if (ping.getNonNullPowerData() != null) {
            raw0PingQueue.addLast(ping);   // add ping to raw0 queue
            raw0Added = true;
         }
         if (raw0PingQueue.size() <= center) {
            continue;  // buffer up <mid> raw0 pings before continuing
         }

         Ping testPing = allPingQueue.getFirst();
         while (testPing.getNonNullPowerData() == null) { // move all non-Raw0 in front of allPingQueue to outbuffer
            outputQueue.add(testPing);
            allPingQueue.removeFirst();
            testPing = allPingQueue.getFirst();
         }

         if (raw0Added) {
            bufferUpData();
            doVertical();                        // update mid ping

            outputQueue.add(raw0PingQueue.getFirst());

            allPingQueue.removeFirst();    //  remove mid ping from AllPingQueue
            raw0PingQueue.removeFirst();
         }
      }
      return outputQueue.poll();
   }

   /**
    * Called when there are no more new pings to process. Processes the remaining buffered
    * pings and cleans up.
    * In order to process the last input ping, we copy it to the time + i bufferelements,
    * so that the matrix is always full. These copied pings are not output.
    */
   private void emptyPingQueues() {
      while (!allPingQueue.isEmpty()) {
         Ping currentPing = allPingQueue.removeFirst();
         Ping outPing;

         //We need to process the remaining powerDatas in the queue. While
         //emptying the queue, we copy the current datagram to the end of the queue.
         if (currentPing.getNonNullPowerData() != null) {
            while (raw0PingQueue.size() <= center) { // necessary if the queue was not filled in the first place (small file)
               Ping extraPingForBuffer = new DefaultPing(currentPing.getPingConfiguration(), currentPing.getPingIndex(), currentPing.getBot0Datagram());

               List<Integer> channels = getCurrentChannelIndexArray();
               for (int i : channels) {
                  PowerData currentDatagram = raw0PingQueue.getFirst().getPowerData(i + 1);
                  if (currentDatagram != null) {
                     //Copy current ping to fill buffer when no further pings are
                     //available.
                     PowerData datagramDeepCopy = createCopyForExtraPing(currentDatagram);
                     extraPingForBuffer.add(datagramDeepCopy);
                  }
               }
               raw0PingQueue.addLast(extraPingForBuffer);
            }
            bufferUpData();
            doVertical();
            outPing = raw0PingQueue.removeFirst();
         } else {
            outPing = currentPing;
         }

         if (allPingQueue.isEmpty()) {
            doFinalOperations(outPing);
         }

         outputQueue.add(outPing);
      }
      raw0PingQueue.clear(); //Need to clear this since we added an extra datagram at the end.
   }

   private static PowerData createCopyForExtraPing(PowerData powerData) {
      return FillMissingDataModule.copy(powerData, powerData.getNTDate());
   }

   private void doVertical() {
      for (int channelIndex = firstChannelIndexToProcess; channelIndex < transducerCount; channelIndex++) {
         int sampleCount = getSampleCount(0, channelIndex);
         if (sampleCount == 0) {
            continue; //nothing to process
         }

         int beginSampleIndex;
         int endSampleIndex;

         if (module.automaticDepthRange.getBooleanValue()) {
            beginSampleIndex = 0;
            BaseDepDatagram depDatagram = raw0PingQueue.getFirst().getBaseDepDatagram(channelIndex + 1);
            if (depDatagram != null) {
               endSampleIndex = depthMeterToSampleIndex(depDatagram.getMinimumDepth(), channelIndex);
            } else {
               endSampleIndex = sampleCount;
            }
         } else {
            beginSampleIndex = depthMeterToSampleIndex(module.startDepth.getFloatValue(), channelIndex);
            endSampleIndex = depthMeterToSampleIndex(module.endDepth.getFloatValue(), channelIndex);
         }

         // if (getPixelHeave(f) > maxPixelHeave)
         maxPixelHeave = Math.max(maxPixelHeave, getPixelHeave(channelIndex));

         // todo: this module does not take heave into account regarding left and right neighbors
         // if neighboring pings have different heaves, their pixels will still be processed as if they are aligned
         // according to their index from 0 and upwards.

         // maxPixelHeave is added so processing goes deep enough to take into account pixels that are below endDepth
         // because of misalignment duo to the fact that heave is not taken into account.
         doPing(Math.clamp(beginSampleIndex + center, 0, sampleCount), Math.clamp(endSampleIndex - center + maxPixelHeave, 0, sampleCount), channelIndex);
         sync(channelIndex);
      }
   }

   /**
    * 'synchronize' all arrays for channelIndex in resultPing with new values.
    *
    * @param channelIndex current channel, starts at 0
    */
   private void sync(int channelIndex) {
      PowerData midRaw0 = getMidRaw0(channelIndex);
      switch (valueType) {
         case SV -> midRaw0.setSv(midRaw0.getSv());
         case LOG_SV -> midRaw0.setLogSv(midRaw0.getLogSv());
      }
   }

   private PowerData getMidRaw0(int channelIndex) {
      Ping ping = raw0PingQueue.getFirst();
      PowerData powerData = ping.getPowerData(channelIndex + 1);
      if (powerData == null) {
         throw new IllegalArgumentException("No data on channel " + (channelIndex + 1) + " in ping " + ping.getInstant());
      }
      return powerData;
   }
}
