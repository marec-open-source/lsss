package no.imr.korona.util.transform;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.util.DataUtils;
import no.imr.tools.geo.Earth;
import no.imr.tools.math.linalg.Matrix3;
import no.imr.tools.math.linalg.TRS;
import no.imr.tools.math.linalg.Vec3;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.util.OptionalDouble;

/**
 * Creates a transformation from the local coordinate system of a ping to a reference coordinate system.
 * Implementations must implement a method for calculating heading if NMEA heading is not
 * available for current ping.
 */
public abstract class PingTransform {
   private final @Nullable GeoPoint referenceGeoPos;
   private final GeoPoint metersPerGeoDegree;

   /**
    * Creates a transform with a reference coordinate system with the origin at given location and no rotation.
    *
    * @param referenceGeoPos the horizontal reference position
    */
   protected PingTransform(@Nullable GeoPoint referenceGeoPos) {
      this.referenceGeoPos = referenceGeoPos;

      if (referenceGeoPos != null) {
         metersPerGeoDegree = Earth.getMetersPerGeoDegree(referenceGeoPos);
      } else {
         metersPerGeoDegree = new GeoPoint(0, 0);
      }
   }

   protected PingTransform(Ping referencePing) {
      this(referencePing.getPingIndex().getGeographicalPosition());
   }

   public Vec3 transform(PingIndex pingIndex, float heave) {
      GeoPoint geoPos = pingIndex.getGeographicalPosition();
      return transform(geoPos, heave);
   }

   public Vec3 transform(@Nullable GeoPoint geoPos, float heave) {
      float dx;
      float dy;
      if (geoPos != null && referenceGeoPos != null) {
         dx = (float) ((geoPos.getX() - referenceGeoPos.getX()) * metersPerGeoDegree.getX());
         dy = (float) ((geoPos.getY() - referenceGeoPos.getY()) * metersPerGeoDegree.getY());
      } else {
         dx = 0;
         dy = 0;
      }
      float dz = -heave;

      return new Vec3(dx, dy, dz);
   }

   public @Nullable GeoPoint getReferenceGeoPos() {
      return referenceGeoPos;
   }

   public GeoPoint getMetersPerGeoDegree() {
      return metersPerGeoDegree;
   }

   public @Nullable GeoPoint toGeoPos(Vec3 pos) {
      if (referenceGeoPos == null) {
         return null;
      }
      return new GeoPoint(
            referenceGeoPos.x + pos.x() / metersPerGeoDegree.x,
            referenceGeoPos.y + pos.y() / metersPerGeoDegree.y
      );
   }

   public TRS getTransform(Ping ping) {
      ChannelData channelData = ping.getFirstAvailableChannelData();
      if (channelData == null) {
         return TRS.IDENTITY;
      }
      Vec3 translation = transform(ping.getPingIndex(), channelData.getHeave());
      Matrix3 rotation = getRotation(ping, channelData);
      return new TRS(translation, rotation);
   }

   public TRS getHeadingTransform(Ping ping) {
      Vec3 translation = transform(ping.getPingIndex(), 0);
      Matrix3 rotation = getHeadingRotation(ping);
      return new TRS(translation, rotation);
   }

   private Matrix3 getRotation(Ping ping, ChannelData channelData) {
      Matrix3 headingRotation = getHeadingRotation(ping);

      Matrix3 rollRotation = Matrix3.createRotation(channelData.getRoll(), new Vec3(1, 0, 0));
      Matrix3 pitchRotation = Matrix3.createRotation(-channelData.getPitch(), new Vec3(0, 1, 0));

      return headingRotation.multiply(pitchRotation).multiply(rollRotation);
   }

   private Matrix3 getHeadingRotation(Ping ping) {
      OptionalDouble heading = DataUtils.getHeadingFromNmea(ping);
      if (heading.isPresent()) {
         return Matrix3.createRotation(90 - heading.getAsDouble(), new Vec3(0, 0, 1));
      } else {
         return fallbackHeadingCalculation(ping);
      }
   }

   protected abstract Matrix3 fallbackHeadingCalculation(Ping ping);
}
