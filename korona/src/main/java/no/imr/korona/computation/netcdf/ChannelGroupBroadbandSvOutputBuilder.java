package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.BroadbandSvByFrequency;
import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

final class ChannelGroupBroadbandSvOutputBuilder extends ChannelGroupOutputBuilder {
   private final FloatRange gridFrequencyRange;
   private final int gridFrequencyLength;
   private final float fftWindowSizeInPulseLengths;
   private final float deltaRange;
   private final int rangeLength;
   private final boolean writeAngles;
   private final RawFileTransducer transducer;
   private final PulseCompression pulseCompression;
   private final double[] calibrationFrequencies;

   ChannelGroupBroadbandSvOutputBuilder(Group.Builder parentGroup, int frequencyIndex, BroadbandData broadbandData,
                                        float deltaRange, float maxRange, boolean writeAngles,
                                        float fftWindowSizeInPulseLengths, float deltaFrequency) {
      super(parentGroup, frequencyIndex, broadbandData);

      gridFrequencyRange = broadbandData.getFrequencyRange().roundToMultipleOf(deltaFrequency);
      gridFrequencyLength = Math.round(gridFrequencyRange.getSize() / deltaFrequency) + 1;
      this.fftWindowSizeInPulseLengths = fftWindowSizeInPulseLengths;

      this.deltaRange = deltaRange;
      rangeLength = (int) Math.floor(maxRange / deltaRange);
      this.writeAngles = writeAngles;

      transducer = broadbandData.getTransducer();
      pulseCompression = broadbandData.getPulseCompression();

      Dimension pingTimeDim = NcWrite.addUnlimitedDimension(groupBuilder, Nc.PING_TIME);
      Dimension rangeDim = NcWrite.addDimension(groupBuilder, Nc.RANGE, rangeLength);
      Dimension broadbandFrequencyDim = NcWrite.addDimension(groupBuilder, Nc.BROADBAND_FREQUENCY, gridFrequencyLength);

      Dimension y_mf_auto_red_dim = NcWrite.addDimension(groupBuilder, Nc.Y_MF_AUTO_RED,
            pulseCompression.getAutoCorrelationTransmitSignal().length());

      NcWrite.addVariable(groupBuilder, Nc.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      NcWrite.addVariable(groupBuilder, Nc.RANGE, DataType.DOUBLE, List.of(rangeDim));

      NcWrite.addFloatVariable(groupBuilder, Nc.SAMPLE_INTERVAL, List.of(pingTimeDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.SOUND_SPEED, List.of(pingTimeDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.TRANSCEIVER_IMPEDANCE, List.of(pingTimeDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.TRANSMIT_FREQUENCY_START, List.of(pingTimeDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.TRANSMIT_FREQUENCY_STOP, List.of(pingTimeDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.TRANSMIT_POWER, List.of(pingTimeDim), List.of());

      NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_SENSITIVITY_ALONGSHIP, List.of(), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_SENSITIVITY_ATHWARTSHIP, List.of(), List.of());

      ChannelCalibration channelCalibration = transducer.getChannelCalibration();
      calibrationFrequencies = Stream.of(
                  channelCalibration.broadbandGain,
                  channelCalibration.broadbandTransducerImpedance,
                  channelCalibration.broadbandEquivalentBeamAngle,
                  channelCalibration.broadbandBeamWidthAlongship,
                  channelCalibration.broadbandBeamWidthAthwartship,
                  channelCalibration.broadbandAngleOffsetAlongship,
                  channelCalibration.broadbandAngleOffsetAthwartship
            )
            .flatMap(Optional::stream)
            .flatMapToDouble(f -> DoubleStream.of(f.getHz()))
            .distinct()
            .sorted()
            .toArray();
      if (calibrationFrequencies.length > 0) {
         Dimension calibrationFrequencyDim = NcWrite.addDimension(groupBuilder, Nc.CALIBRATION_FREQUENCY, calibrationFrequencies.length);
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_FREQUENCY, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_GAIN, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_TRANSDUCER_IMPEDANCE, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_EQUIVALENT_BEAM_ANGLE, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_BEAMWIDTH_ALONGSHIP, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_BEAMWIDTH_ATHWARTSHIP, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_ANGLE_OFFSET_ALONGSHIP, List.of(calibrationFrequencyDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.CALIBRATION_ANGLE_OFFSET_ATHWARTSHIP, List.of(calibrationFrequencyDim), List.of());
      }

      NcWrite.addFloatVariable(groupBuilder, Nc.Y_MF_AUTO_RED_RE, List.of(y_mf_auto_red_dim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.Y_MF_AUTO_RED_IM, List.of(y_mf_auto_red_dim), List.of());

      NcWrite.addFloatVariable(groupBuilder, Nc.BROADBAND_FREQUENCY, List.of(broadbandFrequencyDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.BROADBAND_SV, List.of(pingTimeDim, rangeDim, broadbandFrequencyDim), List.of());

      if (writeAngles) {
         NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_ALONGSHIP, List.of(pingTimeDim, rangeDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_ATHWARTSHIP, List.of(pingTimeDim, rangeDim), List.of());
      }
   }

   @Override
   ChannelGroupOutputWriter createWriter(NcChannelGroupWriter ncChannelGroupWriter) throws InvalidRangeException, IOException {
      return new ChannelGroupPulseCompressionWriter(ncChannelGroupWriter, this);
   }

   private static final class ChannelGroupPulseCompressionWriter extends ChannelGroupOutputWriter {
      private final long referenceTimeInMillis;
      private final float deltaRange;
      private final float rangeLength;
      private final float fftWindowSizeInPulseLengths;
      private final FloatRange gridFrequencyRange;
      private final int gridFrequencyLength;

      private final Variable pingTimeVar;

      private final Variable sampleIntervalVar;
      private final Variable soundSpeedVar;
      private final Variable transceiverImpedanceVar;
      private final Variable transmitFrequencyStartVar;
      private final Variable transmitFrequencyStopVar;
      private final Variable transmitPowerVar;

      private final Variable broadbandSvVar;

      private final @Nullable Variable angleAlongshipVar;
      private final @Nullable Variable angleAthwartshipVar;

      private final float[] angleAlongshipValues;
      private final float[] angleAthwartshipValues;

      private ChannelGroupPulseCompressionWriter(NcChannelGroupWriter ncChannelGroupWriter, ChannelGroupBroadbandSvOutputBuilder channelGroupBuilder) throws InvalidRangeException, IOException {
         super(ncChannelGroupWriter, channelGroupBuilder);

         referenceTimeInMillis = channelGroupBuilder.referenceTimeInMillis;
         deltaRange = channelGroupBuilder.deltaRange;
         rangeLength = channelGroupBuilder.rangeLength;
         fftWindowSizeInPulseLengths = channelGroupBuilder.fftWindowSizeInPulseLengths;
         gridFrequencyRange = channelGroupBuilder.gridFrequencyRange;
         gridFrequencyLength = channelGroupBuilder.gridFrequencyLength;

         pingTimeVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.PING_TIME);
         Variable rangeVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.RANGE);

         sampleIntervalVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.SAMPLE_INTERVAL);
         soundSpeedVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.SOUND_SPEED);
         transceiverImpedanceVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.TRANSCEIVER_IMPEDANCE);
         transmitFrequencyStartVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.TRANSMIT_FREQUENCY_START);
         transmitFrequencyStopVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.TRANSMIT_FREQUENCY_STOP);
         transmitPowerVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.TRANSMIT_POWER);

         NcWrite.writeScalarFloat(writer, channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.ANGLE_SENSITIVITY_ALONGSHIP),
               channelGroupBuilder.transducer.getAngleSensitivityAlongship());
         NcWrite.writeScalarFloat(writer, channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.ANGLE_SENSITIVITY_ATHWARTSHIP),
               channelGroupBuilder.transducer.getAngleSensitivityAthwartship());

         writer.write(channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.Y_MF_AUTO_RED_RE),
               Array.makeFromJavaArray(channelGroupBuilder.pulseCompression.getAutoCorrelationTransmitSignal().reArrayFloat()));
         writer.write(channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.Y_MF_AUTO_RED_IM),
               Array.makeFromJavaArray(channelGroupBuilder.pulseCompression.getAutoCorrelationTransmitSignal().imArrayFloat()));

         broadbandSvVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.BROADBAND_SV);

         angleAlongshipVar = channelGroupBuilder.writeAngles ? channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.ANGLE_ALONGSHIP) : null;
         angleAthwartshipVar = channelGroupBuilder.writeAngles ? channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.ANGLE_ATHWARTSHIP) : null;

         double[] ranges = new double[channelGroupBuilder.rangeLength];
         for (int i = 0; i < ranges.length; i++) {
            ranges[i] = i * channelGroupBuilder.deltaRange;
         }
         writer.write(rangeVar, new int[]{0}, Array.makeFromJavaArray(ranges));

         float[] broadbandFrequencies = new float[channelGroupBuilder.gridFrequencyLength];
         float deltaFrequency = gridFrequencyRange.getSize() / (channelGroupBuilder.gridFrequencyLength - 1);
         for (int i = 0; i < broadbandFrequencies.length; i++) {
            broadbandFrequencies[i] = gridFrequencyRange.min() + i * deltaFrequency;
         }
         Variable broadbandFrequencyVar = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.BROADBAND_FREQUENCY);
         writer.write(broadbandFrequencyVar, new int[]{0}, Array.makeFromJavaArray(broadbandFrequencies));

         if (channelGroupBuilder.calibrationFrequencies.length > 0) {
            writer.write(channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, Nc.CALIBRATION_FREQUENCY),
                  Array.makeFromJavaArray(Utils.toFloats(channelGroupBuilder.calibrationFrequencies)));
            ChannelCalibration channelCalibration = channelGroupBuilder.transducer.getChannelCalibration();
            writeCalibrationParameter(Nc.CALIBRATION_GAIN, channelCalibration.broadbandGain, channelGroupBuilder);
            writeCalibrationParameter(Nc.CALIBRATION_TRANSDUCER_IMPEDANCE, channelCalibration.broadbandTransducerImpedance, channelGroupBuilder);
            writeCalibrationParameter(Nc.CALIBRATION_EQUIVALENT_BEAM_ANGLE, channelCalibration.broadbandEquivalentBeamAngle, channelGroupBuilder);
            writeCalibrationParameter(Nc.CALIBRATION_BEAMWIDTH_ALONGSHIP, channelCalibration.broadbandBeamWidthAlongship, channelGroupBuilder);
            writeCalibrationParameter(Nc.CALIBRATION_BEAMWIDTH_ATHWARTSHIP, channelCalibration.broadbandBeamWidthAthwartship, channelGroupBuilder);
            writeCalibrationParameter(Nc.CALIBRATION_ANGLE_OFFSET_ALONGSHIP, channelCalibration.broadbandAngleOffsetAlongship, channelGroupBuilder);
            writeCalibrationParameter(Nc.CALIBRATION_ANGLE_OFFSET_ATHWARTSHIP, channelCalibration.broadbandAngleOffsetAthwartship, channelGroupBuilder);
         }

         angleAlongshipValues = new float[channelGroupBuilder.rangeLength];
         angleAthwartshipValues = new float[channelGroupBuilder.rangeLength];
      }

      private void writeCalibrationParameter(String variableName, Optional<BroadbandFunction> optionalBroadbandFunction,
                                             ChannelGroupBroadbandSvOutputBuilder channelGroupBuilder) throws InvalidRangeException, IOException {
         BroadbandFunction broadbandFunction = optionalBroadbandFunction.orElse(null);
         if (broadbandFunction == null) {
            return;
         }
         double minFrequency = broadbandFunction.getHz()[0];
         double maxFrequency = broadbandFunction.getHz()[broadbandFunction.getHz().length - 1];
         double[] calibrationFrequencies = channelGroupBuilder.calibrationFrequencies;
         float[] values = new float[calibrationFrequencies.length];
         for (int i = 0; i < calibrationFrequencies.length; i++) {
            double frequency = calibrationFrequencies[i];
            values[i] = frequency >= minFrequency && frequency <= maxFrequency
                  ? (float) broadbandFunction.getValue(frequency)
                  : Float.NaN;
         }
         Variable variable = channelGroupBuilder.findGroupVariable(ncChannelGroupWriter, variableName);
         writer.write(variable, Array.makeFromJavaArray(values));
      }

      @Override
      void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException {
         PingIndex pingIndex = ping.getPingIndex();

         ncChannelGroupWriter.writeLong(pingTimeVar, (pingIndex.getTimeInMillis() - referenceTimeInMillis) * 1_000_000);

         BroadbandData broadbandData = ping.getBroadbandData(channel);
         if (broadbandData == null) {
            return;
         }

         ncChannelGroupWriter.writeFloat(sampleIntervalVar, broadbandData.getSampleInterval());
         ncChannelGroupWriter.writeFloat(soundSpeedVar, broadbandData.getSoundVelocity());
         ncChannelGroupWriter.writeFloat(transmitFrequencyStartVar, broadbandData.getStartFrequency());
         ncChannelGroupWriter.writeFloat(transmitFrequencyStopVar, broadbandData.getEndFrequency());
         ncChannelGroupWriter.writeFloat(transmitPowerVar, broadbandData.getTransmitPower());
         RawFileTransducer.Xml0Info transducerXml0Info = broadbandData.getTransducer().getXml0Info();
         if (transducerXml0Info != null) {
            ncChannelGroupWriter.writeFloat(transceiverImpedanceVar, transducerXml0Info.getImpedance());
         }

         BroadbandSvByFrequency broadbandSvByFrequency = new BroadbandSvByFrequency(broadbandData);

         FloatRange frequencyRange = broadbandData.getFrequencyRange();
         float pulseLength = broadbandData.getSoundVelocity() * broadbandData.getPulseDuration();
         float fftWindowRadius = fftWindowSizeInPulseLengths * pulseLength / 2;

         for (int iRange = 0; iRange < rangeLength; iRange++) {
            float centerRange = (iRange + 0.5f) * deltaRange;
            float minRange = centerRange - fftWindowRadius;
            int iBegin = broadbandData.rangeToSampleIndex(minRange);
            if (iBegin < 0) {
               continue;
            }
            float maxRange = centerRange + fftWindowRadius;
            int iEnd = broadbandData.rangeToSampleIndex(maxRange);
            if (iEnd > broadbandData.getCount()) {
               break;
            }
            float[] sv = broadbandSvByFrequency.calculate(iBegin, iEnd, frequencyRange);
            OffsetValues resampledSv = OffsetValues.resample(sv, frequencyRange, gridFrequencyRange, gridFrequencyLength);
            if (resampledSv != null) {
               ArrayMath.divide(resampledSv.values, PowerData.IMR_CONSTANT);
               writer.write(broadbandSvVar, new int[]{pingTimeIndex, iRange, resampledSv.offset},
                     Array.makeFromJavaArray(new float[][][]{{resampledSv.values}}));
            }
         }
         if (angleAlongshipVar != null && angleAthwartshipVar != null) {
            AngleData angleData = broadbandData.getAngleData();
            if (angleData != null) {
               RawFileTransducer transducer = broadbandData.getTransducer();

               for (int iRange = 0; iRange < rangeLength; iRange++) {
                  float centerRange = (iRange + 0.5f) * deltaRange;
                  int i = broadbandData.rangeToContainingSampleIndex(centerRange);
                  if (i >= 0 && i < broadbandData.getCount()) {
                     angleAlongshipValues[iRange] = angleData.getMechanicalAlongAngle(i, transducer);
                     angleAthwartshipValues[iRange] = angleData.getMechanicalAthwartAngle(i, transducer);
                  } else {
                     angleAlongshipValues[iRange] = Float.NaN;
                     angleAthwartshipValues[iRange] = Float.NaN;
                  }
               }
               writer.write(angleAlongshipVar, new int[]{pingTimeIndex, 0},
                     Array.makeFromJavaArray(new float[][]{angleAlongshipValues}));

               writer.write(angleAthwartshipVar, new int[]{pingTimeIndex, 0},
                     Array.makeFromJavaArray(new float[][]{angleAthwartshipValues}));
            }
         }
      }
   }
}
