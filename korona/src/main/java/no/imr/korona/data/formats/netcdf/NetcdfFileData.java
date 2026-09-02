package no.imr.korona.data.formats.netcdf;

import no.imr.korona.computation.netcdf.LogSvDecompressor;
import no.imr.korona.computation.netcdf.Nc;
import no.imr.korona.computation.netcdf.OffsetValues;
import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.math.ArrayMath;
import no.imr.tools.netcdf.NcTimeDef;
import no.imr.tools.netcdf.NetcdfDataException;
import no.imr.tools.netcdf.NetcdfUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Section;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

final class NetcdfFileData implements Closeable {
   private final Path file;
   private final NetcdfFile dataset;

   private final Variable heaveVar;
   private final Variable rollVar;
   private final Variable pitchVar;
   private final Variable headingVar;
   private final Variable soundSpeedVar;

   private final Variable transducerDraftVar;
   private final Variable transmitBandwidthVar;
   private final Variable transmitModeVar;
   private final Variable transmitPowerVar;
   private final Variable absorptionVar;
   private final Variable receiveDurationEffectiveVar;

   private final Variable svVar;
   private final @Nullable LogSvDecompressor logSvDecompressor;
   private final @Nullable Variable angleAlongshipVar;
   private final @Nullable Variable angleAthwartshipVar;

   private final float startRange;
   private final float sampleDistance;
   private final float[] frequencies;
   private final float[] pulseDurations;
   private final int[] koronaToNcChannelIndex;

   NetcdfFileData(Path file) throws IOException {
      this.file = file;
      dataset = NetcdfFiles.open(file.toString());
      try {
         heaveVar = findVariable(Nc.HEAVE);
         rollVar = findVariable(Nc.ROLL);
         pitchVar = findVariable(Nc.PITCH);
         headingVar = findVariable(Nc.HEADING);
         soundSpeedVar = findVariable(Nc.SOUND_SPEED);

         transducerDraftVar = findVariable(Nc.TRANSDUCER_DRAFT);
         transmitBandwidthVar = findVariable(Nc.TRANSMIT_BANDWIDTH);
         transmitModeVar = findVariable(Nc.TRANSMIT_MODE);
         transmitPowerVar = findVariable(Nc.TRANSMIT_POWER);
         absorptionVar = findVariable(Nc.ABSORPTION);
         receiveDurationEffectiveVar = findVariable(Nc.RECEIVE_DURATION_EFFECTIVE);

         Variable logSvCompressedVar = findOptionalVariable(Nc.LOG_SV_COMPRESSED);
         if (logSvCompressedVar != null) {
            logSvDecompressor = new LogSvDecompressor(logSvCompressedVar);
            svVar = logSvCompressedVar;
         } else {
            logSvDecompressor = null;
            svVar = findVariable(Nc.SV);
         }
         angleAlongshipVar = findOptionalVariable(Nc.ANGLE_ALONGSHIP);
         angleAthwartshipVar = findOptionalVariable(Nc.ANGLE_ATHWARTSHIP);

         frequencies = NetcdfUtils.readFloatArray(findVariable(Nc.FREQUENCY));
         pulseDurations = NetcdfUtils.readFloatArray(findVariable(Nc.PULSE_LENGTH));

         koronaToNcChannelIndex = IntStream.range(0, frequencies.length)
               .boxed()
               .sorted(Comparator.comparingDouble(i -> frequencies[i]))
               .mapToInt(Integer::intValue)
               .toArray();

         Variable rangeVar = findVariable(Nc.RANGE);
         startRange = (float) NetcdfUtils.readDouble(rangeVar, 0);
         sampleDistance = (float) (NetcdfUtils.readDouble(rangeVar, 1) - startRange);

      } catch (Exception e) {
         Utils.closeOrSuppress(e, dataset);
         throw e;
      }
   }

   @Override
   public void close() throws IOException {
      dataset.close();
   }

   private Variable findVariable(String variableName) throws NetcdfDataException {
      return NetcdfUtils.findVariable(dataset.getRootGroup(), variableName);
   }

   private @Nullable Variable findOptionalVariable(String variableName) {
      return NetcdfUtils.findOptionalVariable(dataset.getRootGroup(), variableName);
   }

   SegmentInfo createSegmentInfo() throws IOException, InvalidRangeException {
      PingRange pingRange = createPingRange();
      RawFileConfiguration rawFileConfiguration = createRawFileConfiguration(pingRange.begin().getInstant());
      return new SegmentInfo(rawFileConfiguration, pingRange);
   }

