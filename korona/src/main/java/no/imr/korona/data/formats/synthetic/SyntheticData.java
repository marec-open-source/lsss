package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.geo.Earth;
import no.imr.tools.time.TimeUtils;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Base class for definitions of synthetic echosounder data.
 * This class has reasonable settings for all values except sample values,
 * so subclasses must override {@link #defineSampleValues(PowerData, PingIndex)}.
 */
public abstract class SyntheticData {
   private static final Instant DEFAULT_START_INSTANT = Instant.EPOCH;
   private static final DateTimeFormatter DATE_TIME_FORMATTER = TimeUtils.createUTCDateTimeFormatter("yyyyMMdd-HHmmss");
   private static final String PARAMETER_TIME = "time";

   protected static final GeoPoint REFERENCE_GEO_POS = new GeoPoint(3, 60);
   private static final float[] DEFAULT_FREQUENCIES = {
         18_000,
         38_000,
         70_000,
         120_000,
         200_000,
         364_000,
   };

   private Instant startInstant = DEFAULT_START_INSTANT;

   protected SyntheticData() {
   }

   public PingConfiguration createPingConfiguration(SyntheticDataFile syntheticDataFile) {
      return new PingConfiguration(SyntheticFactory.createRawFileConfiguration(syntheticDataFile));
   }

   //-----------------------------------------------------------------------------
   // Definitions for RawFileConfiguration

   public int getTransducerCount() {
      return DEFAULT_FREQUENCIES.length;
   }

   public float getFrequency(int channel) {
      return DEFAULT_FREQUENCIES[channel - 1];
   }

   //-----------------------------------------------------------------------------
   // Definitions for PingIndex

   public double getVesselDistance(long pingNumber) {
      return pingNumber * 5 / 1000.0;
   }

   public @Nullable WrapAround getWrapAround(SyntheticDataFile syntheticDataFile) {
      return null;
   }

   public GeoPoint getGeographicalPosition(long pingNumber) {
      double meters = KoronaUtils.nmiToMeter(getVesselDistance(pingNumber));
      double r = 100_000;
      double s = meters % (2 * Math.PI * r);
      double a = s / r;
      double x = r * Math.sin(a);
      double y = r * Math.cos(a);
      return Earth.getApproximateGeoPoint(REFERENCE_GEO_POS, new Point2D.Double(x, y));
   }

   //-----------------------------------------------------------------------------
   // Definitions for Bot0Datagram

   public float getBottomDepth(PingIndex pingIndex, int channel) {
      return 100;
   }

   //--------------------------------------------------------------------------
   // Definitions for PowerData

   public float getAbsorptionCoefficient(PingIndex pingIndex, int channel) {
      return 0.00267f;
   }

   public float getSoundVelocity(PingIndex pingIndex, int channel) {
      return 1491;
   }

   public float getSampleInterval(PingIndex pingIndex, int channel) {
      return 2.56e-4f;
   }

   public float getPulseDuration(PingIndex pingIndex, int channel) {
      return 0.001024f;
   }

   public float getEffectivePulseDuration(PingIndex pingIndex, int channel) {
      return PowerData.MISSING_EFFECTIVE_PULSE_DURATION;
   }

   public float getTransducerDepth(PingIndex pingIndex, int channel) {
      return 7.5f;
   }

   public float getTransmitPower(PingIndex pingIndex, int channel) {
      return 2000;
   }

   public short getTransmitMode(PingIndex pingIndex, int channel) {
      return TransmitMode.ACTIVE;
   }

   public float getHeave(PingIndex pingIndex) {
      return 0;
   }

   public float getRoll(PingIndex pingIndex) {
      return 0;
   }

   public float getPitch(PingIndex pingIndex) {
      return 0;
   }

   public float getHeading(PingIndex pingIndex) {
      return 0;
   }

   //-----------------------------------------------------------------------------
   // Definitions for sample values.

   public abstract void defineSampleValues(PowerData powerData, PingIndex pingIndex);

   //-----------------------------------------------------------------------------
   // Definitions for ping

   public boolean hasPowerData(PingIndex pingIndex, int channel) {
      return true;
   }

   /**
    * Optionally add datagrams other than {@link PowerData}.
    *
    * @param pingIndex the ping index of the ping to add datagrams to
    * @param pingData  the ping data to add datagrams to
    */
   public void addOtherDatagrams(PingIndex pingIndex, PingData pingData) {
   }

   public Instant getInstant(long pingNumber) {
      return startInstant.plusSeconds(pingNumber * 2);
   }

   public SyntheticDataFile withFirstAndLastPingNumber(long first, long last) {
      return new SyntheticDataFile(this, first, (int) (last - first) + 1);
   }

   public void addParameters(Map<String, String> parameters) {
      if (!startInstant.equals(DEFAULT_START_INSTANT)) {
         parameters.put(PARAMETER_TIME, DATE_TIME_FORMATTER.format(startInstant));
      }
   }

   public void setParameter(String name, String value) {
      if (name.equals(PARAMETER_TIME)) {
         startInstant = DATE_TIME_FORMATTER.parse(value, Instant::from);
      } else {
         throw new IllegalArgumentException(name + "=" + value);
      }
   }
}
