package no.imr.korona.data.datagrams;

import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.Utils;
import no.imr.tools.math.linalg.Vec3;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The EK60 echosounder configuration datagram.
 */
public final class Con0Datagram extends BaseDatagram {
   public static final DatagramType TYPE = DatagramType.simple("CON0", Con0Datagram::new);

   public String surveyName = ""; // "Loch Ness" (128)
   public String transectName = ""; // "L0123"  (128)
   public String sounderName = ""; // "EK60" (128)
   public String version = ""; // "1.2.3.45" (30)
   public short multiplexing; // 0=Normal, 1=Mux
   public int timeBias; //diff between UTC and local time [minutes]
   public float soundVelocityAverage; // [m/s]
   public float soundVelocityTransducer; // [m/s]
   public Vec3 mruOffset = Vec3.ZERO; // [m]
   public Vec3 mruAlpha = Vec3.ZERO; // [deg]
   public Vec3 gpsOffset = Vec3.ZERO; // [m]
   public String spare = ""; // future use (48)
   public List<Transducer> transducers = new ArrayList<>(); // Transducer settings

   public Con0Datagram(Instant instant) {
      super(instant);
   }

   public Con0Datagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      surveyName = ByteBufferUtils.readCString(byteBuffer, 128);
      transectName = ByteBufferUtils.readCString(byteBuffer, 128);
      sounderName = ByteBufferUtils.readCString(byteBuffer, 128);
      version = ByteBufferUtils.readCString(byteBuffer, 30);
      multiplexing = byteBuffer.getShort();
      timeBias = byteBuffer.getInt();
      soundVelocityAverage = byteBuffer.getFloat();
      soundVelocityTransducer = byteBuffer.getFloat();
      mruOffset = ByteBufferUtils.readVec3(byteBuffer);
      mruAlpha = ByteBufferUtils.readVec3(byteBuffer);
      gpsOffset = ByteBufferUtils.readVec3(byteBuffer);
      spare = ByteBufferUtils.readCString(byteBuffer, 48);
      transducers = ByteBufferUtils.readCountAndList(byteBuffer, 128 + 16 * 4 + 5 * 4 + 8 + 5 * 4 + 8 + 5 * 4 + 2 * 4 + 16 + 28, Transducer::new);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeCString(byteBuffer, surveyName, 128);
      ByteBufferUtils.writeCString(byteBuffer, transectName, 128);
      ByteBufferUtils.writeCString(byteBuffer, sounderName, 128);
      ByteBufferUtils.writeCString(byteBuffer, version, 30);
      byteBuffer.putShort(multiplexing);
      byteBuffer.putInt(timeBias);
      byteBuffer.putFloat(soundVelocityAverage);
      byteBuffer.putFloat(soundVelocityTransducer);
      ByteBufferUtils.writeVec3(byteBuffer, mruOffset);
      ByteBufferUtils.writeVec3(byteBuffer, mruAlpha);
      ByteBufferUtils.writeVec3(byteBuffer, gpsOffset);
      ByteBufferUtils.writeCString(byteBuffer, spare, 48);
      ByteBufferUtils.writeCountAndList(byteBuffer, transducers, Transducer::write);
   }

   @Override
   public void addPingItems(PingConversion pingConversion) {
      pingConversion.addPingItem(new RawFileConfiguration(this));
   }

   @Override
   public String toStringExtra() {
      return "\"" + surveyName + "\" \"" + transectName + "\" \""
            + sounderName + "\" \"" + version + "\" "
            + transducers.stream().map(transducer -> Utils.toString(transducer.frequency)).collect(Collectors.joining(", ", "[", "]"));
   }

   @Override
   public DatagramType getDatagramType() {
      return TYPE;
   }

   public static final class Transducer {
      public String channelId = ""; // Channel identification (128)
      public int beamType; // 0=SINGLE, 1=SPLIT
      public float frequency; // [Hz]
      public float gain; // [dB] - See note below
      public float equivalentBeamAngle; // [dB]
      public float beamWidthAlongship; // [degree]    // RK, MS70: vertical angle
      public float beamWidthAthwartship; // [degree]  // RK, MS70: horizontal angle
      public float angleSensitivityAlongship;
      public float angleSensitivityAthwartship;
      public float angleOffsetAlongship; // [degree]
      public float angleOffsetAthwartship; // [degree]
      public Vec3 pos = Vec3.ZERO; // future use
      public Vec3 dir = Vec3.ZERO; // future use
      public final float[] pulseLengthTable = new float[5]; // Available pulse lengths for the channel [s]
      public final byte[] spare2 = new byte[8]; // future use
      public final float[] gainTable = new float[5]; // Gain for each pulse length in the PulseLengthTable [dB]
      public final byte[] spare3 = new byte[8]; // future use
      public final float[] saCorrectionTable = new float[5]; // Sa correction for each pulse length in the PulseLengthTable [dB]
      public float filterSlope; // [-] Broadband filter slope
      public float directivityDrop; // [-]
      public String transceiverVersion = ""; // Transceiver version /16
      public final byte[] spare4 = new byte[28]; // Future use /28)

      // Note:
      //
      // float gain: The single Gain parameter was used actively in raw data files generated with software version 1.3.
      // This was before PulseLengthTable, GainTable, and SaCorrectionTable were introduced in software version 1.4
      // to enable gain and Sa correction parameters for each pulse length.

      public Transducer() {
      }

      public Transducer(ByteBuffer byteBuffer) {
         channelId = ByteBufferUtils.readCString(byteBuffer, 128);
         beamType = byteBuffer.getInt();
         frequency = byteBuffer.getFloat();
         gain = byteBuffer.getFloat();
         equivalentBeamAngle = byteBuffer.getFloat();
         beamWidthAlongship = byteBuffer.getFloat();
         beamWidthAthwartship = byteBuffer.getFloat();
         angleSensitivityAlongship = byteBuffer.getFloat();
         angleSensitivityAthwartship = byteBuffer.getFloat();
         angleOffsetAlongship = byteBuffer.getFloat();
         angleOffsetAthwartship = byteBuffer.getFloat();
         pos = ByteBufferUtils.readVec3(byteBuffer);
         dir = ByteBufferUtils.readVec3(byteBuffer);
         ByteBufferUtils.readFloatArray(byteBuffer, pulseLengthTable);
         byteBuffer.get(spare2);
         ByteBufferUtils.readFloatArray(byteBuffer, gainTable);
         byteBuffer.get(spare3);
         ByteBufferUtils.readFloatArray(byteBuffer, saCorrectionTable);
         filterSlope = byteBuffer.getFloat();
         directivityDrop = byteBuffer.getFloat();
         transceiverVersion = ByteBufferUtils.readCString(byteBuffer, 16);
         byteBuffer.get(spare4);
      }

      private void write(ByteBuffer byteBuffer) {
         ByteBufferUtils.writeCString(byteBuffer, channelId, 128);
         byteBuffer.putInt(beamType);
         byteBuffer.putFloat(frequency);
         byteBuffer.putFloat(gain);
         byteBuffer.putFloat(equivalentBeamAngle);
         byteBuffer.putFloat(beamWidthAlongship);
         byteBuffer.putFloat(beamWidthAthwartship);
         byteBuffer.putFloat(angleSensitivityAlongship);
         byteBuffer.putFloat(angleSensitivityAthwartship);
         byteBuffer.putFloat(angleOffsetAlongship);
         byteBuffer.putFloat(angleOffsetAthwartship);
         ByteBufferUtils.writeVec3(byteBuffer, pos);
         ByteBufferUtils.writeVec3(byteBuffer, dir);
         ByteBufferUtils.writeFloatArray(byteBuffer, pulseLengthTable);
         byteBuffer.put(spare2);
         ByteBufferUtils.writeFloatArray(byteBuffer, gainTable);
         byteBuffer.put(spare3);
         ByteBufferUtils.writeFloatArray(byteBuffer, saCorrectionTable);
         byteBuffer.putFloat(filterSlope);
         byteBuffer.putFloat(directivityDrop);
         ByteBufferUtils.writeCString(byteBuffer, transceiverVersion, 16);
         byteBuffer.put(spare4);
      }

      @Override
      public String toString() {
         return channelId + ", " + Utils.toString(frequency);
      }
   }
}
