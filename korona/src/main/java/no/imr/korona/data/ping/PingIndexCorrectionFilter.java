package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Pin0Datagram;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.util.Nmea;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.geo.Earth;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Corrects the vessel distance and geographical position from {@link NmeaPingItem}.
 */
public final class PingIndexCorrectionFilter implements PingSource {
   private static final double MIN_SPEED = 0;
   private static final double MAX_SPEED = 100;

   private final PingSource pingSource;

   private final Deque<Ping> pingQueue = new ArrayDeque<>();
   private boolean triedInitializeGeoPos;

   private long lastNTDate;
   private boolean speedValid;
   private double speed;
   private double vesselDistance;
   private @Nullable GeoPoint geographicalPosition;
   private long timeOfLastGeoPos;
   private boolean hasSeenGGA;
   private boolean hasSeenNmeaWithGeoPos;
   private boolean hasSeenVesselDistance;
   private boolean triedInitializeVesselDistance;
   private boolean allowBuffering = true;

   private PingIndexCorrectionOptions pingIndexCorrectionOptions = new PingIndexCorrectionOptions();

   public PingIndexCorrectionFilter(PingSource pingSource) {
      this.pingSource = pingSource;
   }

   public void setAllowBuffering(boolean allowBuffering) {
      this.allowBuffering = allowBuffering;
   }

   public void setPingIndexCorrectionOptions(PingIndexCorrectionOptions pingIndexCorrectionOptions) {
      this.pingIndexCorrectionOptions = pingIndexCorrectionOptions;
   }

   public void initFromPingConfiguration() {
      getPingConfiguration().getConfigurationItems(NmeaPingItem.class).forEach(nmeaPingItem -> {
         updateSpeed(nmeaPingItem);
         updateGeographicalPosition(nmeaPingItem);
         if (pingIndexCorrectionOptions.useNmeaVesselDistance()) {
            nmeaPingItem.getVesselDistance().ifPresent(distance -> {
               vesselDistance = distance;
               hasSeenVesselDistance = true;
            });
         }
      });
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingSource.getPingConfiguration();
   }

   @Override
   public @Nullable Ping nextPing(AsyncHandle asyncHandle) throws IOException {
      if (!triedInitializeGeoPos) {
         triedInitializeGeoPos = true;
         while (geographicalPosition == null && canBufferMore()) {
            Ping ping = pingSource.nextPing(asyncHandle);
            if (ping == null) {
               break;
            }
            pingQueue.add(ping);
            ping.getPingItems(NmeaPingItem.class).forEach(this::updateGeographicalPosition);
         }
      }

      Ping ping;
      if (pingQueue.isEmpty()) {
         ping = pingSource.nextPing(asyncHandle);
      } else {
         ping = pingQueue.remove();
      }

      if (ping == null) {
         return null;
      }

      ping.getPingItems(NmeaPingItem.class).forEach(nmeaPingItem -> {
         updateSpeed(nmeaPingItem);
         updateGeographicalPosition(nmeaPingItem);
      });

      if (!hasSeenNmeaWithGeoPos) {
         // todo: Do this more generally, e.g. use interface GeoPosProviderPingItem.
         ping.getPingItems(Pin0Datagram.class).forEach(pin0Datagram -> {
            geographicalPosition = new GeoPoint(pin0Datagram.longitude, pin0Datagram.latitude);
         });
      }

      updateVesselDistance(ping, asyncHandle);

      PingIndex pingIndex = ping.getPingIndex();
      pingIndex.setVesselDistance(vesselDistance);
      pingIndex.setGeographicalPosition(geographicalPosition);

      lastNTDate = pingIndex.getNTDate();

      return ping;
   }

   @Override
   public void close() throws IOException {
      pingSource.close();
   }

   private boolean canBufferMore() {
      if (!allowBuffering) {
         return false;
      }
      if (pingQueue.size() >= 100) {
         return false;
      }
      if (pingQueue.isEmpty()) {
         return true;
      }
      return pingQueue.getLast().getTimeInMillis() - pingQueue.getFirst().getTimeInMillis() < 10_000;
   }

