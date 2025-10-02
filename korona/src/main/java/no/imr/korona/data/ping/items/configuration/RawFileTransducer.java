package no.imr.korona.data.ping.items.configuration;

import no.imr.korona.computation.broadband.EK80Parameters;
import no.imr.korona.computation.broadband.PulseCompressionFilterChain;
import no.imr.korona.data.datagrams.Con0Datagram;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.tools.Utils;
import no.imr.tools.math.linalg.Vec3;
import no.marec.lsss.api.data.ChannelConfiguration;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class to hold transducer settings.
 */
public final class RawFileTransducer implements ChannelConfiguration {
   private String channelId = ""; // Channel identification (128)
   private int beamType; // 0=SINGLE, 1=SPLIT
   private float frequency; // [Hz]
   private float gain; // [dB] - See note below
   private float equivalentBeamAngle; // [dB]
   private float beamWidthAlongship; // [degree]    // RK, MS70: vertical angle
   private float beamWidthAthwartship; // [degree]  // RK, MS70: horizontal angle
   private float angleSensitivityAlongship;
   private float angleSensitivityAthwartship;
   private float angleOffsetAlongship; // [degree]
   private float angleOffsetAthwartship; // [degree]
   private Vec3 pos = Vec3.ZERO; // future use
   private Vec3 dir = Vec3.ZERO; // future use
   private final float[] pulseDurationTable = new float[5]; // Available pulse lengths for the channel [s]
   // private final byte[] spare2 = new byte[8]; // future use
   private final float[] gainTable = new float[5]; // Gain for each pulse length in the PulseDurationTable [dB]
   // private final byte[] spare3 = new byte[8]; // future use
   private final float[] saCorrectionTable = new float[5]; // Sa correction for each pulse length in the PulseDurationTable [dB]
   private float filterSlope; // [-] Broadband filter slope
   private float directivityDrop; // [-]
   private String transceiverVersion = ""; // Transceiver version /16
   // private final byte[] spare4 = new byte[28]; // Future use /28)

   private final CopyInfo copyInfo;
   private ChannelCalibration channelCalibration = ChannelCalibration.EMPTY;
   private @Nullable Xml0Info xml0Info;
   private PulseCompressionFilterChain pulseCompressionFilterChain = PulseCompressionFilterChain.EMPTY;

   public RawFileTransducer() {
      copyInfo = new CopyInfo(this);
   }

   public RawFileTransducer(RawFileTransducer transducer) {
      channelId = transducer.channelId;
      beamType = transducer.beamType;
      frequency = transducer.frequency;
      gain = transducer.gain;
      equivalentBeamAngle = transducer.equivalentBeamAngle;
      beamWidthAlongship = transducer.beamWidthAlongship;
      beamWidthAthwartship = transducer.beamWidthAthwartship;
      angleSensitivityAlongship = transducer.angleSensitivityAlongship;
      angleSensitivityAthwartship = transducer.angleSensitivityAthwartship;
      angleOffsetAlongship = transducer.angleOffsetAlongship;
      angleOffsetAthwartship = transducer.angleOffsetAthwartship;
      pos = transducer.pos;
      dir = transducer.dir;
      System.arraycopy(transducer.pulseDurationTable, 0, pulseDurationTable, 0, pulseDurationTable.length);
      // System.arraycopy(transducer.spare2, 0, spare2, 0, spare2.length);
      System.arraycopy(transducer.gainTable, 0, gainTable, 0, gainTable.length);
      // System.arraycopy(transducer.spare3, 0, spare3, 0, spare3.length);
      System.arraycopy(transducer.saCorrectionTable, 0, saCorrectionTable, 0, saCorrectionTable.length);
      filterSlope = transducer.filterSlope;
      directivityDrop = transducer.directivityDrop;
      transceiverVersion = transducer.transceiverVersion;
      // System.arraycopy(transducer.spare4, 0, spare4, 0, spare4.length);

      copyInfo = transducer.copyInfo;
      channelCalibration = transducer.channelCalibration;
      xml0Info = transducer.xml0Info != null ? new Xml0Info(transducer.xml0Info) : null;
      pulseCompressionFilterChain = transducer.pulseCompressionFilterChain;
   }

