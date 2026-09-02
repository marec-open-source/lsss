package no.imr.korona.data.ping.items.channel;

import no.imr.korona.data.datagrams.PerChannelDatagram;
import no.imr.korona.data.datagrams.Raw0Datagram;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.util.TvgArray;
import no.imr.korona.data.util.TvgCache;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.range.FloatRange;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Common for all types of channel data.
 */
public abstract class ChannelData implements PingItem, PerChannelDatagram, no.marec.lsss.api.data.ChannelData {
   private static final PingConfiguration EMPTY_PING_CONFIGURATION = PingConfiguration.newEmpty();
   private static final RawFileTransducer EMPTY_RAW_FILE_TRANSDUCER = new RawFileTransducer();

   private Instant instant;
   private short channel; // Channel number
   // private short mode; // Power = 1, Angles = 2, BBT complex = 8
   private float transducerDepth; // [m]
   private float frequency; // [Hz]
   private float transmitPower; // [W]
   private float pulseDuration; // [s]
   private float bandWidth; // [Hz]
   private float sampleInterval; // [s]
   private float soundVelocity; // [m/s]
   private float absorptionCoefficient; // [dB/m]
   private float heave; // [m]
   private float roll; // [deg]
   private float pitch; // [deg]
   private float temperature; // [°C]
   private float heading;  // The heading of the ship [deg]    // RK 2010.11.19
   private short transmitMode;  // The transmit mode: 0=Active, 1=Passive, 2=Test, -1=Unknown  // RK 2010.11.19
   // private short spare;  // Nothing - just to fill up
   // private float sweep;  // The frequency sweep rate [Hz/s]  RK 2010.11.19, WIDEBAND WBT_COMPLEX
   private int offset; // Offset of first sample
   private int count; // Number of samples

   private PingConfiguration pingConfiguration = EMPTY_PING_CONFIGURATION;
   private RawFileTransducer transducer = EMPTY_RAW_FILE_TRANSDUCER;
   private @Nullable String readOnlyBecauseOfDataType;

   protected ChannelData(Instant instant) {
      this.instant = instant;
   }

   protected ChannelData(ChannelData channelData) {
      instant = channelData.instant;
      channel = channelData.channel;
      // mode = channelData.mode;
      transducerDepth = channelData.transducerDepth;
      frequency = channelData.frequency;
      transmitPower = channelData.transmitPower;
      pulseDuration = channelData.pulseDuration;
      bandWidth = channelData.bandWidth;
      sampleInterval = channelData.sampleInterval;
      soundVelocity = channelData.soundVelocity;
      absorptionCoefficient = channelData.absorptionCoefficient;
      heave = channelData.heave;
      roll = channelData.roll;
      pitch = channelData.pitch;
      temperature = channelData.temperature;
      heading = channelData.heading;
      transmitMode = channelData.transmitMode;
      // spare = channelData.spare;
      // sweep = channelData.sweep;
      offset = channelData.offset;
      count = channelData.count;

      pingConfiguration = channelData.pingConfiguration;
      transducer = channelData.transducer;
      readOnlyBecauseOfDataType = null;
      // Not real-only so that copies (e.g. created by makeCopy) can be modified.
   }

   protected ChannelData(Raw0Datagram raw0Datagram) {
      this(raw0Datagram.getInstant());

      channel = raw0Datagram.channel;
      // mode = raw0Datagram.mode;
      transducerDepth = raw0Datagram.transducerDepth;
      frequency = raw0Datagram.frequency;
      transmitPower = raw0Datagram.transmitPower;
      pulseDuration = raw0Datagram.pulseLength;
      bandWidth = raw0Datagram.bandWidth;
      sampleInterval = raw0Datagram.sampleInterval;
      soundVelocity = raw0Datagram.soundVelocity;
      absorptionCoefficient = raw0Datagram.absorptionCoefficient;
      heave = raw0Datagram.heave;
      roll = raw0Datagram.roll;
      pitch = raw0Datagram.pitch;
      temperature = raw0Datagram.temperature;
      heading = raw0Datagram.heading;
      transmitMode = raw0Datagram.transmitMode;
      // spare = raw0Datagram.spare;
      // sweep = raw0Datagram.sweep;
      offset = raw0Datagram.offset;
      count = raw0Datagram.count;
   }

