package no.imr.korona.computation;

import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;

import java.io.IOException;
import java.util.List;

/**
 * Module used for buffering up and manipulating pings.
 * <p>
 * This module is helpful for implementing matrix filters where one performs calculations on a source 'image' and writes the
 * result into a destination 'image'. The data that is returned by reading (getRawDataFromBuffer) will not be affected by updating data (setData),
 * although the data that is going out of the module will be affected.<br>
 * The module achieves this with a minimum of buffering;
 * Working with a buffer of 2*center+1 pings only requires buffering up (and delaying) center pings.
 * <p>
 * It offers easy manipulation of pings by overloading update() for a per-pixel callback from top to bottom of ping or doPing() for a
 * per-ping callback. If the GUI parameter {@code OnlyLast} is true then update() and doPing() will only be called for
 * the last channel, else they will be called for all channels before moving to the next ping.<br>
 * If the GUI parameter {@code OnlyLast} is true then reading and writing to other than the last channel is undefined.
 * <p>
 * One specifies size: {@code center} of buffer in constructor.
 * The module will then buffer up 2*center+1 pings and let the user set data in the mid ping through setData()<br>
 * It offers access to data by the functions getRawDataFromBuffer which can return a value from any ping in the buffer
 * and setData which will only write to the mid ping in the buffer.<br>
 * See the implementation of the modules in the filters package (no.imr.korona.computation.filters) for examples of how to use this module.<br>
 * The user must be aware of that data that is set by setData is not returned by getRawDataFromBuffer for the same ping,channel and depth.<br>
 * getRawDataFromBuffer will return the original unaltered data that entered the module, the data set in setData will be visible for the pings exiting the module, ie
 * in the echogram window and for the modules after this module.
 * <p>
 * If one only wants to work on single ping and needs no buffering, center can be set to 0.
 * <p>
 * Querying missing pings will return UNDEFINED value. If this is unwanted one can put FillMissingData module in front of this module so
 * that all pings have values.
 * <p>
 * Overview of datastructure:
 * <pre>
 * INDEX                                               |       0      |      1      | . . . |  center            |
 * raw0PingQueue                                       | current ping |     . . .   | . . . |  freshest raw ping |
 *
 * INDEX                                               |       0      |      1      | . . . |  center + ...      |
 * allPingQueue                                        | current ping |     . . .   | . . . |  freshest ping     |
 *
 * INDEX             |      0      | . . . | center-1  | center       | center + 1  | . . . |  2*center         |
 * raw0Array         | oldest ping | . . . |   . . .   | current ping |     . . .   | . . . |  freshest raw ping |
 *
 * INDEX                                               |       0      |
 * resultArray                                         |   mid ping   |
 * </pre>
 * <ul>
 * <li>raw0PingQueue stores the pings that are actually being buffered up by this module.
 * <li>allPingQueue stores all pings in raw0PingQueue, but also non-raw pings.
 * <li>raw0Array contains deepcopies of raw0PingQueue for index [0, center-1] and contains
 * quick references for values for index [center, 2*center]
 * <li>resultArray is a quick reference into raw0PingQueue.get(0) ping for updating data
 * </ul>
 * <p>
 * raw0Array has deepcopied the datavalues for index [0, center - 1], so if they are written to, it will not affect the
 * datagrams flowing through, only the local tables. getRawDataFromBuffer will read from raw0Array[center] array. Since copies are taken
 * only datagrams for index [center, 2*center] are buffered up in raw0PingQueue.
 * <p>
 * resultArray is a table referring into the datagram in the middle (center). Writing to this buffer (by setData) will write to the datagram that
 * is about to be sent on out of this module. getRawDataFromBuffer reads from a copy of the datagram, and setData writes to the actual datagram, thus
 * getRawDataFromBuffer will not be affected by setData.
 * <p>
 * Function that are special cases for setting data and are closely tied to the structure above are:<br>
 * setDataNow() sets data in raw0PingQueue freshest ping, this will be visible from getRawDataFromBuffer.<br>
 * setLocalBufferData() sets data in raw0Arrays center and will only write to the local
 * deepcopied buffer and will not affect datagrams going out of the module.
 */
public abstract class BaseMatrixModule extends GeneralPingModule {
   public final BooleanParameter logarithmicValues = new BooleanParameter(
         new Name("LogarithmicValues", "Logarithmic values"),
         true,
         "Use log(sv)(checked) or sv (not checked)");

   public final BooleanParameter showDetails = new BooleanParameter(
         new Name("ShowDetails", "Show details"),
         false,
         "Shows more detailed settings (Only last, Channels to process, Automatic depthRange, Start depth, End depth)");

   public final BooleanParameter onlyLast = new BooleanParameter(
         new Name("OnlyLast", "Only last"),
         false,
         "When true, only the last channel is processed; if false, all channels are processed");

   public final IntParameter channelsToProcess = new IntParameter(
         new Name("ChannelsToProcess", "Channels to process"),
         -1, Unit.COUNT,
         "Process the n last channels (-1 for all channels)");

   public final BooleanParameter automaticDepthRange = new BooleanParameter(
         new Name("AutomaticDepthRange", "Automatic depth range"),
         true,
         "If true and Bottom detection module is used, then the range goes to the detected seabed");

   public final FloatParameter startDepth = new FloatParameter(
         new Name("StartDepth", "Start depth"),
         10, Unit.METER,
         "Start depth in meters for the filter");

   public final FloatParameter endDepth = new FloatParameter(
         new Name("EndDepth", "End depth"),
         1000, Unit.METER,
         "End depth in meters for the filter");

   /**
    * Index of the middle ping in raw0Array.
    */
   private final int center;

   protected BaseMatrixModule(int bufferRadius) {
      this(bufferRadius, ValueType.LOG_SV, true);
   }

   protected BaseMatrixModule(int bufferRadius, ValueType defaultValueType, boolean canChangeValueType) {
      center = bufferRadius;

      logarithmicValues.setBooleanValue(defaultValueType == ValueType.LOG_SV);
      logarithmicValues.setEnabled(canChangeValueType);

      showDetails.addListenerAndNotify(show -> {
         onlyLast.setVisible(show);
         channelsToProcess.setVisible(show);
         automaticDepthRange.setVisible(show);
         startDepth.setVisible(show);
         endDepth.setVisible(show);
      });
      onlyLast.addListenerAndNotify(last -> {
         channelsToProcess.setEnabled(!last);
      });
      automaticDepthRange.addListenerAndNotify(auto -> {
         startDepth.setEnabled(!auto);
         endDepth.setEnabled(!auto);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            active,
            comment,
            //---
            logarithmicValues,
            showDetails,
            onlyLast,
            channelsToProcess,
            automaticDepthRange,
            startDepth,
            endDepth
      );
   }

   int getCenter() {
      return center;
   }

   @Override
   public abstract BaseMatrixModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException;

   /**
    * Enum of value types one can query from the datagrams.
    */
   public enum ValueType {
      LOG_SV {
         @Override
         public float getValueFromLogSv(float logSv) {
            return logSv;
         }

         @Override
         public float[] getArray(PowerData powerData) {
            return powerData.getLogSv();
         }
      },
      SV {
         @Override
         public float getValueFromLogSv(float logSv) {
            return PowerData.logSvToSv(logSv);
         }

         @Override
         public float[] getArray(PowerData powerData) {
            return powerData.getSv();
         }
      };

      public abstract float getValueFromLogSv(float logSv);

      public abstract float[] getArray(PowerData powerData);
   }
}
