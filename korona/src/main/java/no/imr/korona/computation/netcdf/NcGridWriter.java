package no.imr.korona.computation.netcdf;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcWrite;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayLong;
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
import java.util.List;
import java.util.Objects;

final class NcGridWriter implements NcPingWriter {
   private final NetcdfFormatWriter writer;

   private final Variable pingTimeVar;
   private final Variable rawFileVar;

   private final Variable longitudeVar;
   private final Variable latitudeVar;

   private final Variable distanceVar;

   private final Variable heaveVar;
   private final Variable headingVar;
   private final Variable pitchVar;
   private final Variable rollVar;

   private final Variable pulseLengthVar;
   private final Variable transducerDraftVar;

   private final ArrayLong.D1 longArray = new ArrayLong.D1(1, false);
   private final ArrayChar.D2 rawFileArray;
   private final ArrayDouble.D1 doubleArray = new ArrayDouble.D1(1);
   private final ArrayFloat.D1 floatArray = new ArrayFloat.D1(1);
   private final ArrayFloat.D2 frequencyFloatArray;

   private final long referenceTimeInMillis;
   private final int channelCount;
   private final int[] channelIndexToNetcdfFrequencyIndex;
   private final float deltaRange;
   private final int rangeLength;
   private int pingTimeIndex;

   private int missingPulseDurationValues;
   private final boolean[] hasWrittenPulseDuration;

   private final GridOutputWriter gridOutputWriter;

   NcGridWriter(Path file, PingConfiguration pingConfiguration, int referenceChannel, float deltaRange, float maxRange,
                GridOutput gridOutput, NcOptionalConfig optionalConfig) throws IOException, InvalidRangeException {

      this.deltaRange = deltaRange;
      referenceTimeInMillis = pingConfiguration.getRawFileConfiguration().getTimeInMillis();
      List<RawFileTransducer> transducers = pingConfiguration.getRawFileConfiguration().getTransducers();
      channelCount = transducers.size();

      channelIndexToNetcdfFrequencyIndex = new int[channelCount];
      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         channelIndexToNetcdfFrequencyIndex[channelIndex] = channelIndex;
      }
      ArrayMath.swap(channelIndexToNetcdfFrequencyIndex, 0, referenceChannel - 1); // Place reference frequency first.

      rangeLength = (int) Math.floor(maxRange / deltaRange);

      String fileName = pingConfiguration.getRawFileConfiguration().getDataFile().getFileName().toString();
      rawFileArray = (ArrayChar.D2) Array.makeFromJavaArray(new char[][]{fileName.toCharArray()});

      frequencyFloatArray = new ArrayFloat.D2(channelCount, 1);

      missingPulseDurationValues = channelCount;
      hasWrittenPulseDuration = new boolean[channelCount];

      NetcdfFormatWriter.Builder fileBuilder = NcUtils.newBuilder(file);
      Group.Builder builder = fileBuilder.getRootGroup();

      Dimension frequencyDim = fileBuilder.addDimension(Nc.FREQUENCY, channelCount);
      Dimension pingTimeDim = fileBuilder.addUnlimitedDimension(Nc.PING_TIME);
      Dimension rangeDim = fileBuilder.addDimension(Nc.RANGE, rangeLength);