   public RawFileTransducer(Con0Datagram.Transducer transducer) {
      this();

      channelId = transducer.channelId;
      beamType = transducer.beamType;
      frequency = transducer.frequency;
      gain = transducer.gain;
      equivalentBeamAngle = transducer.equivalentBeamAngle;
      beamWidthAlongship = transducer.beamWidthAlongship;
      beamWidthAthwartship = transducer.beamWidthAthwartship;
      angleSensitivityAlongship = transducer.angleSensitivityAlongship;
      angleSensitivityAthwartship = transducer.angleSensitivityAthwartship;
      angleOffsetAlongship = transducer.angleOffsetAlongship;
      angleOffsetAthwartship = transducer.angleOffsetAthwartship;
      pos = transducer.pos;
      dir = transducer.dir;
      System.arraycopy(transducer.pulseLengthTable, 0, pulseDurationTable, 0, pulseDurationTable.length);
      // System.arraycopy(transducer.spare2, 0, spare2, 0, spare2.length);
      System.arraycopy(transducer.gainTable, 0, gainTable, 0, gainTable.length);
      // System.arraycopy(transducer.spare3, 0, spare3, 0, spare3.length);
      System.arraycopy(transducer.saCorrectionTable, 0, saCorrectionTable, 0, saCorrectionTable.length);
      filterSlope = transducer.filterSlope;
      directivityDrop = transducer.directivityDrop;
      transceiverVersion = transducer.transceiverVersion;
      // System.arraycopy(transducer.spare4, 0, spare4, 0, spare4.length);
   }

   public Con0Datagram.Transducer toCon0DatagramTransducer() {
      Con0Datagram.Transducer transducer = new Con0Datagram.Transducer();
      transducer.channelId = channelId;
      transducer.beamType = beamType;
      transducer.frequency = frequency;
      transducer.gain = gain;
      transducer.equivalentBeamAngle = equivalentBeamAngle;
      transducer.beamWidthAlongship = beamWidthAlongship;
      transducer.beamWidthAthwartship = beamWidthAthwartship;
      transducer.angleSensitivityAlongship = angleSensitivityAlongship;
      transducer.angleSensitivityAthwartship = angleSensitivityAthwartship;
      transducer.angleOffsetAlongship = angleOffsetAlongship;
      transducer.angleOffsetAthwartship = angleOffsetAthwartship;
      transducer.pos = pos;
      transducer.dir = dir;
      System.arraycopy(pulseDurationTable, 0, transducer.pulseLengthTable, 0, pulseDurationTable.length);
      // System.arraycopy(spare2, 0, transducer.spare2, 0, spare2.length);
      System.arraycopy(gainTable, 0, transducer.gainTable, 0, gainTable.length);
      // System.arraycopy(spare3, 0, transducer.spare3, 0, spare3.length);
      System.arraycopy(saCorrectionTable, 0, transducer.saCorrectionTable, 0, saCorrectionTable.length);
      transducer.filterSlope = filterSlope;
      transducer.directivityDrop = directivityDrop;
      transducer.transceiverVersion = transceiverVersion;
      // System.arraycopy(spare4, 0, transducer.spare4, 0, spare4.length);
      return transducer;
   }

   @Override
   public String toString() {
      return channelId + ", " + frequency;
   }

   public RawFileTransducer makeCopy() {
      return new RawFileTransducer(this);
   }

   void initAsCopy() {
      String copySuffix = " - #" + copyInfo.counter.incrementAndGet();
      channelId = copyInfo.original.channelId + copySuffix;
      if (xml0Info != null && copyInfo.original.xml0Info != null) {
         xml0Info.name = copyInfo.original.xml0Info.name + copySuffix;
         xml0Info.transducerSerialNumber = copyInfo.original.xml0Info.transducerSerialNumber + copySuffix;
      }
   }