   RawFileConfiguration createRawFileConfiguration(Instant instant) throws IOException {
      RawFileConfiguration rawFileConfiguration = new RawFileConfiguration(instant);
      rawFileConfiguration.setDataFile(file);

      String[] channelId = readStringArray(findVariable(Nc.CHANNEL_ID));
      float[] angleSensitivityAlongship = NetcdfUtils.readFloatArray(findVariable(Nc.ANGLE_SENSITIVITY_ALONGSHIP));
      float[] angleSensitivityAthwartship = NetcdfUtils.readFloatArray(findVariable(Nc.ANGLE_SENSITIVITY_ATHWARTSHIP));
      float[] angleOffsetAlongship = NetcdfUtils.readFloatArray(findVariable(Nc.ANGLE_OFFSET_ALONGSHIP));
      float[] angleOffsetAthwartship = NetcdfUtils.readFloatArray(findVariable(Nc.ANGLE_OFFSET_ATHWARTSHIP));
      float[] beamwidthAlongship = NetcdfUtils.readFloatArray(findVariable(Nc.BEAMWIDTH_ALONGSHIP));
      float[] beamwidthAthwartship = NetcdfUtils.readFloatArray(findVariable(Nc.BEAMWIDTH_ATHWARTSHIP));
      byte[] beamType = (byte[]) findVariable(Nc.BEAM_TYPE).read().get1DJavaArray(DataType.BYTE);
      float[] equivalentBeamAngle = NetcdfUtils.readFloatArray(findVariable(Nc.EQUIVALENT_BEAM_ANGLE));
      float[] saCorrection = NetcdfUtils.readFloatArray(findVariable(Nc.SA_CORRECTION));
      float[] gain = NetcdfUtils.readFloatArray(findVariable(Nc.GAIN));

      for (int koronaChannelIndex = 0; koronaChannelIndex < channelId.length; koronaChannelIndex++) {
         int ncChannelIndex = koronaToNcChannelIndex[koronaChannelIndex];
         RawFileTransducer transducer = new RawFileTransducer();
         transducer.setChannelId(channelId[ncChannelIndex]);
         transducer.setBeamType(beamType[ncChannelIndex]);
         transducer.setFrequency(frequencies[ncChannelIndex]);
         Arrays.fill(transducer.getPulseDurationTable(), pulseDurations[ncChannelIndex]);
         transducer.setGainAndGainTable(gain[ncChannelIndex]);
         transducer.setAngleOffsetAlongship(angleOffsetAlongship[ncChannelIndex]);
         transducer.setAngleOffsetAthwartship(angleOffsetAthwartship[ncChannelIndex]);
         transducer.setAngleSensitivityAlongship(angleSensitivityAlongship[ncChannelIndex]);
         transducer.setAngleSensitivityAthwartship(angleSensitivityAthwartship[ncChannelIndex]);
         transducer.setBeamWidthAlongship(beamwidthAlongship[ncChannelIndex]);
         transducer.setBeamWidthAthwartship(beamwidthAthwartship[ncChannelIndex]);
         transducer.setEquivalentBeamAngle(equivalentBeamAngle[ncChannelIndex]);
         Arrays.fill(transducer.getSaCorrectionTable(), saCorrection[ncChannelIndex]);
         rawFileConfiguration.getTransducers().add(transducer);
      }
      return rawFileConfiguration;
   }

   private static String[] readStringArray(Variable variable) throws IOException {
      // Array.get1DJavaArray(DataType.STRING) returns Object[], not String[]. Why?
      Object[] array = (Object[]) variable.read().get1DJavaArray(DataType.STRING);
      return Arrays.stream(array).map(Object::toString).toArray(String[]::new);
   }

   private PingRange createPingRange() throws IOException, InvalidRangeException {
      Variable pingTimeVar = findVariable(Nc.PING_TIME);
      NcTimeDef pingTimeDef = NcTimeDef.fromVariable(pingTimeVar);
      Variable pingNumberVar = findVariable(Nc.PING_NUMBER);
      Variable distanceVar = findVariable(Nc.DISTANCE);
      Variable longitudeVar = findVariable(Nc.LONGITUDE);
      Variable latitudeVar = findVariable(Nc.LATITUDE);

      Section firstSection = new Section(new int[]{0}, new int[]{1});
      PingIndex first = new DefaultPingIndex(
            pingTimeDef.timeValueToInstant(pingTimeVar.read(firstSection).getLong(0)),
            pingNumberVar.read(firstSection).getLong(0),
            distanceVar.read(firstSection).getDouble(0),
            toGeoPos(longitudeVar.read(firstSection).getDouble(0), latitudeVar.read(firstSection).getDouble(0))
      );

      int n = pingTimeVar.getShape(0);
      Section lastSection = new Section(new int[]{n - 1}, new int[]{1});
      PingIndex end = new DefaultPingIndex(
            pingTimeDef.timeValueToInstant(pingTimeVar.read(lastSection).getLong(0)).plusMillis(1),
            pingNumberVar.read(lastSection).getLong(0),
            distanceVar.read(lastSection).getDouble(0),
            toGeoPos(longitudeVar.read(lastSection).getDouble(0), latitudeVar.read(lastSection).getDouble(0))
      );

      return PingRange.of(first, end);
   }

