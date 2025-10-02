package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.DefaultPing;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.TransmitMode;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.Utils;
import no.imr.tools.geo.Earth;
import no.imr.tools.time.NTDate;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.awt.geom.Point2D;
import java.nio.file.Path;
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

   private volatile @Nullable PingConfiguration pingConfiguration;
   private long ntStartDate = DEFAULT_NT_DATE_START;
   private long firstPingNumber = 1;
   private int pingCount = 100000;

   protected SyntheticData() {
   }

   protected PingConfiguration createPingConfiguration() {
      return new PingConfiguration(SyntheticFactory.createRawFileConfiguration(this));
   }

   public final PingConfiguration getPingConfiguration() {
      PingConfiguration result = pingConfiguration;
      if (result == null) {
         result = createPingConfiguration();
         pingConfiguration = result;
      }
      return result;
   }

   public RawFileConfiguration getRawFileConfiguration() {
      return getPingConfiguration().getRawFileConfiguration();
   }

   //-----------------------------------------------------------------------------
   // Definitions for RawFileConfiguration

   protected float[] getFrequencies() {
      return DEFAULT_FREQUENCIES;
   }

   //-----------------------------------------------------------------------------
   // Definitions for PingIndex

   protected double getVesselDistance(long pingNumber) {
      return pingNumber * 5 / 1000.0;
   }

   protected @Nullable WrapAround getWrapAround() {
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

   //-----------------------------------------------------------------------------
   // Datagram creation

   public Bot0Datagram createBot0Datagram(PingIndex pingIndex) {
      return SyntheticFactory.createBot0Datagram(this, pingIndex);
   }

   public @Nullable PowerData createPowerData(PingIndex pingIndex, int channel) {
      if (hasPowerData(pingIndex, channel)) {
         return SyntheticFactory.createPowerData(this, pingIndex, channel);
      } else {
         return null;
      }
   }

   public Ping createPing(PingIndex pingIndex) {
      return createPing(pingIndex, createBot0Datagram(pingIndex));
   }

   public Ping createPing(PingIndex pingIndex, Bot0Datagram bot0Datagram) {
      return new DefaultPing(pingIndex, bot0Datagram, createPingData(pingIndex));
   }

   public PingData createPingData(PingIndex pingIndex) {
      PingData pingData = new PingData(getPingConfiguration());
      for (int channel = 1; channel <= getRawFileConfiguration().getTransducerCount(); channel++) {
         PowerData powerData = createPowerData(pingIndex, channel);
         if (powerData != null) {
            pingData.add(powerData);
         }
      }
      addOtherDatagrams(pingIndex, pingData);
      return pingData;
   }

   /**
    * Optionally add datagrams other than {@link PowerData}.
    *
    * @param pingIndex the ping index of the ping to add datagrams to
    * @param pingData  the ping data to add datagrams to
    */
   protected void addOtherDatagrams(PingIndex pingIndex, PingData pingData) {
   }

   public long getFirstPingNumber() {
      return firstPingNumber;
   }

   public long getLastPingNumber() {
      return firstPingNumber + pingCount - 1;
   }

   protected long getNTDate(long pingNumber) {
      return ntStartDate + pingNumber * 2 * NTDate.UNITS_PER_SECOND;
   }

   public void setFirstAndLastPingNumber(long first, long last) {
      pingConfiguration = null;
      firstPingNumber = first;
      pingCount = (int) (last - first) + 1;
   }

   public int getPingCount() {
      return pingCount;
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

   public PingIndex createPingIndex(long pingNumber) {
      return SyntheticFactory.createPingIndex(this, pingNumber);
   }

   public Path toFile(long firstPingNumber, long lastPingNumber) {
      setFirstAndLastPingNumber(firstPingNumber, lastPingNumber);
      return new SyntheticDataFile(this).getFile();
   }

   public SegmentHandle toSegmentHandle(long firstPingNumber, long lastPingNumber) {
      setFirstAndLastPingNumber(firstPingNumber, lastPingNumber);
      return new SyntheticSegmentHandle(new SyntheticDataFile(this));
   }

   public SyntheticPingReader toPingReader() {
      return new SyntheticPingReader(this);
   }
}
