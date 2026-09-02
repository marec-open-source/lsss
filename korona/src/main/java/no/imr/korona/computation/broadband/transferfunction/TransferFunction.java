package no.imr.korona.computation.broadband.transferfunction;

import com.google.common.math.IntMath;
import no.imr.korona.computation.broadband.PulseCompression;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.math.FftCache;
import org.apache.commons.numbers.complex.Complex;
import org.jtransforms.fft.DoubleFFT_1D;

import java.util.Arrays;

public interface TransferFunction {

   Complex evaluateGainFunction(double frequency);

   default double evaluateGainFunctionInDb(double frequency) {
      return 20.0 * Math.log10(evaluateGainFunction(frequency).abs());
   }

   default double[] applyToTimeSignal(double[] timeSignal, double samplingFrequency, double slope) {
      int fftSize = IntMath.ceilingPowerOfTwo(timeSignal.length);
      DoubleFFT_1D fft = FftCache.getDouble1D(fftSize);

      double[] paddedSignal = Arrays.copyOf(timeSignal, fftSize);
      if (slope > 0) {
         double[] wtx = PulseCompression.generateWtx(slope, timeSignal.length);
         for (int i = 0; i < timeSignal.length; i++) {
            paddedSignal[i] *= wtx[i];
         }
      }

      // In place forward DFT.
      fft.realForward(paddedSignal);
      ComplexArray paddedSignalDft = ComplexArray.of(paddedSignal);

      for (int i = 0; i < paddedSignalDft.length(); i++) {
         Complex gain = evaluateGainFunction(i * samplingFrequency / paddedSignal.length);
         paddedSignalDft.multiply(i, gain);
      }

      fft.realInverse(paddedSignalDft.values(), true);
      return Arrays.copyOf(paddedSignalDft.values(), timeSignal.length);
   }

   default TransferFunction multiply(TransferFunction otherTransferFunction) {
      if (otherTransferFunction instanceof IdentityTransferFunction) {
         return this;
      }
      return new ProductTransferFunction(this, otherTransferFunction);
   }
}
