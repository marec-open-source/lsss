package no.imr.korona.data.formats.ek500;

import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.io.FileUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * EK500 datagram factory.
 */
final class EK500DatagramFactory {
   private EK500DatagramFactory() {
   }

   static RawFileConfiguration createRawFileConfiguration(Instant instant, EK500Settings ek500Settings, EK500FileSet ek500FileSet, List<InfoRecord> infoRecords) {
      RawFileConfiguration rawFileConfiguration = new RawFileConfiguration(instant);
      rawFileConfiguration.setDataFile(ek500FileSet.getMainFile().resolveSibling(ek500FileSet.getNSS() + "-~-" + ek500FileSet.getDateTime() + "-~"));
      rawFileConfiguration.setSurveyName(ek500FileSet.getNSS());
      rawFileConfiguration.setTransectName("EK500 Transect ???");
      rawFileConfiguration.setSounderName("EK500 Sounder ???");
      rawFileConfiguration.setVersion("EK500 Version ???");
      for (InfoRecord infoRecord : infoRecords) {
         float frequency = ek500Settings.roundToConfiguredFrequency(infoRecord.getFrequency());
         EK500TransducerSettings ek500TransducerSettings = ek500Settings.getEK500TransducerSettings(frequency, NoticeHandler.ignore());
         rawFileConfiguration.getTransducers().add(createRawFileTransducer(frequency, ek500TransducerSettings));
      }
      return rawFileConfiguration;
   }

   private static RawFileTransducer createRawFileTransducer(float frequency, EK500TransducerSettings ek500TransducerSettings) {
      RawFileTransducer transducer = new RawFileTransducer();
      transducer.setFrequency(frequency);
      transducer.setChannelId("EK500: " + KoronaUtils.hzToKHz(frequency) + " kHz");
      transducer.setBeamType(1);
      transducer.setGainAndGainTable(ek500TransducerSettings.gain.getFloatValue());
      transducer.setEquivalentBeamAngle(ek500TransducerSettings.equivalentBeamAngle.getFloatValue());
      return transducer;
   }

   static PowerData createPowerData(EK500TransducerSettings ek500TransducerSettings, RawFileConfiguration rawFileConfiguration, Instant instant, short channel, IndexRecord indexRecord, FileChannel fileChannel, ByteBuffer byteBuffer) throws IOException {
      PowerData powerData = new PowerData(instant);

      read(fileChannel, indexRecord.pelagicOffset, 2 * indexRecord.pelagicCount, byteBuffer);

      powerData.setChannel(channel);
      powerData.setTransducerDepth(findTransducerDepth(indexRecord, byteBuffer));
      powerData.setFrequency(rawFileConfiguration.getTransducers().get(channel - 1).getFrequency());
      powerData.setTransmitPower(ek500TransducerSettings.transmitPower.getFloatValue());
      powerData.setPulseDuration(ek500TransducerSettings.pulseLength.getFloatValue());
      powerData.setBandWidth(ek500TransducerSettings.bandWidth.getFloatValue());
      //powerData.setSampleInterval() see setSampleDistanceAndCount
      powerData.setSoundVelocity(ek500TransducerSettings.soundVelocity.getFloatValue());
      powerData.setAbsorptionCoefficient(ek500TransducerSettings.absorptionCoefficient.getFloatValue());
      //powerData.setHeave() ?;
      //powerData.setRoll() ?;
      //powerData.setPitch() ?;
      //powerData.setTemperature() ?;
      //powerData.setTrawlUpperDepthValid() ?;
      //powerData.setTrawlOpeningValid() ?;
      //powerData.setTrawlUpperDepth() ?;
      //powerData.setTrawlOpening() ?;
      powerData.setOffset(0);
      //powerData.setCount() see setSampleDistanceAndCount

      readData(powerData, indexRecord, fileChannel, byteBuffer);

      return powerData;
   }

   private static void readData(PowerData powerData, IndexRecord indexRecord, FileChannel fileChannel, ByteBuffer byteBuffer) throws IOException {
      if (indexRecord.pelagicAndBottomOverlap() && indexRecord.bottomDepth != 0) {
         float datagramSampleDistance = indexRecord.getBottomEchogramSampleDistance();
         powerData.setSampleDistance(datagramSampleDistance);

         int bottomDataBeginIndex = powerData.depthToSampleIndex(indexRecord.getBottomEchogramMinDepth());
         int bottomDataEndIndex = bottomDataBeginIndex + indexRecord.bottomCount;
         int pelagicDataEndIndex = powerData.depthToSampleIndex(indexRecord.pelagicLower);
         powerData.setCount(Math.max(pelagicDataEndIndex, bottomDataEndIndex));

         parseAndResampleData(powerData, byteBuffer, indexRecord.getPelagicEchogramSampleDistance() / datagramSampleDistance);

         read(fileChannel, indexRecord.bottomOffset, 2 * indexRecord.bottomCount, byteBuffer);
         parseData(powerData, byteBuffer, Math.max(0, bottomDataBeginIndex), bottomDataEndIndex);
      } else {
         powerData.setSampleDistance(indexRecord.getPelagicEchogramSampleDistance());
         powerData.setCount(powerData.depthToSampleIndex(indexRecord.pelagicLower));
         parseData(powerData, byteBuffer, 0, powerData.getCount());
      }
   }

   private static float findTransducerDepth(IndexRecord indexRecord, ByteBuffer byteBuffer) {
      int transducerIndex = 0;
      for (int i = 0; i < indexRecord.pelagicCount; i++) {
         short isv = byteBuffer.getShort();
         if (isv > EK500Settings.DATA_THRESHOLD) {
            transducerIndex = i;
            break;
         }
      }
      byteBuffer.position(2 * transducerIndex);
      return indexRecord.pelagicUpper + transducerIndex * indexRecord.getPelagicEchogramSampleDistance();
   }

   private static void read(FileChannel fileChannel, int fileOffset, int byteCount, ByteBuffer byteBuffer) throws IOException {
      byteBuffer.clear();
      byteBuffer.limit(byteCount);
      FileUtils.read(fileChannel, byteBuffer, fileOffset);
      if (byteBuffer.hasRemaining()) {
         throw new IOException("Read too few bytes");
      }
      byteBuffer.flip();
   }

   private static void parseData(PowerData powerData, ByteBuffer byteBuffer, int begin, int end) {
      float[] logSv = powerData.getLogSv();
      for (int i = begin; i < end; i++) {
         short isv = byteBuffer.getShort();
         logSv[i] = isv * PowerData.POWER2DB;
      }
   }

   private static void parseAndResampleData(PowerData powerData, ByteBuffer byteBuffer, float sampleDistanceRatio) {
      int iLogSvEnd = 0;
      float[] logSv = powerData.getLogSv();
      for (int i = 0; byteBuffer.hasRemaining(); i++) {
         short isv = byteBuffer.getShort();

         int iLogSvBegin = iLogSvEnd;
         iLogSvEnd = Math.round((i + 1) * sampleDistanceRatio);
         Arrays.fill(logSv, iLogSvBegin, iLogSvEnd, isv * PowerData.POWER2DB);
      }
   }
}