   protected void setRaw0DatagramParameters(Raw0Datagram raw0Datagram) {
      raw0Datagram.channel = channel;
      // raw0Datagram.mode = mode;
      raw0Datagram.transducerDepth = transducerDepth;
      raw0Datagram.frequency = frequency;
      raw0Datagram.transmitPower = transmitPower;
      raw0Datagram.pulseLength = pulseDuration;
      raw0Datagram.bandWidth = bandWidth;
      raw0Datagram.sampleInterval = sampleInterval;
      raw0Datagram.soundVelocity = soundVelocity;
      raw0Datagram.absorptionCoefficient = absorptionCoefficient;
      raw0Datagram.heave = heave;
      raw0Datagram.roll = roll;
      raw0Datagram.pitch = pitch;
      raw0Datagram.temperature = temperature;
      raw0Datagram.heading = heading;
      raw0Datagram.transmitMode = transmitMode;
      // raw0Datagram.spare = spare;
      raw0Datagram.sweep = 0;
      raw0Datagram.offset = offset;
      raw0Datagram.count = count;
   }

   @Override
   public String toString() {
      return getInstant() + " [channel " + channel + ", " + KoronaUtils.hzToKHz(frequency) + " kHz]";
   }

   public abstract String getDataTypeName();

   @Override
   public abstract ChannelData makeCopy();

   @Override
   public Instant getInstant() {
      return instant;
   }

   @Override
   public void setInstant(Instant instant) {
      throwExceptionIfReadOnly();
      this.instant = instant;
   }

   public void throwExceptionIfReadOnly() {
      if (readOnlyBecauseOfDataType != null) {
         throw new ChannelDataReadOnlyException(this);
      }
   }

   public void setRange(float minDepth, float maxDepth) {
      throwExceptionIfReadOnly();
      if (maxDepth < minDepth) {
         throw new IllegalArgumentException("min = " + minDepth + " > max = " + maxDepth);
      }
      int minIndex = Math.round((minDepth - getHeaveCorrectedTransducerDepth()) / getSampleDistance());
      int maxIndex = Math.round((maxDepth - getHeaveCorrectedTransducerDepth()) / getSampleDistance());
      setOffset(minIndex);
      setCount(maxIndex - minIndex);
   }

   /**
    * Returns transducer depth plus heave.
    *
    * @return heave corrected transducer depth
    */
   public float getHeaveCorrectedTransducerDepth() {
      return transducerDepth + heave;
   }

   /**
    * Get transducer depth.
    *
    * @return transducer depth
    */
   public float getTransducerDepth() {
      return transducerDepth;
   }

   /**
    * Set transducer depth.
    *
    * @param transducerDepth a new transducer depth
    */
   public void setTransducerDepth(float transducerDepth) {
      throwExceptionIfReadOnly();
      this.transducerDepth = transducerDepth;
   }

   /**
    * A representative frequency.
    * For single frequency data this is that frequency.
    * For broadband data this is somewhere between the start and end frequencies.
    *
    * @return a representative frequency [Hz]
    */
   public float getCenterFrequency() {
      return (getStartFrequency() + getEndFrequency()) / 2;
   }

   public float getStartFrequency() {
      return frequency;
   }

   public float getEndFrequency() {
      return frequency;
   }

   public FloatRange getFrequencyRange() {
      return FloatRange.ofUnsorted(getStartFrequency(), getEndFrequency());
   }

   /**
    * Get frequency.
    *
    * @return the frequency in [Hz]
    */
   public float getFrequency() {
      return frequency;
   }

   public void setFrequency(float frequency) {
      throwExceptionIfReadOnly();
      this.frequency = frequency;
   }

   public float getPulseDuration() {
      return pulseDuration;
   }

   public void setPulseDuration(float pulseDuration) {
      throwExceptionIfReadOnly();
      this.pulseDuration = pulseDuration;
   }

   /**
    * Get bandwidth.
    *
    * @return the bandwidth
    */
   public float getBandWidth() {
      return bandWidth;
   }

   public void setBandWidth(float bandWidth) {
      throwExceptionIfReadOnly();
      this.bandWidth = bandWidth;
   }

