package no.imr.korona.computation.netcdf;

import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.netcdf.NcBuild;
import no.imr.tools.netcdf.NcWrite;
import no.imr.tools.netcdf.NetcdfUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class BasicInfoOutput implements PingByPingOutput {
   private final @Nullable TransducerParameterManager horizontalTransducerParameterManager;
   private final @Nullable TransducerParameterManager verticalTransducerParameterManager;

   BasicInfoOutput(@Nullable TransducerParameterManager horizontalTransducerParameterManager,
                   @Nullable TransducerParameterManager verticalTransducerParameterManager) {
      this.horizontalTransducerParameterManager = horizontalTransducerParameterManager;
      this.verticalTransducerParameterManager = verticalTransducerParameterManager;
   }

   @Override
   public PingByPingBuilder createBuilder(Group.Builder groupBuilder, NcConfig ncConfig) {
      Dimension frequencyDim = NcBuild.addDimension(groupBuilder, Nc.FREQUENCY, ncConfig.channelCount);
      Dimension pingTimeDim = NcBuild.addUnlimitedDimension(groupBuilder, Nc.PING_TIME);

      groupBuilder.addVariable(NcBuild.doubleVariable(Nc.FREQUENCY, List.of(frequencyDim)));
      groupBuilder.addVariable(NcBuild.timeVariable(Nc.PING_TIME, List.of(pingTimeDim), ncConfig.referenceTime));

      groupBuilder.addVariable(NcBuild.newVariable(Nc.CHANNEL_ID, DataType.STRING, List.of(frequencyDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_SENSITIVITY_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_SENSITIVITY_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_OFFSET_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ANGLE_OFFSET_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.BEAMWIDTH_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.BEAMWIDTH_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.newVariable(Nc.BEAM_TYPE, DataType.UBYTE, List.of(frequencyDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.EQUIVALENT_BEAM_ANGLE, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));

      Dimension rawFileDim = NcBuild.addUnlimitedDimension(groupBuilder, "raw_file_dim");
      groupBuilder.addVariable(NcBuild.newVariable(Nc.RAW_FILE, DataType.CHAR, List.of(pingTimeDim, rawFileDim)));

      groupBuilder.addVariable(NcBuild.doubleVariable(Nc.LONGITUDE, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.doubleVariable(Nc.LATITUDE, List.of(pingTimeDim)));

      groupBuilder.addVariable(NcBuild.newVariable(Nc.PING_NUMBER, DataType.UINT, List.of(pingTimeDim)));
      groupBuilder.addVariable(NcBuild.doubleVariable(Nc.DISTANCE, List.of(pingTimeDim)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.BOTTOM_DEPTH, List.of(frequencyDim, pingTimeDim)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.HEAVE, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.HEADING, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.PITCH, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ROLL, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.SOUND_SPEED, List.of(pingTimeDim)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.PULSE_LENGTH, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.SA_CORRECTION, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.GAIN, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));

      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSDUCER_DRAFT, List.of(frequencyDim, pingTimeDim), List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSMIT_BANDWIDTH, List.of(frequencyDim, pingTimeDim)));
      groupBuilder.addVariable(NcBuild.newVariable(Nc.TRANSMIT_MODE, DataType.SHORT, List.of(frequencyDim, pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSMIT_POWER, List.of(frequencyDim, pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.ABSORPTION, List.of(frequencyDim, pingTimeDim)));
      groupBuilder.addVariable(NcBuild.floatVariable(Nc.RECEIVE_DURATION_EFFECTIVE, List.of(frequencyDim, pingTimeDim)));

      if (horizontalTransducerParameterManager != null) {
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSDUCER_OFFSET_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSDUCER_OFFSET_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      }
      if (verticalTransducerParameterManager != null) {
         groupBuilder.addVariable(NcBuild.floatVariable(Nc.TRANSDUCER_OFFSET_VERTICAL, List.of(frequencyDim), List.of(Nc.CHANNEL_ID)));
      }

      return (writer, group) -> {
         writeUpFront(writer, group, ncConfig);
         return createPingByPingWriter(writer, group, ncConfig);
      };
   }

   private void writeUpFront(NetcdfFormatWriter writer, Group group, NcConfig ncConfig) throws IOException, InvalidRangeException {
      List<RawFileTransducer> transducers = ncConfig.pingConfiguration.getRawFileConfiguration().getTransducers();
      int channelCount = ncConfig.channelCount;

      if (horizontalTransducerParameterManager != null) {
         float[] transducerOffsetAlongship = new float[channelCount];
         float[] transducerOffsetAthwartship = new float[channelCount];
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            int kHz = transducers.get(channelIndex).getKHz();
            TransducerParameters par = horizontalTransducerParameterManager.getTransducerOffsetPar(kHz).orElse(null);
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
            transducerOffsetAlongship[frequencyIndex] = par != null ? par.alongCorrection.getFloatValue() : Float.NaN;
            transducerOffsetAthwartship[frequencyIndex] = par != null ? par.athwartCorrection.getFloatValue() : Float.NaN;
         }
         writer.write(NetcdfUtils.findVariable(group, Nc.TRANSDUCER_OFFSET_ALONGSHIP), Array.makeFromJavaArray(transducerOffsetAlongship));
         writer.write(NetcdfUtils.findVariable(group, Nc.TRANSDUCER_OFFSET_ATHWARTSHIP), Array.makeFromJavaArray(transducerOffsetAthwartship));
      }
      if (verticalTransducerParameterManager != null) {
         float[] transducerOffsetVertical = new float[channelCount];
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            int kHz = transducers.get(channelIndex).getKHz();
            TransducerParameters par = verticalTransducerParameterManager.getTransducerOffsetPar(kHz).orElse(null);
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
            transducerOffsetVertical[frequencyIndex] = par != null
                  ? par.getDeltaZ0() + par.getDeltaZPulseDelay()
                  : Float.NaN;
         }
         writer.write(NetcdfUtils.findVariable(group, Nc.TRANSDUCER_OFFSET_VERTICAL), Array.makeFromJavaArray(transducerOffsetVertical));
      }

      double[] frequencies = new double[channelCount];
      String[] channelIds = new String[channelCount];
      ArrayFloat.D1 angleSensitivityAlongship = new ArrayFloat.D1(channelCount);
      ArrayFloat.D1 angleSensitivityAthwartship = new ArrayFloat.D1(channelCount);
      ArrayFloat.D1 angleOffsetAlongship = new ArrayFloat.D1(channelCount);
      ArrayFloat.D1 angleOffsetAthwartship = new ArrayFloat.D1(channelCount);
      ArrayFloat.D1 beamwidthAlongship = new ArrayFloat.D1(channelCount);
      ArrayFloat.D1 beamwidthAthwartship = new ArrayFloat.D1(channelCount);
      ArrayByte.D1 beamType = new ArrayByte.D1(channelCount, true);
      ArrayFloat.D1 equivalentBeamAngle = new ArrayFloat.D1(channelCount);
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         RawFileTransducer transducer = transducers.get(channelIndex);
         int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
         frequencies[frequencyIndex] = transducer.getFrequency();
         channelIds[frequencyIndex] = transducer.getChannelId();
         angleSensitivityAlongship.set(frequencyIndex, transducer.getAngleSensitivityAlongship());
         angleSensitivityAthwartship.set(frequencyIndex, transducer.getAngleSensitivityAthwartship());
         angleOffsetAlongship.set(frequencyIndex, transducer.getAngleOffsetAlongship());
         angleOffsetAthwartship.set(frequencyIndex, transducer.getAngleOffsetAthwartship());
         beamwidthAlongship.set(frequencyIndex, transducer.getBeamWidthAlongship());
         beamwidthAthwartship.set(frequencyIndex, transducer.getBeamWidthAthwartship());
         beamType.set(frequencyIndex, (byte) transducer.getBeamType());
         equivalentBeamAngle.set(frequencyIndex, transducer.getEquivalentBeamAngle());
      }
      writer.write(NetcdfUtils.findVariable(group, Nc.FREQUENCY), Array.makeFromJavaArray(frequencies));
      writer.write(NetcdfUtils.findVariable(group, Nc.CHANNEL_ID), Array.factory(DataType.STRING, new int[]{channelCount}, channelIds));
      writer.write(NetcdfUtils.findVariable(group, Nc.ANGLE_SENSITIVITY_ALONGSHIP), angleSensitivityAlongship);
      writer.write(NetcdfUtils.findVariable(group, Nc.ANGLE_SENSITIVITY_ATHWARTSHIP), angleSensitivityAthwartship);
      writer.write(NetcdfUtils.findVariable(group, Nc.ANGLE_OFFSET_ALONGSHIP), angleOffsetAlongship);
      writer.write(NetcdfUtils.findVariable(group, Nc.ANGLE_OFFSET_ATHWARTSHIP), angleOffsetAthwartship);
      writer.write(NetcdfUtils.findVariable(group, Nc.BEAMWIDTH_ALONGSHIP), beamwidthAlongship);
      writer.write(NetcdfUtils.findVariable(group, Nc.BEAMWIDTH_ATHWARTSHIP), beamwidthAthwartship);
      writer.write(NetcdfUtils.findVariable(group, Nc.BEAM_TYPE), beamType);
      writer.write(NetcdfUtils.findVariable(group, Nc.EQUIVALENT_BEAM_ANGLE), equivalentBeamAngle);
   }

   private static PingByPingWriter createPingByPingWriter(NetcdfFormatWriter writer, Group group, NcConfig ncConfig) throws IOException {
      RawFileConfiguration rawFileConfiguration = ncConfig.pingConfiguration.getRawFileConfiguration();
      int channelCount = ncConfig.channelCount;

      String fileName = rawFileConfiguration.getDataFile().getFileName().toString();
      ArrayChar.D2 rawFileArray = (ArrayChar.D2) Array.makeFromJavaArray(new char[][]{fileName.toCharArray()});

      AtomicInteger missingPulseDurationValues = new AtomicInteger(channelCount);
      boolean[] hasWrittenPulseDuration = new boolean[channelCount];

      Variable pingTimeVar = NetcdfUtils.findVariable(group, Nc.PING_TIME);

      Variable rawFileVar = NetcdfUtils.findVariable(group, Nc.RAW_FILE);

      Variable longitudeVar = NetcdfUtils.findVariable(group, Nc.LONGITUDE);
      Variable latitudeVar = NetcdfUtils.findVariable(group, Nc.LATITUDE);

      Variable pingNumberVar = NetcdfUtils.findVariable(group, Nc.PING_NUMBER);
      Variable distanceVar = NetcdfUtils.findVariable(group, Nc.DISTANCE);

      Variable bottomDepthVar = NetcdfUtils.findVariable(group, Nc.BOTTOM_DEPTH);

      Variable heaveVar = NetcdfUtils.findVariable(group, Nc.HEAVE);
      Variable headingVar = NetcdfUtils.findVariable(group, Nc.HEADING);
      Variable pitchVar = NetcdfUtils.findVariable(group, Nc.PITCH);
      Variable rollVar = NetcdfUtils.findVariable(group, Nc.ROLL);

      Variable soundSpeedVar = NetcdfUtils.findVariable(group, Nc.SOUND_SPEED);

      Variable pulseLengthVar = NetcdfUtils.findVariable(group, Nc.PULSE_LENGTH);
      Variable saCorrectionVar = NetcdfUtils.findVariable(group, Nc.SA_CORRECTION);
      Variable gainVar = NetcdfUtils.findVariable(group, Nc.GAIN);
      Variable transducerDraftVar = NetcdfUtils.findVariable(group, Nc.TRANSDUCER_DRAFT);
      Variable transmitBandwidthVar = NetcdfUtils.findVariable(group, Nc.TRANSMIT_BANDWIDTH);
      Variable transmitModeVar = NetcdfUtils.findVariable(group, Nc.TRANSMIT_MODE);
      Variable transmitPowerVar = NetcdfUtils.findVariable(group, Nc.TRANSMIT_POWER);
      Variable absorptionVar = NetcdfUtils.findVariable(group, Nc.ABSORPTION);
      Variable receiveDurationEffectiveVar = NetcdfUtils.findVariable(group, Nc.RECEIVE_DURATION_EFFECTIVE);

      return (ping, pingTimeIndex) -> {
         if (missingPulseDurationValues.get() > 0) {
            for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
               ChannelData channelData = ping.getChannelData(channelIndex + 1);
               if (channelData != null && !hasWrittenPulseDuration[channelIndex]) {
                  int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
                  float pulseDuration = channelData.getPulseDuration();
                  NcWrite.floatD1(writer, pulseLengthVar, frequencyIndex, pulseDuration);
                  NcWrite.floatD1(writer, saCorrectionVar, frequencyIndex, channelData.getTransducer().getSaCorrection(pulseDuration));
                  NcWrite.floatD1(writer, gainVar, frequencyIndex, channelData.getTransducer().getGainForPulseDuration(pulseDuration));
                  hasWrittenPulseDuration[channelIndex] = true;
                  missingPulseDurationValues.decrementAndGet();
               }
            }
         }

         PingIndex pingIndex = ping.getPingIndex();

         NcWrite.longD1(writer, pingTimeVar, pingTimeIndex, ncConfig.referenceTime.until(pingIndex.getInstant(), ChronoUnit.NANOS));

         GeoPoint geoPos = pingIndex.getGeographicalPosition();
         if (geoPos != null) {
            NcWrite.doubleD1(writer, latitudeVar, pingTimeIndex, geoPos.getLatitude());
            NcWrite.doubleD1(writer, longitudeVar, pingTimeIndex, geoPos.getLongitude());
         }

         NcWrite.uintD1(writer, pingNumberVar, pingTimeIndex, (int) pingIndex.getPingNumber());
         NcWrite.doubleD1(writer, distanceVar, pingTimeIndex, pingIndex.getVesselDistance());

         ArrayFloat.D2 bottomDepthArray = new ArrayFloat.D2(channelCount, 1);
         double[] bot0Depths = ping.getBot0Datagram().getChannelDepths();
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
            bottomDepthArray.set(frequencyIndex, 0, (float) bot0Depths[channelIndex]);
         }
         writer.write(bottomDepthVar, new int[]{0, pingTimeIndex}, bottomDepthArray);

         ChannelData anyChannelData = ping.getFirstAvailableChannelData();
         if (anyChannelData != null) {
            NcWrite.floatD1(writer, heaveVar, pingTimeIndex, anyChannelData.getHeave());
            NcWrite.floatD1(writer, headingVar, pingTimeIndex, anyChannelData.getHeading());
            NcWrite.floatD1(writer, pitchVar, pingTimeIndex, anyChannelData.getPitch());
            NcWrite.floatD1(writer, rollVar, pingTimeIndex, anyChannelData.getRoll());

            NcWrite.floatD1(writer, soundSpeedVar, pingTimeIndex, anyChannelData.getSoundVelocity());
         }

         ArrayFloat.D2 transducerDepthArray = new ArrayFloat.D2(channelCount, 1);
         ArrayFloat.D2 transmitBandwidthArray = new ArrayFloat.D2(channelCount, 1);
         ArrayShort.D2 transmitModeArray = new ArrayShort.D2(channelCount, 1, false);
         ArrayFloat.D2 transmitPowerArray = new ArrayFloat.D2(channelCount, 1);
         ArrayFloat.D2 absorptionArray = new ArrayFloat.D2(channelCount, 1);
         ArrayFloat.D2 receiveDurationEffectiveArray = new ArrayFloat.D2(channelCount, 1);
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            ChannelData channelData = ping.getChannelData(channelIndex + 1);
            int frequencyIndex = ncConfig.channelIndexToNcFrequencyIndex(channelIndex);
            transducerDepthArray.set(frequencyIndex, 0, channelData != null ? channelData.getTransducerDepth() : Float.NaN);
            transmitBandwidthArray.set(frequencyIndex, 0, channelData != null ? channelData.getBandWidth() : Float.NaN);
            transmitModeArray.set(frequencyIndex, 0, channelData != null ? channelData.getTransmitMode() : -1);
            transmitPowerArray.set(frequencyIndex, 0, channelData != null ? channelData.getTransmitPower() : Float.NaN);
            absorptionArray.set(frequencyIndex, 0, channelData != null ? channelData.getAbsorptionCoefficient() : Float.NaN);
            receiveDurationEffectiveArray.set(frequencyIndex, 0, channelData != null ? channelData.getPowerData().getEffectivePulseDuration() : Float.NaN);
         }
         writer.write(transducerDraftVar, new int[]{0, pingTimeIndex}, transducerDepthArray);
         writer.write(transmitBandwidthVar, new int[]{0, pingTimeIndex}, transmitBandwidthArray);
         writer.write(transmitModeVar, new int[]{0, pingTimeIndex}, transmitModeArray);
         writer.write(transmitPowerVar, new int[]{0, pingTimeIndex}, transmitPowerArray);
         writer.write(absorptionVar, new int[]{0, pingTimeIndex}, absorptionArray);
         writer.write(receiveDurationEffectiveVar, new int[]{0, pingTimeIndex}, receiveDurationEffectiveArray);

         writer.write(rawFileVar, new int[]{pingTimeIndex, 0}, rawFileArray);
      };
   }
}
