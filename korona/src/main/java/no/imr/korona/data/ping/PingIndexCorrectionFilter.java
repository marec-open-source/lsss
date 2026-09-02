package no.imr.korona.data.ping;

import no.imr.korona.data.datagrams.Pin0Datagram;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.util.Nmea;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.geo.Earth;
import no.imr.tools.math.MathUtils;
import no.imr.tools.time.TimeUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Corrects the vessel distance and geographical position from {@link NmeaPingItem}.
 */
public final class PingIndexCorrectionFilter implements PingSource {
   private static final double MAX_SPEED_KNOTS = 100;

   private final PingSource pingSource;

   private final Deque<Ping> pingQueue = new ArrayDeque<>();
   private boolean triedInitializeGeoPos;

   private @Nullable Instant lastInstant;
   private boolean speedValid;
   private double speedKnots;
   private double vesselDistance;
   private @Nullable GeoPoint geographicalPosition;
   private @Nullable Instant timeOfLastGeoPos;
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

      lastInstant = pingIndex.getInstant();

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
      return pingQueue.getFirst().getInstant().until(pingQueue.getLast().getInstant(), ChronoUnit.SECONDS) < 10;
   }

   private void updateSpeed(NmeaPingItem nmeaPingItem) {
      if (pingIndexCorrectionOptions.updateSpeedBasedOnGeoPositions()) {
         Nmea nmea = nmeaPingItem.getNmea();
         Optional<GeoPoint> geoPos = nmea.getGeographicalPosition();
         if (geographicalPosition != null && timeOfLastGeoPos != null && geoPos.isPresent() && canUseForGeoPos(nmea.getType())) {
            double secondsDiff = TimeUtils.toSeconds(timeOfLastGeoPos, nmeaPingItem.getInstant());
            if (secondsDiff <= 0) {
               return;
            }
            double meter = Earth.getApproximateDistance(geographicalPosition, geoPos.get());
            double knots = KoronaUtils.meterPerSecondToKnots(meter / secondsDiff);
            updateSpeed(knots);
         }
      } else {
         nmeaPingItem.getKnots().ifPresent(this::updateSpeed);
      }
   }

   private void updateSpeed(double newSpeedKnots) {
      if (newSpeedKnots >= 0 && newSpeedKnots <= MAX_SPEED_KNOTS) {
         speedKnots = newSpeedKnots;
         speedValid = true;
      }
   }

   private void updateGeographicalPosition(NmeaPingItem nmeaPingItem) {
      Nmea nmea = nmeaPingItem.getNmea();
      Optional<GeoPoint> geoPos = nmea.getGeographicalPosition();
      if (geoPos.isPresent() && canUseForGeoPos(nmea.getType())) {
         hasSeenNmeaWithGeoPos = true;
         geographicalPosition = geoPos.get();
         timeOfLastGeoPos = nmeaPingItem.getInstant();
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

         if (lastInstant != null && hasSeenVesselDistance) {
            Ping nextPingWithVesselDistance = nextPingWithVesselDistance(asyncHandle);
            if (nextPingWithVesselDistance != null) {
               getVesselDistance(nextPingWithVesselDistance).ifPresent(nextVesselDistance -> {
                  double a = TimeUtils.toSeconds(lastInstant, ping.getInstant()) / TimeUtils.toSeconds(lastInstant, nextPingWithVesselDistance.getInstant());
                  vesselDistance = MathUtils.interpolate(vesselDistance, nextVesselDistance, a);
               });
            }
            return;
         }
      }

      if (lastInstant != null && speedValid) {
         double secondsDiff = TimeUtils.toSeconds(lastInstant, ping.getInstant());
         double hours = secondsDiff / 3600.0;
         double nmi = speedKnots * hours;
         vesselDistance += nmi;
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
