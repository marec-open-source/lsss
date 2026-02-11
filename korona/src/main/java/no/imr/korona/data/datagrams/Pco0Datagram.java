package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.OtherIdxPingItem;
import no.imr.tools.math.linalg.Vec3;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Ping information.
 */
public sealed class Pco0Datagram extends DatagramPingItem implements OtherIdxPingItem permits Pco1Datagram {
   public static final DatagramType TYPE_PCO0 = DatagramType.simple("PCO0", Pco0Datagram::new);

   public final PingConfiguration pingConfiguration;

   public Pco0Datagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      pingConfiguration = new PingConfiguration(byteBuffer);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      pingConfiguration.write(byteBuffer);
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE_PCO0;
   }

   @Override
   public String toStringExtra() {
      return pingConfiguration.toString();
   }

   public static final class PingConfiguration {
      public final List<TransceiverConfig> transceiverConfigs;
      public final String hintSinceLast;
      public final String hintSinceLastPing;

      public PingConfiguration(ByteBuffer byteBuffer) throws DatagramFormatException {
         transceiverConfigs = ByteBufferUtils.readCountAndList(byteBuffer, 2 * 4 + 2 + 4 + 8 + 12 + 4, TransceiverConfig::new);
         hintSinceLast = ByteBufferUtils.readLengthAndUtf16String(byteBuffer);
         hintSinceLastPing = ByteBufferUtils.readLengthAndUtf16String(byteBuffer);
      }

      private void write(ByteBuffer byteBuffer) {
         ByteBufferUtils.writeCountAndList(byteBuffer, transceiverConfigs, TransceiverConfig::write);
         ByteBufferUtils.writeLengthAndUtf16String(byteBuffer, hintSinceLast);
         ByteBufferUtils.writeLengthAndUtf16String(byteBuffer, hintSinceLastPing);
      }

      @Override
      public String toString() {
         return "PingConfiguration{" +
               "transceiverConfigs=" + transceiverConfigs +
               ", hintSinceLast='" + hintSinceLast + '\'' +
               ", hintSinceLastPing='" + hintSinceLastPing + '\'' +
               '}';
      }
   }

   public static final class TransceiverConfig {
      public final int byteSize;
      public final int id;
      public final String transceiverName;
      public final int splitBeamPercentage;
      public final TxConfiguration txConfig;
      public final RxConfiguration rxConfig;
      public final int transmissionMode;

      public TransceiverConfig(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         id = byteBuffer.getInt();
         transceiverName = ByteBufferUtils.readLengthAndUtf16String(byteBuffer);
         splitBeamPercentage = byteBuffer.getInt();
         txConfig = new TxConfiguration(byteBuffer);
         rxConfig = new RxConfiguration(byteBuffer);
         transmissionMode = byteBuffer.getInt();
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putInt(id);
         ByteBufferUtils.writeLengthAndUtf16String(byteBuffer, transceiverName);
         byteBuffer.putInt(splitBeamPercentage);
         txConfig.write(byteBuffer);
         rxConfig.write(byteBuffer);
         byteBuffer.putInt(transmissionMode);
      }

      @Override
      public String toString() {
         return "TransceiverConfig{" +
               "byteSize=" + byteSize +
               ", id=" + id +
               ", transceiverName='" + transceiverName + '\'' +
               ", splitBeamPercentage=" + splitBeamPercentage +
               ", txConfig=" + txConfig +
               ", rxConfig=" + rxConfig +
               ", transmissionMode=" + transmissionMode +
               '}';
      }
   }

   public static final class TxConfiguration {
      public final int byteSize;
      public final List<TxPingConfig> txPingConfigs;

      public TxConfiguration(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         txPingConfigs = ByteBufferUtils.readCountAndList(byteBuffer, 2 * 4 + 2 + 14 * 4 + 8 + 4 + 8 + 2 * 12 + 2 * 4 + 12, TxPingConfig::new);
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         ByteBufferUtils.writeCountAndList(byteBuffer, txPingConfigs, TxPingConfig::write);
      }

      @Override
      public String toString() {
         return "TxConfiguration{" +
               "byteSize=" + byteSize +
               ", txPingConfigs=" + txPingConfigs +
               '}';
      }
   }

   public static final class TxPingConfig {
      public final int byteSize;
      public final int id;
      public final String pingName;
      public final float frequency;
      public final float pulseDuration;
      public final int pulseForm;
      public final float pulseSweep;
      public final float pulseSlope;
      public final int focusX;
      public final int focusY;
      public final float beamWidthX;
      public final float beamWidthY;
      public final float steeringX;
      public final float steeringY;
      public final float beamDelay;
      public final float txAmplitude;
      public final float txVoltage;
      public final double actualBeamBandWidthRx;
      public final int decimation;
      public final double range;
      public final Vec3 steeringVectorHcs;
      public final Vec3 rotationAxisVector;
      public final float[] txPingWeightX;
      public final float[] txPingWeightY;
      public final TxPingPerformanceInfo performanceInfo;

      public TxPingConfig(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         id = byteBuffer.getInt();
         pingName = ByteBufferUtils.readLengthAndUtf16String(byteBuffer);
         frequency = byteBuffer.getFloat();
         pulseDuration = byteBuffer.getFloat();
         pulseForm = byteBuffer.getInt();
         pulseSweep = byteBuffer.getFloat();
         pulseSlope = byteBuffer.getFloat();
         focusX = byteBuffer.getInt();
         focusY = byteBuffer.getInt();
         beamWidthX = byteBuffer.getFloat();
         beamWidthY = byteBuffer.getFloat();
         steeringX = byteBuffer.getFloat();
         steeringY = byteBuffer.getFloat();
         beamDelay = byteBuffer.getFloat();
         txAmplitude = byteBuffer.getFloat();
         txVoltage = byteBuffer.getFloat();
         actualBeamBandWidthRx = byteBuffer.getDouble();
         decimation = byteBuffer.getInt();
         range = byteBuffer.getDouble();
         steeringVectorHcs = ByteBufferUtils.readVec3(byteBuffer);
         rotationAxisVector = ByteBufferUtils.readVec3(byteBuffer);
         txPingWeightX = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
         txPingWeightY = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
         performanceInfo = new TxPingPerformanceInfo(byteBuffer);
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putInt(id);
         ByteBufferUtils.writeLengthAndUtf16String(byteBuffer, pingName);
         byteBuffer.putFloat(frequency);
         byteBuffer.putFloat(pulseDuration);
         byteBuffer.putInt(pulseForm);
         byteBuffer.putFloat(pulseSweep);
         byteBuffer.putFloat(pulseSlope);
         byteBuffer.putInt(focusX);
         byteBuffer.putInt(focusY);
         byteBuffer.putFloat(beamWidthX);
         byteBuffer.putFloat(beamWidthY);
         byteBuffer.putFloat(steeringX);
         byteBuffer.putFloat(steeringY);
         byteBuffer.putFloat(beamDelay);
         byteBuffer.putFloat(txAmplitude);
         byteBuffer.putFloat(txVoltage);
         byteBuffer.putDouble(actualBeamBandWidthRx);
         byteBuffer.putInt(decimation);
         byteBuffer.putDouble(range);
         ByteBufferUtils.writeVec3(byteBuffer, steeringVectorHcs);
         ByteBufferUtils.writeVec3(byteBuffer, rotationAxisVector);
         ByteBufferUtils.writeCountAndFloatArray(byteBuffer, txPingWeightX);
         ByteBufferUtils.writeCountAndFloatArray(byteBuffer, txPingWeightY);
         performanceInfo.write(byteBuffer);
      }

      @Override
      public String toString() {
         return "TxPingConfig{" +
               "byteSize=" + byteSize +
               ", id=" + id +
               ", pingName='" + pingName + '\'' +
               ", frequency=" + frequency +
               ", pulseDuration=" + pulseDuration +
               ", pulseForm=" + pulseForm +
               ", pulseSweep=" + pulseSweep +
               ", pulseSlope=" + pulseSlope +
               ", focusX=" + focusX +
               ", focusY=" + focusY +
               ", beamWidthX=" + beamWidthX +
               ", beamWidthY=" + beamWidthY +
               ", steeringX=" + steeringX +
               ", steeringY=" + steeringY +
               ", beamDelay=" + beamDelay +
               ", txAmplitude=" + txAmplitude +
               ", txVoltage=" + txVoltage +
               ", actualBeamBandWidthRx=" + actualBeamBandWidthRx +
               ", decimation=" + decimation +
               ", range=" + range +
               ", steeringVectorHcs=" + steeringVectorHcs +
               ", rotationAxisVector=" + rotationAxisVector +
               ", txPingWeightX=" + Arrays.toString(txPingWeightX) +
               ", txPingWeightY=" + Arrays.toString(txPingWeightY) +
               ", performanceInfo=" + performanceInfo +
               '}';
      }
   }

   public static final class TxPingPerformanceInfo {
      public final int byteSize;
      public final float txPower; // watts
      public final float sourceLevel; // dB

      public TxPingPerformanceInfo(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         txPower = byteBuffer.getFloat();
         sourceLevel = byteBuffer.getFloat();
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putFloat(txPower);
         byteBuffer.putFloat(sourceLevel);
      }

      @Override
      public String toString() {
         return "TxPingPerformanceInfo{" +
               "byteSize=" + byteSize +
               ", txPower=" + txPower +
               ", sourceLevel=" + sourceLevel +
               '}';
      }
   }

   public static final class RxConfiguration {
      public final int byteSize;
      public final int audioBeamIndex;
      public final List<FanConfig> fans;

      public RxConfiguration(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         audioBeamIndex = byteBuffer.getInt();
         fans = ByteBufferUtils.readCountAndList(byteBuffer, 2 * 4 + 2 + 8 + 6 * 4 + 40 + 4, FanConfig::new);
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putInt(audioBeamIndex);
         ByteBufferUtils.writeCountAndList(byteBuffer, fans, FanConfig::write);
      }

      @Override
      public String toString() {
         return "RxConfiguration{" +
               "byteSize=" + byteSize +
               ", audioBeamIndex=" + audioBeamIndex +
               ", fans=" + fans +
               '}';
      }
   }

   public static final class FanConfig {
      public final int byteSize;
      public final int id;
      public final String fanName;
      public final double sampleInterval;
      public final int txPingId;
      public final float[] mainBeamRxWeightX;
      public final float[] mainBeamRxWeightY;
      public final float[] splitBeamRxWeightX;
      public final float[] splitBeamRxWeightY;
      public final int noiseFilter;
      public final FanProcessing processing;
      public final List<RxBeamConfig> rxBeams;
      public final boolean rxDelayPresent;
      public final int rxDelay;

      public FanConfig(ByteBuffer byteBuffer) throws DatagramFormatException {
         int p0 = byteBuffer.position();
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         id = byteBuffer.getInt();
         fanName = ByteBufferUtils.readLengthAndUtf16String(byteBuffer);
         sampleInterval = byteBuffer.getDouble();
         txPingId = byteBuffer.getInt();
         mainBeamRxWeightX = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
         mainBeamRxWeightY = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
         splitBeamRxWeightX = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
         splitBeamRxWeightY = ByteBufferUtils.readCountAndFloatArray(byteBuffer);
         noiseFilter = byteBuffer.getInt();
         processing = new FanProcessing(byteBuffer);
         rxBeams = ByteBufferUtils.readCountAndList(byteBuffer, 2 * 4 + 2 + 5 * 4 + 12 + 4 + 10 * 4, RxBeamConfig::new);
         rxDelayPresent = byteBuffer.position() - p0 + 4 <= byteSize;
         rxDelay = rxDelayPresent ? byteBuffer.getInt() : 0;
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putInt(id);
         ByteBufferUtils.writeLengthAndUtf16String(byteBuffer, fanName);
         byteBuffer.putDouble(sampleInterval);
         byteBuffer.putInt(txPingId);
         ByteBufferUtils.writeCountAndFloatArray(byteBuffer, mainBeamRxWeightX);
         ByteBufferUtils.writeCountAndFloatArray(byteBuffer, mainBeamRxWeightY);
         ByteBufferUtils.writeCountAndFloatArray(byteBuffer, splitBeamRxWeightX);
         ByteBufferUtils.writeCountAndFloatArray(byteBuffer, splitBeamRxWeightY);
         byteBuffer.putInt(noiseFilter);
         processing.write(byteBuffer);
         ByteBufferUtils.writeCountAndList(byteBuffer, rxBeams, RxBeamConfig::write);
         if (rxDelayPresent) {
            byteBuffer.putInt(rxDelay);
         }
      }

      @Override
      public String toString() {
         return "FanConfig{" +
               "byteSize=" + byteSize +
               ", id=" + id +
               ", fanName='" + fanName + '\'' +
               ", sampleInterval=" + sampleInterval +
               ", txPingId=" + txPingId +
               ", mainBeamRxWeightX=" + Arrays.toString(mainBeamRxWeightX) +
               ", mainBeamRxWeightY=" + Arrays.toString(mainBeamRxWeightY) +
               ", splitBeamRxWeightX=" + Arrays.toString(splitBeamRxWeightX) +
               ", splitBeamRxWeightY=" + Arrays.toString(splitBeamRxWeightY) +
               ", noiseFilter=" + noiseFilter +
               ", processing=" + processing +
               ", rxBeams=" + rxBeams +
               ", rxDelayPresent=" + rxDelayPresent +
               ", rxDelay=" + rxDelay +
               '}';
      }
   }

   public static final class FanProcessing {
      public final int byteSize;
      public final double tvgA;
      public final double tvgB;
      public final double tvgC;
      public final int rcg;
      public final int agc;
      public final int ampGain;

      public FanProcessing(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         tvgA = byteBuffer.getDouble();
         tvgB = byteBuffer.getDouble();
         tvgC = byteBuffer.getDouble();
         rcg = byteBuffer.getInt();
         agc = byteBuffer.getInt();
         ampGain = byteBuffer.getInt();
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putDouble(tvgA);
         byteBuffer.putDouble(tvgB);
         byteBuffer.putDouble(tvgC);
         byteBuffer.putInt(rcg);
         byteBuffer.putInt(agc);
         byteBuffer.putInt(ampGain);
      }

      @Override
      public String toString() {
         return "FanProcessing{" +
               "byteSize=" + byteSize +
               ", tvgA=" + tvgA +
               ", tvgB=" + tvgB +
               ", tvgC=" + tvgC +
               ", rcg=" + rcg +
               ", agc=" + agc +
               ", ampGain=" + ampGain +
               '}';
      }
   }

   public static final class RxBeamConfig {
      public final int byteSize;
      public final int id;
      public final String beamName;
      public final float beamWidthX;
      public final float beamWidthY;
      public final float steeringX;
      public final float steeringY;
      public final int beamType;
      public final Vec3 steeringVectorHcs;
      public final int processingType; // 1 = sonar, 2 = echosounder, 3 = both
      public final RxBeamPerformanceInfo performanceInfo;
      public final boolean rxDelayPresent;
      public final int rxDelay;

      public RxBeamConfig(ByteBuffer byteBuffer) throws DatagramFormatException {
         int p0 = byteBuffer.position();
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         id = byteBuffer.getInt();
         beamName = ByteBufferUtils.readLengthAndUtf16String(byteBuffer);
         beamWidthX = byteBuffer.getFloat();
         beamWidthY = byteBuffer.getFloat();
         steeringX = byteBuffer.getFloat();
         steeringY = byteBuffer.getFloat();
         beamType = byteBuffer.getInt();
         steeringVectorHcs = ByteBufferUtils.readVec3(byteBuffer);
         processingType = byteBuffer.getInt();
         performanceInfo = new RxBeamPerformanceInfo(byteBuffer);
         rxDelayPresent = byteBuffer.position() - p0 + 4 <= byteSize;
         rxDelay = rxDelayPresent ? byteBuffer.getInt() : 0;
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putInt(id);
         ByteBufferUtils.writeLengthAndUtf16String(byteBuffer, beamName);
         byteBuffer.putFloat(beamWidthX);
         byteBuffer.putFloat(beamWidthY);
         byteBuffer.putFloat(steeringX);
         byteBuffer.putFloat(steeringY);
         byteBuffer.putInt(beamType);
         ByteBufferUtils.writeVec3(byteBuffer, steeringVectorHcs);
         byteBuffer.putInt(processingType);
         performanceInfo.write(byteBuffer);
         if (rxDelayPresent) {
            byteBuffer.putInt(rxDelay);
         }
      }

      @Override
      public String toString() {
         return "RxBeamConfig{" +
               "byteSize=" + byteSize +
               ", id=" + id +
               ", beamName='" + beamName + '\'' +
               ", beamWidthX=" + beamWidthX +
               ", beamWidthY=" + beamWidthY +
               ", steeringX=" + steeringX +
               ", steeringY=" + steeringY +
               ", beamType=" + beamType +
               ", steeringVectorHcs=" + steeringVectorHcs +
               ", processingType=" + processingType +
               ", performanceInfo=" + performanceInfo +
               ", rxDelayPresent=" + rxDelayPresent +
               ", rxDelay=" + rxDelay +
               '}';
      }
   }

   public static final class RxBeamPerformanceInfo {
      public final int byteSize;
      public final float directivityIndex;
      public final float gain;
      public final float gainAdjust;
      public final float saCorrection;
      public final float saCorrectionAdjust;
      public final float equivalentBeamAngle;
      public final float absorptionCoefficient;
      public final float angleSensitivityAlongship;
      public final float angleSensitivityAthwartship;

      public RxBeamPerformanceInfo(ByteBuffer byteBuffer) throws DatagramFormatException {
         byteSize = ByteBufferUtils.readByteSize(byteBuffer);
         directivityIndex = byteBuffer.getFloat();
         gain = byteBuffer.getFloat();
         gainAdjust = byteBuffer.getFloat();
         saCorrection = byteBuffer.getFloat();
         saCorrectionAdjust = byteBuffer.getFloat();
         equivalentBeamAngle = byteBuffer.getFloat();
         absorptionCoefficient = byteBuffer.getFloat();
         angleSensitivityAlongship = byteBuffer.getFloat();
         angleSensitivityAthwartship = byteBuffer.getFloat();
      }

      private void write(ByteBuffer byteBuffer) {
         byteBuffer.putInt(byteSize);
         byteBuffer.putFloat(directivityIndex);
         byteBuffer.putFloat(gain);
         byteBuffer.putFloat(gainAdjust);
         byteBuffer.putFloat(saCorrection);
         byteBuffer.putFloat(saCorrectionAdjust);
         byteBuffer.putFloat(equivalentBeamAngle);
         byteBuffer.putFloat(absorptionCoefficient);
         byteBuffer.putFloat(angleSensitivityAlongship);
         byteBuffer.putFloat(angleSensitivityAthwartship);
      }

      @Override
      public String toString() {
         return "RxBeamPerformanceInfo{" +
               "byteSize=" + byteSize +
               ", directivityIndex=" + directivityIndex +
               ", gain=" + gain +
               ", gainAdjust=" + gainAdjust +
               ", saCorrection=" + saCorrection +
               ", saCorrectionAdjust=" + saCorrectionAdjust +
               ", equivalentBeamAngle=" + equivalentBeamAngle +
               ", absorptionCoefficient=" + absorptionCoefficient +
               ", angleSensitivityAlongship=" + angleSensitivityAlongship +
               ", angleSensitivityAthwartship=" + angleSensitivityAthwartship +
               '}';
      }
   }
}