   List<PingIndex> createPingIndexes() throws IOException {
      Variable pingTimeVar = findVariable(Nc.PING_TIME);
      NcTimeDef pingTimeDef = NcTimeDef.fromVariable(pingTimeVar);
      long[] pingTimes = (long[]) pingTimeVar.read().get1DJavaArray(DataType.LONG);
      long[] pingNumbers = (long[]) findVariable(Nc.PING_NUMBER).read().get1DJavaArray(DataType.LONG);
      double[] distances = (double[]) findVariable(Nc.DISTANCE).read().get1DJavaArray(DataType.DOUBLE);
      double[] longitudes = (double[]) findVariable(Nc.LONGITUDE).read().get1DJavaArray(DataType.DOUBLE);
      double[] latitudes = (double[]) findVariable(Nc.LATITUDE).read().get1DJavaArray(DataType.DOUBLE);
      return IntStream.range(0, pingTimes.length)
            .<PingIndex>mapToObj(i -> {
               return new DefaultPingIndex(
                     pingTimeDef.timeValueToInstant(pingTimes[i]),
                     pingNumbers[i],
                     distances[i],
                     toGeoPos(longitudes[i], latitudes[i])
               );
            })
            .toList();
   }

   private static @Nullable GeoPoint toGeoPos(double longitude, double latitude) {
      return !Double.isNaN(longitude) && !Double.isNaN(latitude) ? new GeoPoint(longitude, latitude) : null;
   }

   List<Bot0Datagram> createBot0Datagrams(List<PingIndex> pingIndices) throws IOException {
      Variable bottomDepthVariable = findOptionalVariable(Nc.BOTTOM_DEPTH);
      if (bottomDepthVariable == null) {
         return pingIndices.stream()
               .map(pingIndex -> new Bot0Datagram(pingIndex.getInstant(), frequencies.length))
               .toList();
      }
      ArrayFloat.D2 bottomDepthArray = (ArrayFloat.D2) bottomDepthVariable.read();
      return IntStream.range(0, pingIndices.size())
            .mapToObj(i -> {
               double[] bot0Depths = new double[frequencies.length];
               for (int koronaChannelIndex = 0; koronaChannelIndex < bot0Depths.length; koronaChannelIndex++) {
                  int ncChannelIndex = koronaToNcChannelIndex[koronaChannelIndex];
                  bot0Depths[koronaChannelIndex] = bottomDepthArray.get(ncChannelIndex, i);
               }
               return new Bot0Datagram(pingIndices.get(i).getInstant(), bot0Depths);
            })
            .toList();
   }

