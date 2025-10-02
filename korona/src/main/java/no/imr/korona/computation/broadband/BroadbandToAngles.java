package no.imr.korona.computation.broadband;

import no.imr.korona.data.ping.items.channel.BroadbandData;
import no.imr.korona.data.ping.items.configuration.BeamType;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.logging.LogOnce;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.ComplexArrayUtils;
import org.apache.commons.numbers.complex.Complex;

public final class BroadbandToAngles {
   private static final double SQRT_3 = Math.sqrt(3);
   private static final double SIN_COS_45 = Math.sin(Math.toRadians(45));

   private final BroadbandData broadbandData;
   private final ComplexArray[] sectors;

   public BroadbandToAngles(BroadbandData broadbandData) {
      this.broadbandData = broadbandData;
      sectors = new ComplexArray[broadbandData.getSectorCount()];
      for (int i = 0; i < sectors.length; i++) {
         sectors[i] = ComplexArray.of(broadbandData.getReal()[i], broadbandData.getImag()[i]);
      }
   }

   public float[] computeElectricalAngles(PulseCompression pulseCompression, int downSamplingFactor) {
      ComplexArray[] pulseCompressedSignal = new ComplexArray[sectors.length];
      for (int i = 0; i < sectors.length; i++) {
         pulseCompressedSignal[i] = pulseCompression.computePulseCompressedSignal(sectors[i]);
      }

      int beamType = broadbandData.getTransducer().getBeamType();
      int count = broadbandData.getCount() / downSamplingFactor;
      float[] angles = new float[count * 2];
      for (int i = 0; i < count; i++) {
         double alongDeltaPhiSum = 0;
         double athwartDeltaPhiSum = 0;
         for (int j = 0; j < downSamplingFactor; j++) {
            int originalIndex = i * downSamplingFactor + j;

            DeltaPhi deltaPhi = switch (beamType) {
               case BeamType.SINGLE -> {
                  yield DeltaPhi.ZERO;
               }
               case BeamType.REF, BeamType.REF_B, BeamType.SPLIT_2 -> {
                  LogOnce.warning("Computation of angles for beam type " + beamType + " is not yet implemented", broadbandData.getPingConfiguration());
                  yield DeltaPhi.ZERO;
               }
               case BeamType.SPLIT -> {
                  yield computeDeltaPhiFromFourSectors(
                        pulseCompressedSignal[0].toComplex(originalIndex),
                        pulseCompressedSignal[1].toComplex(originalIndex),
                        pulseCompressedSignal[2].toComplex(originalIndex),
                        pulseCompressedSignal[3].toComplex(originalIndex));
               }
               case BeamType.SPLIT_3, BeamType.SPLIT_3_C, BeamType.SPLIT_3_CN, BeamType.SPLIT_3_CW -> {
                  if (sectors.length > 3) {
                     yield calculateDeltaPhiFromThreeSectorsAndCenter(
                           pulseCompressedSignal[0].toComplex(originalIndex),
                           pulseCompressedSignal[1].toComplex(originalIndex),
                           pulseCompressedSignal[2].toComplex(originalIndex),
                           pulseCompressedSignal[3].toComplex(originalIndex));
                  } else {
                     yield calculateDeltaPhiFromThreeSectors(
                           pulseCompressedSignal[0].toComplex(originalIndex),
                           pulseCompressedSignal[1].toComplex(originalIndex),
                           pulseCompressedSignal[2].toComplex(originalIndex));
                  }
               }
               case BeamType.SPLIT_4_B -> {
                  yield calculateDeltaPhiForBeamType97(
                        pulseCompressedSignal[0].re(originalIndex), pulseCompressedSignal[0].im(originalIndex),
                        pulseCompressedSignal[1].re(originalIndex), pulseCompressedSignal[1].im(originalIndex),
                        pulseCompressedSignal[2].re(originalIndex), pulseCompressedSignal[2].im(originalIndex),
                        pulseCompressedSignal[3].re(originalIndex), pulseCompressedSignal[3].im(originalIndex),
                        broadbandData.getTransducer());
               }
               default -> {
                  LogOnce.warning("Unknown beam type " + beamType, broadbandData.getPingConfiguration());
                  yield DeltaPhi.ZERO;
               }
            };

            alongDeltaPhiSum += deltaPhi.along;
            athwartDeltaPhiSum += deltaPhi.athwart;
         }
         angles[2 * i] = (float) deltaPhiToElectricalAngle(athwartDeltaPhiSum / downSamplingFactor);
         angles[2 * i + 1] = (float) deltaPhiToElectricalAngle(alongDeltaPhiSum / downSamplingFactor);
      }
      return angles;
   }