      fileBuilder.addVariable(Nc.FREQUENCY, DataType.DOUBLE, List.of(frequencyDim));
      fileBuilder.addVariable(Nc.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));
      fileBuilder.addVariable(Nc.RANGE, DataType.DOUBLE, List.of(rangeDim));

      fileBuilder.addVariable(Nc.CHANNEL_ID, DataType.STRING, List.of(frequencyDim));
      NcWrite.addFloatVariable(builder, Nc.EQUIVALENT_BEAM_ANGLE, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));

      Dimension rawFileDim = fileBuilder.addUnlimitedDimension("raw_file_dim");
      fileBuilder.addVariable(Nc.RAW_FILE, DataType.CHAR, List.of(pingTimeDim, rawFileDim));

      NcWrite.addDoubleVariable(builder, Nc.LONGITUDE, List.of(pingTimeDim), List.of());
      NcWrite.addDoubleVariable(builder, Nc.LATITUDE, List.of(pingTimeDim), List.of());

      NcWrite.addDoubleVariable(builder, Nc.DISTANCE, List.of(pingTimeDim), List.of());

      NcWrite.addFloatVariable(builder, Nc.HEAVE, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));
      NcWrite.addFloatVariable(builder, Nc.HEADING, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));
      NcWrite.addFloatVariable(builder, Nc.PITCH, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));
      NcWrite.addFloatVariable(builder, Nc.ROLL, List.of(pingTimeDim), List.of(Nc.RAW_FILE, Nc.LONGITUDE, Nc.LATITUDE));

      NcWrite.addFloatVariable(builder, Nc.PULSE_LENGTH, List.of(frequencyDim), List.of(Nc.CHANNEL_ID));
      NcWrite.addFloatVariable(builder, Nc.TRANSDUCER_DRAFT, List.of(frequencyDim, pingTimeDim), List.of(Nc.CHANNEL_ID, Nc.RAW_FILE, Nc.LATITUDE, Nc.LONGITUDE));

      NcUtils.addTransducerOffsetVariables(builder, frequencyDim, optionalConfig);

      gridOutput.addVariables(builder, frequencyDim, pingTimeDim, rangeDim);

      writer = fileBuilder.build();

      try {
         Variable frequencyVar = findVariable(Nc.FREQUENCY);
         pingTimeVar = findVariable(Nc.PING_TIME);
         Variable rangeVar = findVariable(Nc.RANGE);
         Variable channelIdVar = findVariable(Nc.CHANNEL_ID);
         Variable equivalentBeamAngleVar = findVariable(Nc.EQUIVALENT_BEAM_ANGLE);

         rawFileVar = findVariable(Nc.RAW_FILE);

         longitudeVar = findVariable(Nc.LONGITUDE);
         latitudeVar = findVariable(Nc.LATITUDE);

         distanceVar = findVariable(Nc.DISTANCE);

         heaveVar = findVariable(Nc.HEAVE);
         headingVar = findVariable(Nc.HEADING);
         pitchVar = findVariable(Nc.PITCH);
         rollVar = findVariable(Nc.ROLL);

         pulseLengthVar = findVariable(Nc.PULSE_LENGTH);
         transducerDraftVar = findVariable(Nc.TRANSDUCER_DRAFT);

         NcUtils.writeTransducerOffsets(writer, transducers, optionalConfig);

         double[] frequencies = new double[channelCount];
         String[] channelIds = new String[channelCount];
         float[] equivalentBeamAngle = new float[channelCount];
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            RawFileTransducer transducer = transducers.get(channelIndex);
            int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
            frequencies[frequencyIndex] = transducer.getFrequency();
            channelIds[frequencyIndex] = transducer.getChannelId();
            equivalentBeamAngle[frequencyIndex] = transducer.getEquivalentBeamAngle();
         }
         writer.write(frequencyVar, new int[]{0}, Array.makeFromJavaArray(frequencies));
         writer.write(channelIdVar, new int[]{0}, Array.factory(DataType.STRING, new int[]{channelCount}, channelIds));
         writer.write(equivalentBeamAngleVar, new int[]{0}, Array.makeFromJavaArray(equivalentBeamAngle));

         double[] ranges = new double[rangeLength];
         for (int i = 0; i < rangeLength; i++) {
            ranges[i] = i * deltaRange;
         }
         writer.write(rangeVar, new int[]{0}, Array.makeFromJavaArray(ranges));

         gridOutputWriter = gridOutput.createWriter(this);

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

   @Override
   public void writePing(Ping ping) throws InvalidRangeException, IOException {
      if (missingPulseDurationValues > 0) {
         for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
            ChannelData channelData = ping.getChannelData(channelIndex + 1);
            if (channelData != null && !hasWrittenPulseDuration[channelIndex]) {
               floatArray.set(0, channelData.getPulseDuration());
               int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
               writer.write(pulseLengthVar, new int[]{frequencyIndex}, floatArray);
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

      writeDouble(distanceVar, pingIndex.getVesselDistance());

      ChannelData anyChannelData = ping.getNonNullChannelData();
      if (anyChannelData != null) {
         writeFloat(heaveVar, anyChannelData.getHeave());
         writeFloat(headingVar, anyChannelData.getHeading());
         writeFloat(pitchVar, anyChannelData.getPitch());
         writeFloat(rollVar, anyChannelData.getRoll());
      }

      for (int channelIndex = 0; channelIndex < channelCount; channelIndex++) {
         ChannelData channelData = ping.getChannelData(channelIndex + 1);
         int frequencyIndex = channelIndexToNetcdfFrequencyIndex[channelIndex];
         frequencyFloatArray.set(frequencyIndex, 0, channelData != null ? channelData.getTransducerDepth() : Float.NaN);
      }
      writer.write(transducerDraftVar, new int[]{0, pingTimeIndex}, frequencyFloatArray);

      writer.write(rawFileVar, new int[]{pingTimeIndex, 0}, rawFileArray);

      gridOutputWriter.write(ping, pingTimeIndex);

      pingTimeIndex++;
   }

   private void writeLong(Variable variable, long value) throws IOException, InvalidRangeException {
      longArray.set(0, value);
      writer.write(variable, new int[]{pingTimeIndex}, longArray);
   }

   private void writeDouble(Variable variable, double value) throws IOException, InvalidRangeException {
      doubleArray.set(0, value);
      writer.write(variable, new int[]{pingTimeIndex}, doubleArray);
   }

   private void writeFloat(Variable variable, float value) throws IOException, InvalidRangeException {
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