   void addPingItems(PingData pingData, PingIndex pingIndex, int pingTimeIndex, AsyncHandle asyncHandle) throws IOException, InvalidRangeException {
      int channelCount = svVar.getShape(0);
      int sampleCount = svVar.getShape(2);

      Section section1D = new Section(new int[]{pingTimeIndex}, new int[]{1});
      float heave = heaveVar.read(section1D).getFloat(0);
      float roll = rollVar.read(section1D).getFloat(0);
      float pitch = pitchVar.read(section1D).getFloat(0);
      float heading = headingVar.read(section1D).getFloat(0);
      float soundSpeed = soundSpeedVar.read(section1D).getFloat(0);

      Section section2D = new Section(new int[]{0, pingTimeIndex}, new int[]{channelCount, 1});
      float[] transducerDepths = (float[]) transducerDraftVar.read(section2D).get1DJavaArray(DataType.FLOAT);
      float[] transmitBandwidthArray = (float[]) transmitBandwidthVar.read(section2D).get1DJavaArray(DataType.FLOAT);
      short[] transmitModeArray = (short[]) transmitModeVar.read(section2D).get1DJavaArray(DataType.SHORT);
      float[] transmitPowerArray = (float[]) transmitPowerVar.read(section2D).get1DJavaArray(DataType.FLOAT);
      float[] absorptionArray = (float[]) absorptionVar.read(section2D).get1DJavaArray(DataType.FLOAT);
      float[] receiveDurationEffectiveArray = (float[]) receiveDurationEffectiveVar.read(section2D).get1DJavaArray(DataType.FLOAT);

      Section section3D = new Section(new int[]{0, pingTimeIndex, 0}, new int[]{channelCount, 1, sampleCount});
      Array svArray = svVar.read(section3D);
      Array angleAlongshipArray = angleAlongshipVar != null ? angleAlongshipVar.read(section3D) : null;
      Array angleAthwartshipArray = angleAthwartshipVar != null ? angleAthwartshipVar.read(section3D) : null;

      for (int koronaChannelIndex = 0; koronaChannelIndex < channelCount; koronaChannelIndex++) {
         if (asyncHandle.isCancelled()) {
            return;
         }
         int ncChannelIndex = koronaToNcChannelIndex[koronaChannelIndex];
         float transducerDepth = transducerDepths[ncChannelIndex];
         if (Float.isNaN(transducerDepth)) {
            continue;
         }
         RawFileTransducer transducer = pingData.getPingConfiguration().getRawFileConfiguration().getTransducers().get(koronaChannelIndex);
         int iBegin;
         float[] sv;
         if (logSvDecompressor != null) {
            short[] compressedValues = (short[]) svArray.section(new int[]{ncChannelIndex, 0, 0}, new int[]{1, 1, sampleCount}).get1DJavaArray(DataType.USHORT);
            OffsetValues decompressed = logSvDecompressor.decompressToSv(compressedValues);
            iBegin = decompressed.offset();
            sv = decompressed.values();
         } else {
            sv = (float[]) svArray.section(new int[]{ncChannelIndex, 0, 0}, new int[]{1, 1, sampleCount}).get1DJavaArray(DataType.FLOAT);
            int iEnd = sv.length;
            while (iEnd > 0 && Float.isNaN(sv[iEnd - 1])) {
               iEnd--;
            }
            iBegin = 0;
            while (iBegin < iEnd && Float.isNaN(sv[iBegin])) {
               iBegin++;
            }
            if (iEnd - iBegin != sv.length) {
               sv = Arrays.copyOfRange(sv, iBegin, iEnd);
            }
            ArrayMath.multiply(sv, PowerData.IMR_CONSTANT);
         }
         PowerData powerData = new PowerData(pingIndex.getInstant());
         powerData.setChannel(koronaChannelIndex + 1);
         powerData.setTransducerDepth(transducerDepth);
         powerData.setFrequency(frequencies[ncChannelIndex]);
         powerData.setTransmitPower(transmitPowerArray[ncChannelIndex]);
         powerData.setPulseDuration(pulseDurations[ncChannelIndex]);
         powerData.setEffectivePulseDuration(receiveDurationEffectiveArray[ncChannelIndex]);
         powerData.setBandWidth(transmitBandwidthArray[ncChannelIndex]);
         powerData.setSoundVelocity(soundSpeed);
         powerData.setAbsorptionCoefficient(absorptionArray[ncChannelIndex]);
         powerData.setHeave(heave);
         powerData.setRoll(roll);
         powerData.setPitch(pitch);
         //powerData.setTemperature(); // Not used.
         powerData.setHeading(heading);
         powerData.setTransmitMode(transmitModeArray[ncChannelIndex]);
         //powerData.setSampleInterval(); // Cannot use original sample interval since data is gridded.
         powerData.setSampleDistance(sampleDistance); // Sets sample interval. Must be called after setting sound speed.
         powerData.setOffset(Math.round(startRange / sampleDistance) + iBegin);
         powerData.setSv(sv);
         if (angleAlongshipArray != null && angleAthwartshipArray != null) {
            float[] angleAlongship = (float[]) angleAlongshipArray.section(new int[]{ncChannelIndex, 0, 0}, new int[]{1, 1, sampleCount}).get1DJavaArray(DataType.FLOAT);
            float[] angleAthwartship = (float[]) angleAthwartshipArray.section(new int[]{ncChannelIndex, 0, 0}, new int[]{1, 1, sampleCount}).get1DJavaArray(DataType.FLOAT);
            float sensitivityAlongship = transducer.getAngleSensitivityAlongship();
            float sensitivityAthwartship = transducer.getAngleSensitivityAthwartship();
            float offsetAlongship = transducer.getAngleOffsetAlongship();
            float offsetAthwartship = transducer.getAngleOffsetAthwartship();
            float[] electricalAngles = new float[2 * sv.length];
            for (int i = 0; i < sv.length; i++) {
               electricalAngles[2 * i + 1] = AngleData.mechanicalToElectricalAngle(angleAlongship[i + iBegin], sensitivityAlongship, offsetAlongship);
               electricalAngles[2 * i] = AngleData.mechanicalToElectricalAngle(angleAthwartship[i + iBegin], sensitivityAthwartship, offsetAthwartship);
            }
            powerData.setElectricAngles(electricalAngles);
         }
         pingData.add(powerData);
      }
   }
}
