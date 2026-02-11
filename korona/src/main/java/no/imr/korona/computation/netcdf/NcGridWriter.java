package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcWrite;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayByte;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayLong;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class NcGridWriter implements AutoCloseable {
   private final NetcdfFormatWriter writer;

   private final Variable pingTimeVar;
   private final Variable rawFileVar;

   private final Variable longitudeVar;
   private final Variable latitudeVar;

   private final Variable pingNumberVar;
   private final Variable distanceVar;

   private final Variable bottomDepthVar;

   private final Variable heaveVar;
   private final Variable headingVar;
   private final Variable pitchVar;
   private final Variable rollVar;

   private final Variable soundSpeedVar;

   private final Variable pulseLengthVar;
   private final Variable saCorrectionVar;
   private final Variable gainVar;

   private final Variable transducerDraftVar;
   private final Variable transmitBandwidthVar;
   private final Variable transmitModeVar;
   private final Variable transmitPowerVar;
   private final Variable absorptionVar;
   private final Variable receiveDurationEffectiveVar;

   private final ArrayInt.D1 uintArray = new ArrayInt.D1(1, true);
   private final ArrayLong.D1 longArray = new ArrayLong.D1(1, false);
   private final ArrayChar.D2 rawFileArray;
   private final ArrayDouble.D1 doubleArray = new ArrayDouble.D1(1);
   private final ArrayFloat.D1 floatArray = new ArrayFloat.D1(1);

   private final long referenceTimeInMillis;
   private final int channelCount;
   private final int[] channelIndexToNetcdfFrequencyIndex;
   private final float deltaRange;
   private final int rangeLength;
   private int pingTimeIndex;

   private int missingPulseDurationValues;
   private final boolean[] hasWrittenPulseDuration;

   private final @Nullable CommonGridOutputWriter commonGridOutputWriter;
   private final List<ChannelGroupOutputWriter> channelGroupOutputWriters = new ArrayList<>();

   NcGridWriter(Path file, PingConfiguration pingConfiguration, int referenceChannel,
                @Nullable CommonGridConfig commonGridConfig, @Nullable ChannelGroupConfig channelGroupConfig,
                NcOptionalConfig optionalConfig
   ) throws IOException, InvalidRangeException {

      referenceTimeInMillis = pingConfiguration.getRawFileConfiguration().getTimeInMillis();
      List<RawFileTransducer> transducers = pingConfiguration.getRawFileConfiguration().getTransducers();
      channelCount = transducers.size();

      channelIndexToNetcdfFrequencyIndex = new int[channelCount];
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         channelIndexToNetcdfFrequencyIndex[channelIndex] = channelIndex;
      }
      ArrayMath.swap(channelIndexToNetcdfFrequencyIndex, 0, referenceChannel - 1); // Place reference frequency first.

      if (commonGridConfig != null) {
         deltaRange = commonGridConfig.deltaRange();
         rangeLength = (int) Math.floor(commonGridConfig.maxRange() / deltaRange);
      } else {
         deltaRange = Float.NaN;
         rangeLength = 0;
      }

      String fileName = pingConfiguration.getRawFileConfiguration().getDataFile().getFileName().toString();
      rawFileArray = (ArrayChar.D2) Array.makeFromJavaArray(new char[][]{fileName.toCharArray()});

      missingPulseDurationValues = channelCount;
      hasWrittenPulseDuration = new boolean[channelCount];

      NetcdfFormatWriter.Builder fileBuilder = NcUtils.newBuilder(file);
      Group.Builder builder = fileBuilder.getRootGroup();

      Dimension frequencyDim = fileBuilder.addDimension(Nc.FREQUENCY, channelCount);
      Dimension pingTimeDim = fileBuilder.addUnlimitedDimension(Nc.PING_TIME);
      Dimension rangeDim = commonGridConfig != null ? fileBuilder.addDimension(Nc.RANGE, rangeLength) : null;

      fileBuilder.addVariable(Nc.FREQUENCY, DataType.DOUBLE, List.of(frequencyDim));
      fileBuilder.addVariable(Nc.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      if (rangeDim != null) {
         fileBuilder.addVariable(Nc.RANGE, DataType.DOUBLE, List.of(rangeDim));
      }

      fileBuilder.addVariable(Nc.CHANNEL_ID, DataType.STRING, List.of(frequencyDim));
      NcWrite.addFloatVariable(builder, Nc.ANGLE_SENSITIVITY_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.ANGLE_SENSITIVITY_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.ANGLE_OFFSET_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.ANGLE_OFFSET_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.BEAMWIDTH_ALONGSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.BEAMWIDTH_ATHWARTSHIP, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addVariable(builder, Nc.BEAM_TYPE, DataType.UBYTE, List.of(frequencyDim));
      NcWrite.addFloatVariable(builder, Nc.EQUIVALENT_BEAM_ANGLE, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));

      Dimension rawFileDim = fileBuilder.addUnlimitedDimension("raw_file_dim");
      fileBuilder.addVariable(Nc.RAW_FILE, DataType.CHAR, List.of(pingTimeDim, rawFileDim));

      NcWrite.addDoubleVariable(builder, Nc.LONGITUDE, List.of(pingTimeDim), List.of());
      NcWrite.addDoubleVariable(builder, Nc.LATITUDE, List.of(pingTimeDim), List.of());

      NcWrite.addVariable(builder, Nc.PING_NUMBER, DataType.UINT, List.of(pingTimeDim));
      NcWrite.addDoubleVariable(builder, Nc.DISTANCE, List.of(pingTimeDim), List.of());

      NcWrite.addFloatVariable(builder, Nc.BOTTOM_DEPTH, List.of(frequencyDim, pingTimeDim), List.of());

      NcWrite.addFloatVariable(builder, Nc.HEAVE, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));
      NcWrite.addFloatVariable(builder, Nc.HEADING, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));
      NcWrite.addFloatVariable(builder, Nc.PITCH, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));
      NcWrite.addFloatVariable(builder, Nc.ROLL, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));

      NcWrite.addFloatVariable(builder, Nc.SOUND_SPEED, List.of(pingTimeDim), List.of());

      NcWrite.addFloatVariable(builder, Nc.PULSE_LENGTH, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.SA_CORRECTION, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.GAIN, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));

      NcWrite.addFloatVariable(builder, Nc.TRANSDUCER_DRAFT, List.of(frequencyDim, pingTimeDim), List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE));
      NcWrite.addFloatVariable(builder, Nc.TRANSMIT_BANDWIDTH, List.of(frequencyDim, pingTimeDim), List.of());
      NcWrite.addVariable(builder, Nc.TRANSMIT_MODE, DataType.SHORT, List.of(frequencyDim, pingTimeDim));
      NcWrite.addFloatVariable(builder, Nc.TRANSMIT_POWER, List.of(frequencyDim, pingTimeDim), List.of());
      NcWrite.addFloatVariable(builder, Nc.ABSORPTION, List.of(frequencyDim, pingTimeDim), List.of());
      NcWrite.addFloatVariable(builder, Nc.RECEIVE_DURATION_EFFECTIVE, List.of(frequencyDim, pingTimeDim), List.of());

      NcUtils.addTransducerOffsetVariables(builder, frequencyDim, optionalConfig);

      if (commonGridConfig != null && rangeDim != null) {
         commonGridConfig.commonGridOutput().addVariables(builder, frequencyDim, pingTimeDim, rangeDim);
      }

      if (channelGroupConfig != null) {
         Group.Builder environmentBuilder = new Group.Builder()
               .setName(Nc.ENVIRONMENT_GROUP);
         builder.addGroup(environmentBuilder);

         NcWrite.addFloatVariable(environmentBuilder, Nc.ENVIRONMENT_DEPTH, List.of(), List.of());
         NcWrite.addFloatVariable(environmentBuilder, Nc.ENVIRONMENT_ACIDITY, List.of(), List.of());
         NcWrite.addFloatVariable(environmentBuilder, Nc.ENVIRONMENT_SALINITY, List.of(), List.of());
         NcWrite.addFloatVariable(environmentBuilder, Nc.ENVIRONMENT_TEMPERATURE, List.of(), List.of());
      }

      List<ChannelGroupOutputBuilder> channelGroupOutputBuilders = new ArrayList<>();
      if (channelGroupConfig != null) {
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            ChannelData channelData = channelGroupConfig.channelToChannelData().get(channelIndex + 1);
            if (channelData != null) {
               int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
               ChannelGroupOutputBuilder channelGroupOutputBuilder = channelGroupConfig.channelGroupOutput().createBuilder(builder, frequencyIndex, channelData);
               if (channelGroupOutputBuilder != null) {
                  channelGroupOutputBuilders.add(channelGroupOutputBuilder);
               }
            }
         }
      }

      writer = fileBuilder.build();

      try {
         Variable frequencyVar = findVariable(Nc.FREQUENCY);
         pingTimeVar = findVariable(Nc.PING_TIME);
         Variable rangeVar = commonGridConfig != null ? findVariable(Nc.RANGE) : null;
         Variable channelIdVar = findVariable(Nc.CHANNEL_ID);
         Variable equivalentBeamAngleVar = findVariable(Nc.EQUIVALENT_BEAM_ANGLE);

         rawFileVar = findVariable(Nc.RAW_FILE);

         longitudeVar = findVariable(Nc.LONGITUDE);
         latitudeVar = findVariable(Nc.LATITUDE);

         pingNumberVar = findVariable(Nc.PING_NUMBER);
         distanceVar = findVariable(Nc.DISTANCE);

         bottomDepthVar = findVariable(Nc.BOTTOM_DEPTH);

         heaveVar = findVariable(Nc.HEAVE);
         headingVar = findVariable(Nc.HEADING);
         pitchVar = findVariable(Nc.PITCH);
         rollVar = findVariable(Nc.ROLL);

         soundSpeedVar = findVariable(Nc.SOUND_SPEED);

         pulseLengthVar = findVariable(Nc.PULSE_LENGTH);
         saCorrectionVar = findVariable(Nc.SA_CORRECTION);
         gainVar = findVariable(Nc.GAIN);
         transducerDraftVar = findVariable(Nc.TRANSDUCER_DRAFT);
         transmitBandwidthVar = findVariable(Nc.TRANSMIT_BANDWIDTH);
         transmitModeVar = findVariable(Nc.TRANSMIT_MODE);
         transmitPowerVar = findVariable(Nc.TRANSMIT_POWER);
         absorptionVar = findVariable(Nc.ABSORPTION);
         receiveDurationEffectiveVar = findVariable(Nc.RECEIVE_DURATION_EFFECTIVE);

         NcUtils.writeTransducerOffsets(writer, transducers, optionalConfig);

         double[] frequencies = new double[channelCount];
         String[] channelIds = new String[channelCount];
         ArrayFloat.D1 angleSensitivityAlongship = new ArrayFloat.D1(channelCount);
         ArrayFloat.D1 angleSensitivityAthwartship = new ArrayFloat.D1(channelCount);
         ArrayFloat.D1 angleOffsetAlongship = new ArrayFloat.D1(channelCount);
         ArrayFloat.D1 angleOffsetAthwartship = new ArrayFloat.D1(channelCount);
         ArrayFloat.D1 beamwidthAlongship = new ArrayFloat.D1(channelCount);
         ArrayFloat.D1 beamwidthAthwartship = new ArrayFloat.D1(channelCount);
         ArrayByte.D1 beamType = new ArrayByte.D1(channelCount, true);
         float[] equivalentBeamAngle = new float[channelCount];
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            RawFileTransducer transducer = transducers.get(channelIndex);
            int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
            frequencies[frequencyIndex] = transducer.getFrequency();
            channelIds[frequencyIndex] = transducer.getChannelId();
            angleSensitivityAlongship.set(frequencyIndex, transducer.getAngleSensitivityAlongship());
            angleSensitivityAthwartship.set(frequencyIndex, transducer.getAngleSensitivityAthwartship());
            angleOffsetAlongship.set(frequencyIndex, transducer.getAngleOffsetAlongship());
            angleOffsetAthwartship.set(frequencyIndex, transducer.getAngleOffsetAthwartship());
            beamwidthAlongship.set(frequencyIndex, transducer.getBeamWidthAlongship());
            beamwidthAthwartship.set(frequencyIndex, transducer.getBeamWidthAthwartship());
            beamType.set(frequencyIndex, (byte) transducer.getBeamType());
            equivalentBeamAngle[frequencyIndex] = transducer.getEquivalentBeamAngle();
         }
         writer.write(frequencyVar, new int[]{0}, Array.makeFromJavaArray(frequencies));
         writer.write(channelIdVar, new int[]{0}, Array.factory(DataType.STRING, new int[]{channelCount}, channelIds));
         writer.write(findVariable(Nc.ANGLE_SENSITIVITY_ALONGSHIP), new int[]{0}, angleSensitivityAlongship);
         writer.write(findVariable(Nc.ANGLE_SENSITIVITY_ATHWARTSHIP), new int[]{0}, angleSensitivityAthwartship);
         writer.write(findVariable(Nc.ANGLE_OFFSET_ALONGSHIP), new int[]{0}, angleOffsetAlongship);
         writer.write(findVariable(Nc.ANGLE_OFFSET_ATHWARTSHIP), new int[]{0}, angleOffsetAthwartship);
         writer.write(findVariable(Nc.BEAMWIDTH_ALONGSHIP), new int[]{0}, beamwidthAlongship);
         writer.write(findVariable(Nc.BEAMWIDTH_ATHWARTSHIP), new int[]{0}, beamwidthAthwartship);
         writer.write(findVariable(Nc.BEAM_TYPE), new int[]{0}, beamType);
         writer.write(equivalentBeamAngleVar, new int[]{0}, Array.makeFromJavaArray(equivalentBeamAngle));

         if (commonGridConfig != null) {
            double[] ranges = new double[rangeLength];
            for (int i = 0; i < rangeLength; i++) {
               ranges[i] = i * deltaRange;
            }
            writer.write(rangeVar, new int[]{0}, Array.makeFromJavaArray(ranges));
         }

         commonGridOutputWriter = commonGridConfig != null ? commonGridConfig.commonGridOutput().createWriter(this) : null;

         if (channelGroupConfig != null) {
            RawFileConfiguration.Xml0Info xml0Info = pingConfiguration.getRawFileConfiguration().getXml0Info();
            if (xml0Info != null) {
               NcWrite.writeScalarFloat(writer, findVariable(Nc.ENVIRONMENT_GROUP + "/" + Nc.ENVIRONMENT_DEPTH), xml0Info.getDepth());
               NcWrite.writeScalarFloat(writer, findVariable(Nc.ENVIRONMENT_GROUP + "/" + Nc.ENVIRONMENT_ACIDITY), xml0Info.getAcidity());
               NcWrite.writeScalarFloat(writer, findVariable(Nc.ENVIRONMENT_GROUP + "/" + Nc.ENVIRONMENT_SALINITY), xml0Info.getSalinity());
               NcWrite.writeScalarFloat(writer, findVariable(Nc.ENVIRONMENT_GROUP + "/" + Nc.ENVIRONMENT_TEMPERATURE), xml0Info.getTemperature());
            }
         }

         for (ChannelGroupOutputBuilder channelGroupOutputBuilder : channelGroupOutputBuilders) {
            channelGroupOutputWriters.add(channelGroupOutputBuilder.createWriter(this));
         }
      } catch (Exception e) {
         try {
            writer.close();
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
   }

   Variable findVariable(String variable) {
      return Objects.requireNonNull(writer.findVariable(variable), variable);
   }

   NetcdfFormatWriter getWriter() {
      return writer;
   }

   int[] getChannelIndexToNetcdfFrequencyIndex() {
      return channelIndexToNetcdfFrequencyIndex;
   }

   float getDeltaRange() {
      return deltaRange;
   }

   int getRangeLength() {
      return rangeLength;
   }

   void writePing(Ping ping) throws InvalidRangeException, IOException {
      if (missingPulseDurationValues > 0) {
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            ChannelData channelData = ping.getChannelData(channelIndex + 1);
            if (channelData != null && !hasWrittenPulseDuration[channelIndex]) {
               int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
               float pulseDuration = channelData.getPulseDuration();
               writer.write(pulseLengthVar, new int[]{frequencyIndex}, Array.makeFromJavaArray(new float[]{pulseDuration}));
               writer.write(saCorrectionVar, new int[]{frequencyIndex}, Array.makeFromJavaArray(new float[]{channelData.getTransducer().getSaCorrection(pulseDuration)}));
               writer.write(gainVar, new int[]{frequencyIndex}, Array.makeFromJavaArray(new float[]{channelData.getTransducer().getGainForPulseDuration(pulseDuration)}));
               hasWrittenPulseDuration[channelIndex] = true;
               missingPulseDurationValues--;
            }
         }
      }

      PingIndex pingIndex = ping.getPingIndex();

      writeLong(pingTimeVar, (pingIndex.getTimeInMillis() - referenceTimeInMillis) * 1_000_000);

      GeoPoint geoPos = pingIndex.getGeographicalPosition();
      if (geoPos != null) {
         writeDouble(latitudeVar, geoPos.getLatitude());
         writeDouble(longitudeVar, geoPos.getLongitude());
      }

      writeUInt(pingNumberVar, (int) pingIndex.getPingNumber());
      writeDouble(distanceVar, pingIndex.getVesselDistance());

      ArrayFloat.D2 bottomDepthArray = new ArrayFloat.D2(channelCount, 1);
      double[] bot0Depths = ping.getBot0Datagram().getChannelDepths();
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
         bottomDepthArray.set(frequencyIndex, 0, (float) bot0Depths[channelIndex]);
      }
      writer.write(bottomDepthVar, new int[]{0, pingTimeIndex}, bottomDepthArray);

      ChannelData anyChannelData = ping.getNonNullChannelData();
      if (anyChannelData != null) {
         writeFloat(heaveVar, anyChannelData.getHeave());
         writeFloat(headingVar, anyChannelData.getHeading());
         writeFloat(pitchVar, anyChannelData.getPitch());
         writeFloat(rollVar, anyChannelData.getRoll());

         writeFloat(soundSpeedVar, anyChannelData.getSoundVelocity());
      }

      ArrayFloat.D2 transducerDepthArray = new ArrayFloat.D2(channelCount, 1);
      ArrayFloat.D2 transmitBandwidthArray = new ArrayFloat.D2(channelCount, 1);
      ArrayShort.D2 transmitModeArray = new ArrayShort.D2(channelCount, 1, false);
      ArrayFloat.D2 transmitPowerArray = new ArrayFloat.D2(channelCount, 1);
      ArrayFloat.D2 absorptionArray = new ArrayFloat.D2(channelCount, 1);
      ArrayFloat.D2 receiveDurationEffectiveArray = new ArrayFloat.D2(channelCount, 1);
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         ChannelData channelData = ping.getChannelData(channelIndex + 1);
         int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
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

      if (commonGridOutputWriter != null) {
         commonGridOutputWriter.write(ping, pingTimeIndex);
      }

      for (ChannelGroupOutputWriter channelGroupOutputWriter : channelGroupOutputWriters) {
         channelGroupOutputWriter.write(ping, pingTimeIndex);
      }

      pingTimeIndex++;
   }

   private void writeUInt(Variable variable, int value) throws IOException, InvalidRangeException {
      uintArray.set(0, value);
      writer.write(variable, new int[]{pingTimeIndex}, uintArray);
   }

   void writeLong(Variable variable, long value) throws IOException, InvalidRangeException {
      longArray.set(0, value);
      writer.write(variable, new int[]{pingTimeIndex}, longArray);
   }

   private void writeDouble(Variable variable, double value) throws IOException, InvalidRangeException {
      doubleArray.set(0, value);
      writer.write(variable, new int[]{pingTimeIndex}, doubleArray);
   }

   void writeFloat(Variable variable, float value) throws IOException, InvalidRangeException {
      floatArray.set(0, value);
      writer.write(variable, new int[]{pingTimeIndex}, floatArray);
   }

   @Nullable OffsetValues resample(ChannelData channelData, float[] values) {
      return OffsetValues.resample(values, channelData, deltaRange, rangeLength);
   }

   @Override
   public void close() throws IOException {
      writer.close();
   }
}