   public RawFileTransducer getOriginalTransducer() {
      return copyInfo.original;
   }

   public void calibrate(ChannelCalibration calibration) {
      channelCalibration = calibration;
      calibration.gain.ifPresent(this::setGainAndGainTable);
      calibration.equivalentBeamAngle.ifPresent(this::setEquivalentBeamAngle);
      calibration.beamWidthAlongship.ifPresent(this::setBeamWidthAlongship);
      calibration.beamWidthAthwartship.ifPresent(this::setBeamWidthAthwartship);
      calibration.angleOffsetAlongship.ifPresent(this::setAngleOffsetAlongship);
      calibration.angleOffsetAthwartship.ifPresent(this::setAngleOffsetAthwartship);
      calibration.saCorrections.forEach(this::setSaCorrection);
   }

   @Override
   public String getChannelId() {
      return channelId;
   }

   public void setChannelId(String channelId) {
      this.channelId = channelId;
   }

   public int getBeamType() {
      return beamType;
   }

   public void setBeamType(int beamType) {
      this.beamType = beamType;
   }

   @Override
   public float getNominalFrequency() {
      return getFrequency();
   }

   public float getFrequency() {
      return frequency;
   }

   public void setFrequency(float frequency) {
      this.frequency = frequency;
   }

   public int getKHz() {
      return Utils.hzToKHz(frequency);
   }

   public float getGain() {
      return gain;
   }

   public float getGainForPulseDuration(float pulseDuration) {
      if (Float.isNaN(gain)) {
         // Use gainTable only if gain is NaN. Is this correct?
         int i = pulseDurationToIndex(pulseDuration);
         if (i < 0) {
            return 0; // returning default value of 0 if pulse length is not found
         }
         return gainTable[i];
      } else {
         return gain;
      }
   }

   public void setGainAndGainTable(float gain) {
      this.gain = gain;
      Arrays.fill(gainTable, gain);
   }

   public float getEquivalentBeamAngle() {
      return equivalentBeamAngle;
   }

   public void setEquivalentBeamAngle(float equivalentBeamAngle) {
      this.equivalentBeamAngle = equivalentBeamAngle;
   }

   public float getBeamWidthAlongship() {
      return beamWidthAlongship;
   }

   public void setBeamWidthAlongship(float beamWidthAlongship) {
      this.beamWidthAlongship = beamWidthAlongship;
   }

   public float getBeamWidthAthwartship() {
      return beamWidthAthwartship;
   }

   public void setBeamWidthAthwartship(float beamWidthAthwartship) {
      this.beamWidthAthwartship = beamWidthAthwartship;
   }

   public float getAngleSensitivityAlongship() {
      return angleSensitivityAlongship;
   }

   public void setAngleSensitivityAlongship(float angleSensitivityAlongship) {
      this.angleSensitivityAlongship = angleSensitivityAlongship;
   }

   public float getAngleSensitivityAthwartship() {
      return angleSensitivityAthwartship;
   }

   public void setAngleSensitivityAthwartship(float angleSensitivityAthwartship) {
      this.angleSensitivityAthwartship = angleSensitivityAthwartship;
   }

   public float getAngleOffsetAlongship() {
      return angleOffsetAlongship;
   }

   public void setAngleOffsetAlongship(float angleOffsetAlongship) {
      this.angleOffsetAlongship = angleOffsetAlongship;
   }

   public float getAngleOffsetAthwartship() {
      return angleOffsetAthwartship;
   }

   public void setAngleOffsetAthwartship(float angleOffsetAthwartship) {
      this.angleOffsetAthwartship = angleOffsetAthwartship;
   }

   public Vec3 getPos() {
      return pos;
   }