   /**
    * Get sample interval [s].
    *
    * @return the sample interval
    */
   public float getSampleInterval() {
      return sampleInterval;
   }

   public void setSampleInterval(float sampleInterval) {
      throwExceptionIfReadOnly();
      this.sampleInterval = sampleInterval;
   }

   /**
    * Get sample distance (m).
    *
    * @return the sample distance
    */
   @Override
   public float getSampleDistance() {
      return sampleInterval * soundVelocity / 2;
   }

   /**
    * Set sample distance.
    *
    * @param sampleDistance a new sample distance
    */
   public void setSampleDistance(float sampleDistance) {
      throwExceptionIfReadOnly();
      sampleInterval = 2 * sampleDistance / soundVelocity;
   }

   /**
    * Get sample depth.
    *
    * @param sampleIndex the index of a sample point
    * @return distance from surface to the sample point, i.e., a positive number
    */
   public float getSampleDepth(int sampleIndex) {
      return rangeToDepth(getSampleRange(sampleIndex));
   }

   /**
    * Get sample range.
    *
    * @param sampleIndex index of sample
    * @return distance from transducer to sample
    */
   public float getSampleRange(int sampleIndex) {
      return (sampleIndex + offset) * getSampleDistance();
   }

   /**
    * Get transmit power.
    *
    * @return the transmit power in [W]
    */
   public float getTransmitPower() {
      return transmitPower;
   }

   public void setTransmitPower(float transmitPower) {
      throwExceptionIfReadOnly();
      this.transmitPower = transmitPower;
   }

   /**
    * Converts depth to range.
    *
    * @param depth the depth
    * @return the range
    */
   public float depthToRange(float depth) {
      return depth - getHeaveCorrectedTransducerDepth();
   }

   /**
    * Converts range to depth.
    *
    * @param range the range
    * @return the depth
    */
   public float rangeToDepth(float range) {
      return getHeaveCorrectedTransducerDepth() + range;
   }

   /**
    * Get minimum depth.
    *
    * @return the minimum depth
    */
   @Override
   public float getMinDepth() {
      return getSampleDepth(0);
   }

   /**
    * Get maximum depth.
    *
    * @return the maximum depth
    */
   @Override
   public float getMaxDepth() {
      return rangeToDepth(getMaxRange());
   }

   public float getMinRange() {
      return getSampleRange(0);
   }

   public float getMaxRange() {
      return getSampleRange(count);
   }

   /**
    * Returns the depth range.
    *
    * @return depth range
    */
   public FloatRange getDepthRange() {
      return FloatRange.of(getMinDepth(), getMaxDepth());
   }

   /**
    * Get the closest index for a given depth.
    *
    * @param depth depth
    * @return the closest sample index
    */
   public int depthToSampleIndex(float depth) {
      return rangeToSampleIndex(depthToRange(depth));
   }

   public int depthToClampedSampleIndex(float depth) {
      return rangeToClampedSampleIndex(depthToRange(depth));
   }

   public float depthToClampedSampleIndexAsFloat(float depth) {
      return rangeToClampedSampleIndexAsFloat(depthToRange(depth));
   }

   /**
    * Get the closest index for a given range.
    *
    * @param range distance from transducer
    * @return the closest sample index
    */
   public int rangeToSampleIndex(float range) {
      return Math.round(rangeToSampleIndexAsFloat(range));
   }

   public float rangeToSampleIndexAsFloat(float range) {
      return range / getSampleDistance() - offset;
   }

   public int rangeToClampedSampleIndex(float range) {
      return Math.clamp(rangeToSampleIndex(range), 0, count);
   }

   public float rangeToClampedSampleIndexAsFloat(float range) {
      return Math.clamp(rangeToSampleIndexAsFloat(range), 0, count);
   }

   /**
    * Get containing index for depth.
    *
    * @param depth depth
    * @return the containing sample index
    */
   @Override
   public int depthToContainingSampleIndex(float depth) {
      return rangeToContainingSampleIndex(depthToRange(depth));
   }

   /**
    * Get containing index for range.
    *
    * @param range distance from transducer
    * @return the containing sample index
    */
   public int rangeToContainingSampleIndex(float range) {
      return (int) Math.floor(rangeToSampleIndexAsFloat(range));
   }

