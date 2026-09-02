package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.ma2.Array;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

final class BroadbandInfoOutput implements PingByPingOutput {
   private final int channel;
   private final RawFileTransducer transducer;
   private final PulseCompression pulseCompression;
   private final ChannelCalibration channelCalibration;
   private final double[] calibrationFrequencies;

   BroadbandInfoOutput(BroadbandData broadbandData) {
      channel = broadbandData.getChannel();
      transducer = broadbandData.getTransducer();
      pulseCompression = broadbandData.getPulseCompression();
      channelCalibration = transducer.getChannelCalibration();
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
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig) {
      Dimension pingTimeDim = NcBuild.addUnlimitedDimension(groupBuilder, Nc.PING_TIME);
      groupBuilder.addVariable(NcBuild.timeVariable(Nc.PING_TIME, List.of(pingTimeDim), ncConfig.referenceTime));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.SAMPLE_INTERVAL, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.SOUND_SPEED, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSCEIVER_IMPEDANCE, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSMIT_FREQUENCY_START, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSMIT_FREQUENCY_STOP, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSMIT_POWER, List.of(pingTimeDim)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_SENSITIVITY_ALONGSHIP, List.of()));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_SENSITIVITY_ATHWARTSHIP, List.of()));

      if (calibrationFrequencies.length > 0) {
         Dimension calibrationFrequencyDim = NcBuild.addDimension(groupBuilder, Nc.CALIBRATION_FREQUENCY, calibrationFrequencies.length);
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_FREQUENCY, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_GAIN, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_TRANSDUCER_IMPEDANCE, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_EQUIVALENT_BEAM_ANGLE, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_BEAMWIDTH_ALONGSHIP, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_BEAMWIDTH_ATHWARTSHIP, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_ANGLE_OFFSET_ALONGSHIP, List.of(calibrationFrequencyDim)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.CALIBRATION_ANGLE_OFFSET_ATHWARTSHIP, List.of(calibrationFrequencyDim)));
      }

      Dimension y_mf_auto_red_dim = NcBuild.addDimension(groupBuilder, Nc.Y_MF_AUTO_RED,
            pulseCompression.getAutoCorrelationTransmitSignal().length());
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.Y_MF_AUTO_RED_RE, List.of(y_mf_auto_red_dim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.Y_MF_AUTO_RED_IM, List.of(y_mf_auto_red_dim)));


      return (writer, group) -> {
         writeUpFront(writer, group);
         return createPingByPingWriter(writer, group, ncConfig);
      };
   }

   private void writeUpFront(NetcdfFormatWriter writer, Group group) throws InvalidRangeException, IOException {
      NcWrite.floatD0(writer, NetcdfUtils.findVariable(group, Nc.ANGLE_SENSITIVITY_ALONGSHIP),
            transducer.getAngleSensitivityAlongship());
      NcWrite.floatD0(writer, NetcdfUtils.findVariable(group, Nc.ANGLE_SENSITIVITY_ATHWARTSHIP),
            transducer.getAngleSensitivityAthwartship());

      writer.write(NetcdfUtils.findVariable(group, Nc.Y_MF_AUTO_RED_RE),
            Array.makeFromJavaArray(pulseCompression.getAutoCorrelationTransmitSignal().reArrayFloat()));
      writer.write(NetcdfUtils.findVariable(group, Nc.Y_MF_AUTO_RED_IM),
            Array.makeFromJavaArray(pulseCompression.getAutoCorrelationTransmitSignal().imArrayFloat()));

      if (calibrationFrequencies.length > 0) {
         writer.write(NetcdfUtils.findVariable(group, Nc.CALIBRATION_FREQUENCY),
               Array.makeFromJavaArray(Utils.toFloats(calibrationFrequencies)));
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_GAIN, channelCalibration.broadbandGain);
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_TRANSDUCER_IMPEDANCE, channelCalibration.broadbandTransducerImpedance);
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_EQUIVALENT_BEAM_ANGLE, channelCalibration.broadbandEquivalentBeamAngle);
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_BEAMWIDTH_ALONGSHIP, channelCalibration.broadbandBeamWidthAlongship);
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_BEAMWIDTH_ATHWARTSHIP, channelCalibration.broadbandBeamWidthAthwartship);
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_ANGLE_OFFSET_ALONGSHIP, channelCalibration.broadbandAngleOffsetAlongship);
         writeCalibrationParameter(writer, group, Nc.CALIBRATION_ANGLE_OFFSET_ATHWARTSHIP, channelCalibration.broadbandAngleOffsetAthwartship);
      }
   }

   private PingByPingWriter createPingByPingWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig) throws IOException {
      Variable pingTimeVar = NetcdfUtils.findVariable(group, Nc.PING_TIME);
      Variable sampleIntervalVar = NetcdfUtils.findVariable(group, Nc.SAMPLE_INTERVAL);
      Variable soundSpeedVar = NetcdfUtils.findVariable(group, Nc.SOUND_SPEED);
      Variable transceiverImpedanceVar = NetcdfUtils.findVariable(group, Nc.TRANSCEIVER_IMPEDANCE);
      Variable transmitFrequencyStartVar = NetcdfUtils.findVariable(group, Nc.TRANSMIT_FREQUENCY_START);
      Variable transmitFrequencyStopVar = NetcdfUtils.findVariable(group, Nc.TRANSMIT_FREQUENCY_STOP);
      Variable transmitPowerVar = NetcdfUtils.findVariable(group, Nc.TRANSMIT_POWER);

      return (ping, pingTimeIndex) -> {
         NcWrite.longD1(writer, pingTimeVar, pingTimeIndex, ncConfig.referenceTime.until(ping.getInstant(), ChronoUnit.NANOS));

         BroadbandData broadbandData = ping.getBroadbandData(channel);
         if (broadbandData == null) {
            return;
         }

         NcWrite.floatD1(writer, sampleIntervalVar, pingTimeIndex, broadbandData.getSampleInterval());
         NcWrite.floatD1(writer, soundSpeedVar, pingTimeIndex, broadbandData.getSoundVelocity());
         NcWrite.floatD1(writer, transmitFrequencyStartVar, pingTimeIndex, broadbandData.getStartFrequency());
         NcWrite.floatD1(writer, transmitFrequencyStopVar, pingTimeIndex, broadbandData.getEndFrequency());
         NcWrite.floatD1(writer, transmitPowerVar, pingTimeIndex, broadbandData.getTransmitPower());
         RawFileTransducer.Xml0Info transducerXml0Info = broadbandData.getTransducer().getXml0Info();
         if (transducerXml0Info != null) {
            NcWrite.floatD1(writer, transceiverImpedanceVar, pingTimeIndex, transducerXml0Info.getImpedance());
         }
      };
   }

   private void writeCalibrationParameter(NetcdfFormatWriter writer, Group group, String variableName,
                                          Optional<BroadbandFunction> optionalBroadbandFunction) throws InvalidRangeException, IOException {
      BroadbandFunction broadbandFunction = optionalBroadbandFunction.orElse(null);
      if (broadbandFunction == null) {
         return;
      }
      double minFrequency = broadbandFunction.getHz()[0];
      double maxFrequency = broadbandFunction.getHz()[broadbandFunction.getHz().length - 1];
      float[] values = new float[calibrationFrequencies.length];
      for (int i = 0; i < calibrationFrequencies.length; i++) {
         double frequency = calibrationFrequencies[i];
         values[i] = frequency >= minFrequency && frequency <= maxFrequency
               ? (float) broadbandFunction.getValue(frequency)
               : Float.NaN;
      }
      Variable variable = NetcdfUtils.findVariable(group, variableName);
      writer.write(variable, Array.makeFromJavaArray(values));
   }
}
