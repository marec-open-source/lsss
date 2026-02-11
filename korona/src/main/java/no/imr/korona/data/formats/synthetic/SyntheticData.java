package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.tools.Utils;
import no.imr.tools.geo.Earth;
import no.imr.tools.time.NTDate;
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
   private static final long DEFAULT_NT_DATE_START = NTDate.timeInMillisToNTDate(0);
   private static final DateTimeFormatter DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("yyyyMMdd-HHmmss");
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

   private long ntStartDate = DEFAULT_NT_DATE_START;

   protected SyntheticData() {
   }

   protected PingConfiguration createPingConfiguration(SyntheticDataFile syntheticDataFile) {
      return new PingConfiguration(SyntheticFactory.createRawFileConfiguration(syntheticDataFile));
   }

   //-----------------------------------------------------------------------------
   // Definitions for RawFileConfiguration

   protected int getTransducerCount() {
      return DEFAULT_FREQUENCIES.length;
   }

   protected float getFrequency(int channel) {
      return DEFAULT_FREQUENCIES[channel - 1];
   }

   //-----------------------------------------------------------------------------
   // Definitions for PingIndex

   protected double getVesselDistance(long pingNumber) {
      return pingNumber * 5 / 1000.0;
   }

   protected @Nullable WrapAround getWrapAround(SyntheticDataFile syntheticDataFile) {
      return null;
   }

   protected GeoPoint getGeographicalPosition(long pingNumber) {
      double meters = Utils.nmiToMeter(getVesselDistance(pingNumber));
      double r = 100_000;
      double s = meters % (2 * Math.PI * r);
      double a = s / r;
      double x = r * Math.sin(a);
      double y = r * Math.cos(a);
      return Earth.getApproximateGeoPoint(REFERENCE_GEO_POS, new Point2D.Double(x, y));
   }

   //-----------------------------------------------------------------------------
   // Definitions for Bot0Datagram

   protected float getBottomDepth(PingIndex pingIndex, int channel) {
      return 100;
   }

   //--------------------------------------------------------------------------
   // Definitions for PowerData

   protected float getAbsorptionCoefficient(PingIndex pingIndex, int channel) {
      return 0.00267f;
   }

   protected float getSoundVelocity(PingIndex pingIndex, int channel) {
      return 1491;
   }

   protected float getSampleInterval(PingIndex pingIndex, int channel) {
      return 2.56e-4f;
   }

   protected float getPulseDuration(PingIndex pingIndex, int channel) {
      return 0.001024f;
   }

   protected float getEffectivePulseDuration(PingIndex pingIndex, int channel) {
      return PowerData.MISSING_EFFECTIVE_PULSE_DURATION;
   }

   protected float getTransducerDepth(PingIndex pingIndex, int channel) {
      return 7.5f;
   }

   protected float getTransmitPower(PingIndex pingIndex, int channel) {
      return 2000;
   }

   protected short getTransmitMode(PingIndex pingIndex, int channel) {
      return TransmitMode.ACTIVE;
   }

   protected float getHeave(PingIndex pingIndex) {
      return 0;
   }

   protected float getRoll(PingIndex pingIndex) {
      return 0;
   }

   protected float getPitch(PingIndex pingIndex) {
      return 0;
   }

   //-----------------------------------------------------------------------------
   // Definitions for sample values.

   protected abstract void defineSampleValues(PowerData powerData, PingIndex pingIndex);

   //-----------------------------------------------------------------------------
   // Definitions for ping

   protected boolean hasPowerData(PingIndex pingIndex, int channel) {
      return true;
   }

   /**
    * Optionally add datagrams other than {@link PowerData}.
    *
    * @param pingIndex the ping index of the ping to add datagrams to
    * @param pingData  the ping data to add datagrams to
    */
   protected void addOtherDatagrams(PingIndex pingIndex, PingData pingData) {
   }

   protected long getNTDate(long pingNumber) {
      return ntStartDate + pingNumber * 2 * NTDate.UNITS_PER_SECOND;
   }

   public SyntheticDataFile withFirstAndLastPingNumber(long first, long last) {
      return new SyntheticDataFile(this, first, (int) (last - first) + 1);
   }

   protected void addParameters(Map<String, String> parameters) {
      if (ntStartDate != DEFAULT_NT_DATE_START) {
         parameters.put(PARAMETER_TIME, DATE_TIME_FORMATTER.format(Instant.ofEpochMilli(NTDate.ntDateToTimeInMillis(ntStartDate))));
      }
   }

   protected void setParameter(String name, String value) {
      if (name.equals(PARAMETER_TIME)) {
         ntStartDate = NTDate.timeInMillisToNTDate(DATE_TIME_FORMATTER.parse(value, Instant::from).toEpochMilli());
      } else {
         throw new IllegalArgumentException(name + "=" + value);
      }
   }
}
