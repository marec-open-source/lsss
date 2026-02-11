package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.computation.broadband.PulseCompressionConfig;
import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.netcdf.NcWrite;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class ChannelGroupPulseCompressionOutputBuilder extends ChannelGroupOutputBuilder {
   private final int sectorLength;
   private final float minRange;
   private final float sampleDistance;
   private final int rangeLength;
   private final boolean writeAngles;
   private final RawFileTransducer transducer;
   private final PulseCompression pulseCompression;
   private final double[] calibrationFrequencies;

   ChannelGroupPulseCompressionOutputBuilder(Group.Builder parentGroup, int frequencyIndex, BroadbandData broadbandData,
                                             float maxRange, boolean writeAngles) {
      super(parentGroup, frequencyIndex, broadbandData);

      sectorLength = broadbandData.getSectorCount();
      minRange = broadbandData.getMinRange();
      sampleDistance = broadbandData.getSampleDistance();
      rangeLength = (int) Math.floor(maxRange / sampleDistance);
      this.writeAngles = writeAngles;

      transducer = broadbandData.getTransducer();
      pulseCompression = broadbandData.getPulseCompression();

      Dimension pingTimeDim = NcWrite.addUnlimitedDimension(groupBuilder, Nc.PING_TIME);
      Dimension rangeDim = NcWrite.addDimension(groupBuilder, Nc.RANGE, rangeLength);
      Dimension sectorDim = NcWrite.addDimension(groupBuilder, Nc.SECTOR, sectorLength);
      Dimension y_mf_auto_red_dim = NcWrite.addDimension(groupBuilder, Nc.Y_MF_AUTO_RED,
            pulseCompression.getAutoCorrelationTransmitSignal().length());

      NcWrite.addVariable(groupBuilder, Nc.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      NcWrite.addVariable(groupBuilder, Nc.SECTOR, DataType.INT, List.of(sectorDim));
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

      NcWrite.addFloatVariable(groupBuilder, Nc.PULSE_COMPRESSED_RE, List.of(pingTimeDim, sectorDim, rangeDim), List.of());
      NcWrite.addFloatVariable(groupBuilder, Nc.PULSE_COMPRESSED_IM, List.of(pingTimeDim, sectorDim, rangeDim), List.of());

      if (writeAngles) {
         NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_ALONGSHIP, List.of(pingTimeDim, rangeDim), List.of());
         NcWrite.addFloatVariable(groupBuilder, Nc.ANGLE_ATHWARTSHIP, List.of(pingTimeDim, rangeDim), List.of());
      }
   }

   @Override
   ChannelGroupOutputWriter createWriter(NcGridWriter ncGridWriter) throws InvalidRangeException, IOException {
      return new ChannelGroupPulseCompressionWriter(ncGridWriter, this);
   }

   private static final class ChannelGroupPulseCompressionWriter extends ChannelGroupOutputWriter {
      private final long referenceTimeInMillis;

      private final Variable pingTimeVar;

      private final Variable sampleIntervalVar;
      private final Variable soundSpeedVar;
      private final Variable transceiverImpedanceVar;
      private final Variable transmitFrequencyStartVar;
      private final Variable transmitFrequencyStopVar;
      private final Variable transmitPowerVar;

      private final PulseCompressionConfig pulseCompressionConfig;

      private final Variable pulseCompressedReVar;
      private final Variable pulseCompressedImVar;

      private final @Nullable Variable angleAlongshipVar;
      private final @Nullable Variable angleAthwartshipVar;

      private final float[] floatValues;

      private ChannelGroupPulseCompressionWriter(NcGridWriter ncGridWriter, ChannelGroupPulseCompressionOutputBuilder channelGroupBuilder) throws InvalidRangeException, IOException {
         super(ncGridWriter, channelGroupBuilder);

         referenceTimeInMillis = channelGroupBuilder.referenceTimeInMillis;

         pingTimeVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.PING_TIME);
         Variable sectorVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.SECTOR);
         Variable rangeVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.RANGE);

         sampleIntervalVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.SAMPLE_INTERVAL);
         soundSpeedVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.SOUND_SPEED);
         transceiverImpedanceVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.TRANSCEIVER_IMPEDANCE);
         transmitFrequencyStartVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.TRANSMIT_FREQUENCY_START);
         transmitFrequencyStopVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.TRANSMIT_FREQUENCY_STOP);
         transmitPowerVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.TRANSMIT_POWER);

         NcWrite.writeScalarFloat(writer, channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.ANGLE_SENSITIVITY_ALONGSHIP),
               channelGroupBuilder.transducer.getAngleSensitivityAlongship());
         NcWrite.writeScalarFloat(writer, channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.ANGLE_SENSITIVITY_ATHWARTSHIP),
               channelGroupBuilder.transducer.getAngleSensitivityAthwartship());

         pulseCompressionConfig = channelGroupBuilder.pulseCompression.getConfig();
         writer.write(channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.Y_MF_AUTO_RED_RE),
               Array.makeFromJavaArray(channelGroupBuilder.pulseCompression.getAutoCorrelationTransmitSignal().reArrayFloat()));
         writer.write(channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.Y_MF_AUTO_RED_IM),
               Array.makeFromJavaArray(channelGroupBuilder.pulseCompression.getAutoCorrelationTransmitSignal().imArrayFloat()));

         pulseCompressedReVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.PULSE_COMPRESSED_RE);
         pulseCompressedImVar = channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.PULSE_COMPRESSED_IM);

         angleAlongshipVar = channelGroupBuilder.writeAngles ? channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.ANGLE_ALONGSHIP) : null;
         angleAthwartshipVar = channelGroupBuilder.writeAngles ? channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.ANGLE_ATHWARTSHIP) : null;

         int[] sectors = IntStream.rangeClosed(1, channelGroupBuilder.sectorLength).toArray();
         writer.write(sectorVar, new int[]{0}, Array.makeFromJavaArray(sectors));

         double[] ranges = new double[channelGroupBuilder.rangeLength];
         for (int i = 0; i < ranges.length; i++) {
            ranges[i] = channelGroupBuilder.minRange + i * channelGroupBuilder.sampleDistance;
         }
         writer.write(rangeVar, new int[]{0}, Array.makeFromJavaArray(ranges));

         if (channelGroupBuilder.calibrationFrequencies.length > 0) {
            writer.write(channelGroupBuilder.findGroupVariable(ncGridWriter, Nc.CALIBRATION_FREQUENCY),
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

         floatValues = new float[channelGroupBuilder.rangeLength];
      }

      private void writeCalibrationParameter(String variableName, Optional<BroadbandFunction> optionalBroadbandFunction,
                                             ChannelGroupPulseCompressionOutputBuilder channelGroupBuilder) throws InvalidRangeException, IOException {
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
         Variable variable = channelGroupBuilder.findGroupVariable(ncGridWriter, variableName);
         writer.write(variable, Array.makeFromJavaArray(values));
      }

      @Override
      void write(Ping ping, int pingTimeIndex) throws InvalidRangeException, IOException {
         PingIndex pingIndex = ping.getPingIndex();

         ncGridWriter.writeLong(pingTimeVar, (pingIndex.getTimeInMillis() - referenceTimeInMillis) * 1_000_000);

         BroadbandData broadbandData = ping.getBroadbandData(channel);
         if (broadbandData == null) {
            return;
         }

         if (!pulseCompressionConfig.equals(broadbandData.getPulseCompression().getConfig())) {
            throw new IOException("Pulse compression config changed for channel " + broadbandData.getTransducer().getChannelId()
                  + " at " + ping.getInstant());
         }

         ncGridWriter.writeFloat(sampleIntervalVar, broadbandData.getSampleInterval());
         ncGridWriter.writeFloat(soundSpeedVar, broadbandData.getSoundVelocity());
         ncGridWriter.writeFloat(transmitFrequencyStartVar, broadbandData.getStartFrequency());
         ncGridWriter.writeFloat(transmitFrequencyStopVar, broadbandData.getEndFrequency());
         ncGridWriter.writeFloat(transmitPowerVar, broadbandData.getTransmitPower());
         RawFileTransducer.Xml0Info transducerXml0Info = broadbandData.getTransducer().getXml0Info();
         if (transducerXml0Info != null) {
            ncGridWriter.writeFloat(transceiverImpedanceVar, transducerXml0Info.getImpedance());
         }

         int n = Math.min(floatValues.length, broadbandData.getCount());
         if (n < floatValues.length) {
            Arrays.fill(floatValues, n, floatValues.length, Float.NaN);
         }

         for (int sectorIndex = 0; sectorIndex < broadbandData.getSectorCount(); sectorIndex++) {
            ComplexArray sectorData = ComplexArray.of(broadbandData.getReal()[sectorIndex], broadbandData.getImag()[sectorIndex]);
            ComplexArray pc = broadbandData.getPulseCompression().computePulseCompressedSignal(sectorData);

            for (int i = 0; i < n; i++) {
               floatValues[i] = (float) pc.re(i);
            }
            writer.write(pulseCompressedReVar, new int[]{pingTimeIndex, sectorIndex, 0},
                  Array.makeFromJavaArray(new float[][][]{{floatValues}}));

            for (int i = 0; i < n; i++) {
               floatValues[i] = (float) pc.im(i);
            }
            writer.write(pulseCompressedImVar, new int[]{pingTimeIndex, sectorIndex, 0},
                  Array.makeFromJavaArray(new float[][][]{{floatValues}}));
         }

         if (angleAlongshipVar != null && angleAthwartshipVar != null) {
            AngleData angleData = broadbandData.getAngleData();
            if (angleData != null) {
               RawFileTransducer transducer = broadbandData.getTransducer();

               for (int i = 0; i < n; i++) {
                  floatValues[i] = angleData.getMechanicalAlongAngle(i, transducer);
               }
               writer.write(angleAlongshipVar, new int[]{pingTimeIndex, 0},
                     Array.makeFromJavaArray(new float[][]{floatValues}));

               for (int i = 0; i < n; i++) {
                  floatValues[i] = angleData.getMechanicalAthwartAngle(i, transducer);
               }
               writer.write(angleAthwartshipVar, new int[]{pingTimeIndex, 0},
                     Array.makeFromJavaArray(new float[][]{floatValues}));
            }
         }
      }
   }
}