   private void updateSpeed(NmeaPingItem nmeaPingItem) {
      if (pingIndexCorrectionOptions.updateSpeedBasedOnGeoPositions()) {
         Nmea nmea = nmeaPingItem.getNmea();
         Optional<GeoPoint> geoPos = nmea.getGeographicalPosition();
         if (geographicalPosition != null && geoPos.isPresent() && canUseForGeoPos(nmea.getType())) {
            double meter = Earth.getApproximateDistance(geographicalPosition, geoPos.get());
            long ntDateDiff = nmeaPingItem.getNTDate() - timeOfLastGeoPos;
            if (ntDateDiff <= 0) {
               return;
            }
            double seconds = (double) ntDateDiff / (double) NTDate.UNITS_PER_SECOND;
            updateSpeed(meter / seconds);
         }
      } else {
         nmeaPingItem.getMeterPerSec().ifPresent(this::updateSpeed);
      }
   }

   private void updateSpeed(double newSpeed) {
      if (newSpeed >= MIN_SPEED && newSpeed <= MAX_SPEED) {
         speed = newSpeed;
         speedValid = true;
      }
   }

   private void updateGeographicalPosition(NmeaPingItem nmeaPingItem) {
      Nmea nmea = nmeaPingItem.getNmea();
      Optional<GeoPoint> geoPos = nmea.getGeographicalPosition();
      if (geoPos.isPresent() && canUseForGeoPos(nmea.getType())) {
         hasSeenNmeaWithGeoPos = true;
         geographicalPosition = geoPos.get();
         timeOfLastGeoPos = nmeaPingItem.getNTDate();
      }
   }

   private boolean canUseForGeoPos(Nmea.Type type) {
      if (hasSeenGGA) {
         // If we have seen GGA then we only use GGA
         return type == Nmea.Type.GGA;
      }
      hasSeenGGA = type == Nmea.Type.GGA;
      return true;
   }

   private void updateVesselDistance(Ping ping, AsyncHandle asyncHandle) throws IOException {
      if (pingIndexCorrectionOptions.useNmeaVesselDistance()) {
         OptionalDouble distance = getVesselDistance(ping);
         if (distance.isPresent()) {
            vesselDistance = distance.getAsDouble();
            hasSeenVesselDistance = true;
            return;
         }

         if (!triedInitializeVesselDistance) {
            triedInitializeVesselDistance = true;
            if (hasSeenVesselDistance && vesselDistance == 0 && nextPingWithVesselDistance(asyncHandle) == null) {
               hasSeenVesselDistance = false;
            }
         }

         if (hasSeenVesselDistance) {
            Ping nextPingWithVesselDistance = nextPingWithVesselDistance(asyncHandle);
            if (nextPingWithVesselDistance != null) {
               getVesselDistance(nextPingWithVesselDistance).ifPresent(nextVesselDistance -> {
                  double a = (double) (ping.getNTDate() - lastNTDate) / (nextPingWithVesselDistance.getNTDate() - lastNTDate);
                  vesselDistance = (1 - a) * vesselDistance + a * nextVesselDistance;
               });
            }
            return;
         }
      }

      if (lastNTDate != 0 && speedValid) {
         long ntDateDiff = ping.getNTDate() - lastNTDate;
         double seconds = (double) ntDateDiff / (double) NTDate.UNITS_PER_SECOND;
         double meter = speed * seconds;
         vesselDistance += Utils.meterToNmi(meter);
      }
   }

   private static OptionalDouble getVesselDistance(Ping ping) {
      return ping.getPingItems(NmeaPingItem.class)
            .flatMapToDouble(nmea -> nmea.getVesselDistance().stream())
            .findFirst();
   }

   private @Nullable Ping nextPingWithVesselDistance(AsyncHandle asyncHandle) throws IOException {
      for (Ping ping : pingQueue) {
         if (getVesselDistance(ping).isPresent()) {
            return ping;
         }
      }
      while (canBufferMore()) {
         Ping ping = pingSource.nextPing(asyncHandle);
         if (ping == null) {
            break;
         }
         pingQueue.add(ping);
         if (getVesselDistance(ping).isPresent()) {
            return ping;
         }
      }
      return null;
   }
}
