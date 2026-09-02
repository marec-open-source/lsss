package no.imr.korona.data.formats.missing;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentData;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MissingSegmentData extends SegmentData {
   private final PingConfiguration pingConfiguration;
   private final List<PingIndex> pingIndices;
   private final List<Bot0Datagram> bot0Datagrams;

   public MissingSegmentData(PingConfiguration pingConfiguration, PingRange pingRange) {
      if (pingRange.isEmpty()) {
         throw new IllegalArgumentException("Empty ping range");
      }

      this.pingConfiguration = pingConfiguration;

      int pingCount = pingRange.getPingCount();
      pingIndices = new ArrayList<>(pingCount);
      bot0Datagrams = new ArrayList<>(pingCount);

      for (int i = 0; i < pingCount; i++) {
         long pingNumber = pingRange.begin().getPingNumber() + i;
         PingIndex pingIndex = MissingPingIndex.create(pingRange.begin(), pingRange.end(), pingNumber);
         pingIndices.add(pingIndex);
         bot0Datagrams.add(new MissingBot0Datagram(pingConfiguration.getRawFileConfiguration(), pingIndex));
      }
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public void close() {
   }

   @Override
   public List<PingIndex> getPingIndices() {
      return pingIndices;
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return null;
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) {
      PingData pingData = new PingData(pingConfiguration);
      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
         pingData.add(createPowerData(rawFileConfiguration, pingIndex, channel));
      }
      return pingData;
   }

   private static PowerData createPowerData(RawFileConfiguration rawFileConfiguration, PingIndex pingIndex, int channel) {
      PowerData powerData = new PowerData(pingIndex.getInstant());
      powerData.setChannel(channel);
      powerData.setFrequency(rawFileConfiguration.getTransducers().get(channel - 1).getFrequency());
      powerData.setTransmitPower(2000);
      powerData.setAbsorptionCoefficient(0.00267f);
      powerData.setSoundVelocity(1491);
      powerData.setSampleInterval(2.56e-4f);
      powerData.setPulseDuration(0.001024f);
      powerData.setTransducerDepth(0);
      powerData.setCount(0);
      powerData.setSv(Utils.EMPTY_FLOAT_ARRAY);
      return powerData;
   }
}
