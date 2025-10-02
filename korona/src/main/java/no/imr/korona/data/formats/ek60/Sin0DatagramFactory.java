package no.imr.korona.data.formats.ek60;

import no.imr.korona.data.datagrams.Phy0Datagram;
import no.imr.korona.data.datagrams.Sin0Datagram;
import no.imr.korona.data.datagrams.Ver0Datagram;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;

import java.util.Optional;

final class Sin0DatagramFactory {
   private Sin0DatagramFactory() {
   }

   static RawFileConfiguration createRawFileConfiguration(Sin0Datagram sin0Datagram, Ver0Datagram ver0Datagram, Phy0Datagram phy0Datagram) {
      RawFileConfiguration rawFileConfiguration = new RawFileConfiguration(sin0Datagram.getNTDate());
      rawFileConfiguration.setSurveyName("SIN0 Survey ???");
      rawFileConfiguration.setTransectName("SIN0 Transect ???");
      rawFileConfiguration.setSounderName(ver0Datagram.productName);
      rawFileConfiguration.setVersion(ver0Datagram.softwareVersion);
      rawFileConfiguration.setMultiplexing((short) 0); // todo ?
      rawFileConfiguration.setTimeBias(0); // todo ?
      rawFileConfiguration.setSoundVelocityAverage(0); // todo
      rawFileConfiguration.setSoundVelocityTransducer(0); // todo ?

      findSensor(phy0Datagram, "MRU").ifPresent(mru -> {
         rawFileConfiguration.setMRUOffset(mru.offset);
         rawFileConfiguration.setMRUAlpha(mru.rotation);
      });

      findSensor(phy0Datagram, "GPS").ifPresent(gps -> rawFileConfiguration.setGPSOffset(gps.offset));

      sin0Datagram.transceivers.stream()
            .map(Sin0DatagramFactory::createRawFileTransducer)
            .forEach(rawFileConfiguration.getTransducers()::add);

      return rawFileConfiguration;
   }

   private static Optional<Phy0Datagram.SensorPlatform> findSensor(Phy0Datagram phy0Datagram, String sensorName) {
      return Utils.getAllOfType(phy0Datagram.platforms, Phy0Datagram.SensorPlatform.class)
            .filter(sensor -> sensor.name.equals(sensorName))
            .findFirst();
   }

   private static RawFileTransducer createRawFileTransducer(Sin0Datagram.Transceiver transceiver) {
      RawFileTransducer transducer = new RawFileTransducer();
      transducer.setChannelId(transceiver.name);
      return transducer;
   }
}