   public static DeltaPhi computeDeltaPhiFromFourSectors(Complex starAft, Complex portAft, Complex portFore, Complex starFore) {
      Complex fore = ComplexArrayUtils.average(starFore, portFore);
      Complex aft = ComplexArrayUtils.average(starAft, portAft);
      Complex star = ComplexArrayUtils.average(starAft, starFore);
      Complex port = ComplexArrayUtils.average(portAft, portFore);
      double alongDeltaPhi = computeDeltaPhi(fore, aft);
      double athwartDeltaPhi = computeDeltaPhi(star, port);
      return new DeltaPhi(alongDeltaPhi, athwartDeltaPhi);
   }

   public static DeltaPhi calculateDeltaPhiFromThreeSectorsAndCenter(Complex sec1, Complex sec2, Complex sec3, Complex center) {
      return calculateDeltaPhiFromThreeSectors(sec1.add(center), sec2.add(center), sec3.add(center));
   }

   public static DeltaPhi calculateDeltaPhiFromThreeSectors(Complex sec1, Complex sec2, Complex sec3) {
      // Reference: Splitbeam with 3 sector elements and one or no centre element, For External Use, Rev01.pdf

      double phi31 = computeDeltaPhi(sec3, sec1);
      double phi32 = computeDeltaPhi(sec3, sec2);

      double deltaPhiAlong = (phi31 + phi32) / SQRT_3;
      double deltaPhiAthwart = phi32 - phi31;

      return new DeltaPhi(deltaPhiAlong, deltaPhiAthwart);
   }

   public static DeltaPhi calculateDeltaPhiForBeamType97(double x1, double y1,
                                                         double x2, double y2,
                                                         double x3, double y3,
                                                         double x4, double y4,
                                                         RawFileTransducer transducer) {
      // Reference: https://www.simrad.online/ek80/interface/ek80_interface_en_a4.pdf

      double angleSensitivity = (transducer.getAngleSensitivityAlongship() + transducer.getAngleSensitivityAthwartship()) / 2;
      double uxm = Math.asin(Math.atan2(x2 * y1 - x1 * y2, x1 * x2 + y1 * y2) / angleSensitivity);
      double uym = Math.asin(Math.atan2(x4 * y3 - x3 * y4, x3 * x4 + y3 * y4) / angleSensitivity);

      double xm = Math.tan(uxm);
      double ym = Math.tan(uym);

      double x = SIN_COS_45 * xm - SIN_COS_45 * ym;
      double y = SIN_COS_45 * xm + SIN_COS_45 * ym;

      double ux = Math.atan2(x, 1);
      double uy = Math.atan2(y, 1);

      double deltaPhiAlong = Math.sin(ux) * angleSensitivity;
      double deltaPhiAthwart = Math.sin(uy) * angleSensitivity;

      return new DeltaPhi(deltaPhiAlong, deltaPhiAthwart);
   }

   public static double computeDeltaPhi(Complex sector, Complex otherSector) {
      return sector.multiply(otherSector.conj()).arg();
   }

   public static double deltaPhiToElectricalAngle(double deltaPhi) {
      return Math.toDegrees(deltaPhi);
   }

   public record DeltaPhi(double along, double athwart) {
      public static final DeltaPhi ZERO = new DeltaPhi(0, 0);
   }
}