   public void setPos(Vec3 pos) {
      this.pos = pos;
   }

   public Vec3 getDir() {
      return dir;
   }

   public void setDir(Vec3 dir) {
      this.dir = dir;
   }

   public float[] getPulseDurationTable() {
      return pulseDurationTable;
   }

   public float[] getGainTable() {
      return gainTable;
   }

   public float[] getSaCorrectionTable() {
      return saCorrectionTable;
   }

   public float getFilterSlope() {
      return filterSlope;
   }

   public void setFilterSlope(float filterSlope) {
      this.filterSlope = filterSlope;
   }

   public float getDirectivityDrop() {
      return directivityDrop;
   }

   public void setDirectivityDrop(float directivityDrop) {
      this.directivityDrop = directivityDrop;
   }

   public String getTransceiverVersion() {
      return transceiverVersion;
   }

   public void setTransceiverVersion(String transceiverVersion) {
      this.transceiverVersion = transceiverVersion;
   }

   public int pulseDurationToIndex(float pulseDuration) {
      for (int i = 0; i < pulseDurationTable.length; i++) {
         if (pulseDurationTable[i] == pulseDuration) {
            return i;
         }
      }
      return -1;
   }

   public int pulseDurationToClosestIndex(float pulseDuration) {
      int closestIndex = -1;
      float closestDiff = Float.POSITIVE_INFINITY;
      for (int i = 0; i < pulseDurationTable.length; i++) {
         float diff = Math.abs(pulseDurationTable[i] - pulseDuration);
         if (diff < closestDiff) {
            closestIndex = i;
            closestDiff = diff;
         }
      }
      return closestIndex;
   }

   public float getSaCorrection(float pulseDuration) {
      int i = pulseDurationToIndex(pulseDuration);
      if (i < 0) {
         return 0; // returning default value of 0 if pulse length is not found
      }
      return saCorrectionTable[i];
   }

   public void setSaCorrection(float pulseDuration, float saCorrection) {
      int i = pulseDurationToIndex(pulseDuration);
      if (i < 0) {
         return; //will not update table if the pulse length is not found
      }
      saCorrectionTable[i] = saCorrection;
   }

   public ChannelCalibration getChannelCalibration() {
      return channelCalibration;
   }

   public @Nullable Xml0Info getXml0Info() {
      return xml0Info;
   }

   public @Nullable InitialParameters getXml0InitialParameters() {
      return xml0Info != null ? xml0Info.initialParameters : null;
   }

   public void setXml0Info(Xml0Info xml0Info) {
      this.xml0Info = xml0Info;
   }

   public double getEK80rwbtrx() {
      return xml0Info != null ? xml0Info.getImpedance() : EK80Parameters.rwbtrx;
   }

   public float getEK80fs() {
      if (xml0Info != null) {
         float samplingFrequency = xml0Info.getSamplingFrequency();
         if (samplingFrequency > 0) {
            return samplingFrequency;
         }
      }
      return EK80Parameters.fs;
   }

   public boolean isWBT() {
      return xml0Info != null && TransceiverType.isWBT(xml0Info.transceiverType);
   }

   public PulseCompressionFilterChain getPulseCompressionFilterChain() {
      return pulseCompressionFilterChain;
   }

   public void setPulseCompressionFilterChain(PulseCompressionFilterChain pulseCompressionFilterChain) {
      this.pulseCompressionFilterChain = pulseCompressionFilterChain;
   }

   /**
    * Extra info in XML0/Configuration that is not in CON0.
    */
   public static final class Xml0Info {
      private String channelIdShort = "";
      private String name = "";
      private String transducerSerialNumber = "";
      private String transceiverSerialNumber = "";
      private String transceiverSoftwareVersion = "";
      private String transceiverType = "";
      private float impedance;
      private float samplingFrequency;
      private float byteAngleScalingAthwartship = 1; // Added by marec
      private float byteAngleScalingAlongship = 1;   // Added by marec
      private String transducerMounting = "";
      private boolean dropKeel;
      private @Nullable InitialParameters initialParameters;

