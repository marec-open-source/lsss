package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

/**
 * Creates synthetic datagrams.
 */
public final class SyntheticFactory {
   private SyntheticFactory() {
   }

   static PingIndex createPingIndex(SyntheticData syntheticData, long pingNumber) {
      return new DefaultPingIndex(syntheticData.getNTDate(pingNumber),
            pingNumber,
            syntheticData.getVesselDistance(pingNumber),
            syntheticData.getGeographicalPosition(pingNumber));
   }

   static Bot0Datagram createBot0Datagram(SyntheticData syntheticData, PingIndex pingIndex) {
      double[] channelDepths = new double[syntheticData.getTransducerCount()];
      for (int channelIndex = 0; channelIndex < channelDepths.length; channelIndex++) {
         channelDepths[channelIndex] = syntheticData.getBottomDepth(pingIndex, channelIndex + 1);
      }
      return new Bot0Datagram(pingIndex.getNTDate(), channelDepths);
   }

   public static RawFileConfiguration createRawFileConfiguration(SyntheticDataFile syntheticDataFile) {
      SyntheticData syntheticData = syntheticDataFile.getSyntheticData();
      long ntDate = syntheticData.getNTDate(syntheticDataFile.getFirstPingNumber());
      RawFileConfiguration rawFileConfiguration = new RawFileConfiguration(ntDate);
      rawFileConfiguration.setSurveyName("LSSS synthetic survey");
      rawFileConfiguration.setTransectName("LSSS synthetic transect");
      rawFileConfiguration.setSounderName("LSSS synthetic sounder");
      rawFileConfiguration.setVersion("LSSS synthetic version");
      int transducerCount = syntheticData.getTransducerCount();
      for (int channel = 1; channel <= transducerCount; channel++) {
         rawFileConfiguration.getTransducers().add(createRawFileTransducer(syntheticData.getFrequency(channel)));
      }
      rawFileConfiguration.setDataFile(syntheticDataFile.toFile());
      return rawFileConfiguration;
   }

   private static RawFileTransducer createRawFileTransducer(float frequency) {
      RawFileTransducer transducer = new RawFileTransducer();
      transducer.setFrequency(frequency);
      transducer.setChannelId("Synthetic channel " + transducer.getKHz() + " kHz");
      transducer.setGainAndGainTable(22.31f);
      transducer.setEquivalentBeamAngle(-17.3f);

      //Settings from G.O.Sars D20031026-T040600.raw

      transducer.setBeamType(1);
      transducer.setBeamWidthAlongship(10.96f);
      transducer.setBeamWidthAthwartship(10.55f);
      transducer.setAngleSensitivityAlongship(13.9f);
      transducer.setAngleSensitivityAthwartship(13.9f);
      transducer.setAngleOffsetAlongship(-0.01f);
      transducer.setAngleOffsetAthwartship(-0.18f);

      setArray(transducer.getPulseDurationTable(), new float[]{5.12e-4f, 0.001024f, 0.002048f, 0.004096f, 0.008192f});
      setArray(transducer.getGainTable(), new float[]{21.66f, 22.31f, 22.61f, 22.6f, 22.6f});
      setArray(transducer.getSaCorrectionTable(), new float[]{-0.59f, -0.65f, -0.45f, -0.4f, -0.35f});

      return transducer;
   }

   private static void setArray(float[] array, float[] values) {
      System.arraycopy(values, 0, array, 0, array.length);
   }

   static PowerData createPowerData(SyntheticDataFile syntheticDataFile, PingIndex pingIndex, int channel) {
      SyntheticData syntheticData = syntheticDataFile.getSyntheticData();
      PowerData powerData = new PowerData(pingIndex.getNTDate());
      powerData.setChannel(channel);
      powerData.setFrequency(syntheticData.getFrequency(channel));
      powerData.setTransmitPower(syntheticData.getTransmitPower(pingIndex, channel));
      powerData.setTransmitMode(syntheticData.getTransmitMode(pingIndex, channel));
      powerData.setAbsorptionCoefficient(syntheticData.getAbsorptionCoefficient(pingIndex, channel));
      powerData.setSoundVelocity(syntheticData.getSoundVelocity(pingIndex, channel));
      powerData.setSampleInterval(syntheticData.getSampleInterval(pingIndex, channel));
      powerData.setPulseDuration(syntheticData.getPulseDuration(pingIndex, channel));
      powerData.setEffectivePulseDuration(syntheticData.getEffectivePulseDuration(pingIndex, channel));
      powerData.setTransducerDepth(syntheticData.getTransducerDepth(pingIndex, channel));
      powerData.setHeave(syntheticData.getHeave(pingIndex));
      powerData.setRoll(syntheticData.getRoll(pingIndex));
      powerData.setPitch(syntheticData.getPitch(pingIndex));

      powerData.setPingConfiguration(syntheticDataFile.getPingConfiguration());

      syntheticData.defineSampleValues(powerData, pingIndex);

      return powerData;
   }
}