   public float getSoundVelocity() {
      return soundVelocity;
   }

   public void setSoundVelocity(float soundVelocity) {
      throwExceptionIfReadOnly();
      this.soundVelocity = soundVelocity;
   }

   public float getAbsorptionCoefficient() {
      return absorptionCoefficient;
   }

   public void setAbsorptionCoefficient(float absorptionCoefficient) {
      throwExceptionIfReadOnly();
      this.absorptionCoefficient = absorptionCoefficient;
   }

   public float getHeave() {
      return heave;
   }

   public void setHeave(float heave) {
      throwExceptionIfReadOnly();
      this.heave = heave;
   }

   public float getRoll() {
      return roll;
   }

   public void setRoll(float roll) {
      throwExceptionIfReadOnly();
      this.roll = roll;
   }

   public float getPitch() {
      return pitch;
   }

   public void setPitch(float pitch) {
      throwExceptionIfReadOnly();
      this.pitch = pitch;
   }

   public float getTemperature() {
      return temperature;
   }

   public void setTemperature(float temperature) {
      throwExceptionIfReadOnly();
      this.temperature = temperature;
   }

   public float getHeading() {
      return heading;
   }

   public void setHeading(float heading) {
      throwExceptionIfReadOnly();
      this.heading = heading;
   }

   public short getTransmitMode() {
      return transmitMode;
   }

   public void setTransmitMode(short transmitMode) {
      throwExceptionIfReadOnly();
      this.transmitMode = transmitMode;
   }

   public int getOffset() {
      return offset;
   }

   public void setOffset(int offset) {
      throwExceptionIfReadOnly();
      this.offset = offset;
   }

   @Override
   public int getChannel() {
      return channel;
   }

   @Override
   public void setChannel(int channel) {
      throwExceptionIfReadOnly();
      this.channel = (short) channel;
   }

   @Override
   public int getSampleCount() {
      return count;
   }

   public int getCount() {
      return count;
   }

   public void setCount(int count) {
      throwExceptionIfReadOnly();
      if (count < 0) {
         throw new IllegalArgumentException("count: " + count);
      }
      this.count = count;
   }

   @Override
   public void setPingConfiguration(PingConfiguration pingConfiguration) {
      throwExceptionIfReadOnly();
      this.pingConfiguration = pingConfiguration;
      RawFileTransducer transducer = pingConfiguration.getRawFileConfiguration().getTransducers().get(channel - 1);
      setTransducer(transducer);
   }

   void setTransducer(RawFileTransducer transducer) {
      throwExceptionIfReadOnly();
      this.transducer = transducer;
      ChannelCalibration calibration = transducer.getChannelCalibration();
      calibration.absorptionCoefficient.ifPresent(this::setAbsorptionCoefficient);
      calibration.soundVelocity.ifPresent(this::setSoundVelocity);
      onSetTransducer();
   }

   protected void onSetTransducer() {
   }

   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   public ChannelCalibration getCalibration() {
      return transducer.getChannelCalibration();
   }

   public RawFileTransducer getTransducer() {
      return transducer;
   }

   public float getTvgRange(int sampleIndex) {
      float r = getSampleRange(sampleIndex) - getTvgRangeCorrection();
      return Math.max(r, getSampleDistance());
   }

   public float getTvgRangeCorrection() {
      float tau = pulseDuration;
      float c = soundVelocity;
      return tau * c / 4; // = t*c/2, for t = tau/2
   }

   public TvgArray getTVGArray() {
      return TvgCache.getTvgArray(this);
   }

   public @Nullable String getReadOnlyBecauseOfDataType() {
      return readOnlyBecauseOfDataType;
   }

   public void setReadOnly(ChannelData owner) {
      readOnlyBecauseOfDataType = owner.getDataTypeName();
   }

   public void setReadWrite() {
      readOnlyBecauseOfDataType = null;
   }

   public abstract PowerData getPowerData();

   public @Nullable AngleData getAngleData() {
      return getPowerData().getAngleData();
   }

   public abstract void removeAngles();

   public float getTSU(int sampleIndex) {
      return getPowerData().getTSU(sampleIndex);
   }

   public abstract void reduceData(int beginSampleIndex, int endSampleIndex);
}
