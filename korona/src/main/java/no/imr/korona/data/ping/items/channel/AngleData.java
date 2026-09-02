package no.imr.korona.data.ping.items.channel;

import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.math.MathUtils;
import org.jspecify.annotations.Nullable;

import java.util.function.Supplier;

public final class AngleData {
   public static final float BYTE_TO_ELECTRICAL_ANGLE = 180f / 128f;
   private static final double TWENTY_LOG_2 = 20 * Math.log10(2); // 6.0206...

   private volatile float @Nullable [] electricalAngles;
   private volatile @Nullable Supplier<float[]> electricalAnglesSupplier;

   public AngleData(float[] electricalAngles) {
      this.electricalAngles = electricalAngles;
   }

   public AngleData(Supplier<float[]> electricalAnglesSupplier) {
      this.electricalAnglesSupplier = electricalAnglesSupplier;
   }

   /**
    * Helper for converting from electrical to mechanical angle, including offsets.
    *
    * @param electricalAngle  electrical angle to be converted
    * @param angleSensitivity angle sensitivity
    * @param angleOffset      angle offset
    * @return mechanical angle
    */
   static float electricalToMechanicalAngle(float electricalAngle, float angleSensitivity, float angleOffset) {
      return electricalAngle / angleSensitivity - angleOffset;
   }

   public static float mechanicalToElectricalAngle(float mechanicalAngle, float angleSensitivity, float angleOffset) {
      return (mechanicalAngle + angleOffset) * angleSensitivity;
   }

   /**
    * The Target Strength directivity correction function.
    *
    * @param alongAngle       along angle
    * @param athwartAngle     athwart angle
    * @param alongBeamWidth   beam width alongship
    * @param athwartBeamWidth beam width athwartship
    * @return the correction, should be added to the uncompensated target strength
    */
   public static double getDirectivityCorrection(double alongAngle, double athwartAngle,
                                                 double alongBeamWidth, double athwartBeamWidth) {
      double alongTerm = MathUtils.sq(alongAngle / (alongBeamWidth / 2));
      double athwartTerm = MathUtils.sq(athwartAngle / (athwartBeamWidth / 2));

      // Bessel directivity function https://doi.org/10.1093/icesjms/fsn052
      return TWENTY_LOG_2 * (alongTerm + athwartTerm - 0.18 * alongTerm * athwartTerm);
   }

   public float getDirectivityCorrection(int sampleIndex, RawFileTransducer transducer) {
      return (float) getDirectivityCorrection(
            getMechanicalAlongAngle(sampleIndex, transducer),
            getMechanicalAthwartAngle(sampleIndex, transducer),
            transducer.getBeamWidthAlongship(),
            transducer.getBeamWidthAthwartship());
   }

   public float[] getElectricalAngles() {
      float[] angles = electricalAngles;
      if (angles == null) {
         Supplier<float[]> anglesSupplier = electricalAnglesSupplier;
         if (anglesSupplier != null) {
            angles = anglesSupplier.get();
            electricalAngles = angles;
            electricalAnglesSupplier = null;
         } else {
            angles = electricalAngles;
            if (angles == null) {
               throw new IllegalStateException();
            }
         }
      }
      return angles;
   }

   public float getElectricalAlongAngle(int sampleIndex) {
      return getElectricalAngles()[2 * sampleIndex + 1];
   }

   public float getElectricalAthwartAngle(int sampleIndex) {
      return getElectricalAngles()[2 * sampleIndex];
   }

   public float getMechanicalAlongAngle(int sampleIndex, RawFileTransducer transducer) {
      return electricalToMechanicalAngle(getElectricalAlongAngle(sampleIndex), transducer.getAngleSensitivityAlongship(), transducer.getAngleOffsetAlongship());
   }

   public float getMechanicalAthwartAngle(int sampleIndex, RawFileTransducer transducer) {
      return electricalToMechanicalAngle(getElectricalAthwartAngle(sampleIndex), transducer.getAngleSensitivityAthwartship(), transducer.getAngleOffsetAthwartship());
   }

   public static byte[] electricalAnglesToBytes(float[] electricalAngles, RawFileTransducer transducer) {
      float byteToAngleAthwartship = BYTE_TO_ELECTRICAL_ANGLE * getByteAngleScalingAthwartship(transducer);
      float byteToAngleAlongship = BYTE_TO_ELECTRICAL_ANGLE * getByteAngleScalingAlongship(transducer);
      byte[] bytes = new byte[electricalAngles.length];
      for (int i = 0; i < electricalAngles.length; i += 2) {
         bytes[i] = clampToByte(electricalAngles[i] / byteToAngleAthwartship);
         bytes[i + 1] = clampToByte(electricalAngles[i + 1] / byteToAngleAlongship);
      }
      return bytes;
   }

   private static byte clampToByte(float value) {
      return (byte) Math.clamp(Math.round(value), Byte.MIN_VALUE, Byte.MAX_VALUE);
   }

   public static float[] bytesToElectricalAngles(byte[] bytes, RawFileTransducer transducer) {
      float byteToAngleAthwartship = BYTE_TO_ELECTRICAL_ANGLE * getByteAngleScalingAthwartship(transducer);
      float byteToAngleAlongship = BYTE_TO_ELECTRICAL_ANGLE * getByteAngleScalingAlongship(transducer);
      float[] electricalAngles = new float[bytes.length];
      for (int i = 0; i < bytes.length; i += 2) {
         electricalAngles[i] = bytes[i] * byteToAngleAthwartship;
         electricalAngles[i + 1] = bytes[i + 1] * byteToAngleAlongship;
      }
      return electricalAngles;
   }

   private static float getByteAngleScalingAthwartship(RawFileTransducer transducer) {
      RawFileTransducer.Xml0Info xml0Info = transducer.getXml0Info();
      return xml0Info != null ? xml0Info.getByteAngleScalingAthwartship() : 1;
   }

   private static float getByteAngleScalingAlongship(RawFileTransducer transducer) {
      RawFileTransducer.Xml0Info xml0Info = transducer.getXml0Info();
      return xml0Info != null ? xml0Info.getByteAngleScalingAlongship() : 1;
   }
}