      public Xml0Info() {
      }

      private Xml0Info(Xml0Info xml0Info) {
         channelIdShort = xml0Info.channelIdShort;
         name = xml0Info.name;
         transducerSerialNumber = xml0Info.transducerSerialNumber;
         transceiverSerialNumber = xml0Info.transceiverSerialNumber;
         transceiverSoftwareVersion = xml0Info.transceiverSoftwareVersion;
         transceiverType = xml0Info.transceiverType;
         impedance = xml0Info.impedance;
         samplingFrequency = xml0Info.samplingFrequency;
         byteAngleScalingAthwartship = xml0Info.byteAngleScalingAthwartship;
         byteAngleScalingAlongship = xml0Info.byteAngleScalingAlongship;
         transducerMounting = xml0Info.transducerMounting;
         dropKeel = xml0Info.dropKeel;
         initialParameters = xml0Info.initialParameters;
      }

      public void setChannelIdShort(String channelIdShort) {
         this.channelIdShort = channelIdShort;
      }

      public String getChannelIdShort() {
         return channelIdShort;
      }

      public String getName() {
         return name;
      }

      public void setName(String name) {
         this.name = name;
      }

      public String getTransducerSerialNumber() {
         return transducerSerialNumber;
      }

      public void setTransducerSerialNumber(String transducerSerialNumber) {
         this.transducerSerialNumber = transducerSerialNumber;
      }

      public String getTransceiverSerialNumber() {
         return transceiverSerialNumber;
      }

      public void setTransceiverSerialNumber(String transceiverSerialNumber) {
         this.transceiverSerialNumber = transceiverSerialNumber;
      }

      public String getTransceiverSoftwareVersion() {
         return transceiverSoftwareVersion;
      }

      public void setTransceiverSoftwareVersion(String transceiverSoftwareVersion) {
         this.transceiverSoftwareVersion = transceiverSoftwareVersion;
      }

      public String getTransceiverType() {
         return transceiverType;
      }

      public void setTransceiverType(String transceiverType) {
         this.transceiverType = transceiverType;
      }

      public float getImpedance() {
         return impedance;
      }

      public void setImpedance(float impedance) {
         this.impedance = impedance;
      }

      public float getSamplingFrequency() {
         return samplingFrequency;
      }

      public void setSamplingFrequency(float samplingFrequency) {
         this.samplingFrequency = samplingFrequency;
      }

      public float getByteAngleScalingAthwartship() {
         return byteAngleScalingAthwartship;
      }

      public void setByteAngleScalingAthwartship(float byteAngleScalingAthwartship) {
         this.byteAngleScalingAthwartship = byteAngleScalingAthwartship;
      }

      public float getByteAngleScalingAlongship() {
         return byteAngleScalingAlongship;
      }

      public void setByteAngleScalingAlongship(float byteAngleScalingAlongship) {
         this.byteAngleScalingAlongship = byteAngleScalingAlongship;
      }

      public String getTransducerMounting() {
         return transducerMounting;
      }

      public void setTransducerMounting(String transducerMounting) {
         this.transducerMounting = transducerMounting;
         dropKeel = transducerMounting.equals(TransducerMounting.DROP_KEEL);
      }

      public boolean isDropKeel() {
         return dropKeel;
      }

      public @Nullable InitialParameters getInitialParameters() {
         return initialParameters;
      }

      public void setInitialParameters(@Nullable InitialParameters initialParameters) {
         this.initialParameters = initialParameters;
      }
   }

   public record InitialParameters(
         String pingId,
         int channelMode,
         int pulseForm
   ) {
   }

   private static final class CopyInfo {
      private final AtomicInteger counter = new AtomicInteger();
      private final RawFileTransducer original;

      private CopyInfo(RawFileTransducer original) {
         this.original = original;
      }
   }
}
