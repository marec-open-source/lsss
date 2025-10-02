package no.imr.tools.math;

import org.apache.commons.numbers.complex.Complex;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * An array of complex numbers stored as [re_0, im_0, re_1, im_1, ...].
 */
public final class ComplexArray {
   public static final ComplexArray EMPTY = new ComplexArray(new double[0]);

   private final int length;
   private final double[] values;

   private ComplexArray(double[] values) {
      if ((values.length & 1) != 0) {
         throw new IllegalArgumentException("Odd length: " + values.length);
      }
      length = values.length / 2;
      this.values = values;
   }

   public static ComplexArray ofLength(int length) {
      return new ComplexArray(new double[2 * length]);
   }

   public static ComplexArray of(double[] values) {
      return new ComplexArray(values);
   }

   public static ComplexArray of(Complex[] values) {
      int length = values.length;
      ComplexArray complexArray = ofLength(length);
      for (int i = 0; i < length; i++) {
         complexArray.set(i, values[i].getReal(), values[i].getImaginary());
      }
      return complexArray;
   }

   public static ComplexArray of(double[] re, double[] im) {
      ArrayMath.requireSameLength(re, im);
      ComplexArray complexArray = ofLength(re.length);
      for (int i = 0; i < re.length; i++) {
         complexArray.set(i, re[i], im[i]);
      }
      return complexArray;
   }

   public static ComplexArray of(float[] re, float[] im) {
      ArrayMath.requireSameLength(re, im);
      int length = re.length;
      ComplexArray complexArray = ofLength(length);
      for (int i = 0; i < length; i++) {
         complexArray.set(i, re[i], im[i]);
      }
      return complexArray;
   }

   public static ComplexArray ofReal(double[] re) {
      int length = re.length;
      ComplexArray complexArray = ofLength(length);
      for (int i = 0; i < length; i++) {
         complexArray.set(i, re[i], 0);
      }
      return complexArray;
   }

   @Override
   public String toString() {
      return "{length: " + length + ", values: " + toComplexValuesString() + "}";
   }

   public String toComplexValuesString() {
      StringBuilder sb = new StringBuilder()
            .append('[');
      for (int i = 0; i < length; i++) {
         if (i > 0) {
            sb.append(", ");
         }
         sb.append('(').append(re(i)).append(", ").append(im(i)).append(')');
      }
      sb.append(']');
      return sb.toString();
   }

   /**
    * Copies this complex array, truncating or padding with zeros (if necessary) so the copy has the specified length.
    * Similar to {@link Arrays#copyOf(double[], int)}.
    *
    * @param newLength new length (the number of complex numbers)
    * @return the copied complex array
    */
   public ComplexArray copyOf(int newLength) {
      double[] copiedValues = Arrays.copyOf(values, 2 * newLength);
      return of(copiedValues);
   }

   /**
    * Copies a range of this complex array.
    * Similar to {@link Arrays#copyOfRange(double[], int, int)}.
    *
    * @param beginIndex begin index
    * @param endIndex   end index
    * @return the copied complex array
    */
   public ComplexArray copyOfRange(int beginIndex, int endIndex) {
      double[] copiedValues = Arrays.copyOfRange(values, 2 * beginIndex, 2 * endIndex);
      return of(copiedValues);
   }

   public ComplexArray copyOfRange(int beginIndex, int endIndex, int newLength) {
      int valueCount = 2 * Math.min(Math.min(endIndex, length) - beginIndex, newLength);
      double[] copiedValues = new double[2 * newLength];
      System.arraycopy(values, 2 * beginIndex, copiedValues, 0, valueCount);
      return of(copiedValues);
   }

   public int length() {
      return length;
   }

   public double[] values() {
      return values;
   }

   public double re(int index) {
      return values[2 * index];
   }

   public float[] reArrayFloat() {
      float[] reArray = new float[length];
      for (int i = 0; i < length; i++) {
         reArray[i] = (float) re(i);
      }
      return reArray;
   }

   public double im(int index) {
      return values[2 * index + 1];
   }

   public float[] imArrayFloat() {
      float[] imArray = new float[length];
      for (int i = 0; i < length; i++) {
         imArray[i] = (float) im(i);
      }
      return imArray;
   }

   public Complex toComplex(int index) {
      int i = 2 * index;
      return Complex.ofCartesian(values[i], values[i + 1]);
   }

   public void set(int index, double re, double im) {
      int i = 2 * index;
      values[i] = re;
      values[i + 1] = im;
   }

   public void set(int index, ComplexArray other, int otherIndex) {
      int i = 2 * index;
      int otherI = 2 * otherIndex;
      values[i] = other.values[otherI];
      values[i + 1] = other.values[otherI + 1];
   }

   public void set(int beginIndex, ComplexArray source, int sourceBeginIndex, int length) {
      System.arraycopy(source.values, 2 * sourceBeginIndex, values, 2 * beginIndex, 2 * length);
   }

   public void setConjugated(int index, ComplexArray other, int otherIndex) {
      int i = 2 * index;
      int otherI = 2 * otherIndex;
      values[i] = other.values[otherI];
      values[i + 1] = -other.values[otherI + 1];
   }

   public double arg(int index) {
      int i = 2 * index;
      double a = values[i];
      double b = values[i + 1];
      return Math.atan2(b, a);
   }

   public double abs(int index) {
      return Math.sqrt(abs2(index));
   }

   public double abs2(int index) {
      int i = 2 * index;
      double a = values[i];
      double b = values[i + 1];
      return a * a + b * b;
   }

   public double[] abs2() {
      double[] result = new double[length];
      for (int i = 0; i < result.length; i++) {
         result[i] = abs2(i);
      }
      return result;
   }

   public float[] abs2AsFloats() {
      float[] result = new float[length];
      for (int i = 0; i < result.length; i++) {
         result[i] = (float) abs2(i);
      }
      return result;
   }

   public void add(ComplexArray other) {
      double[] otherValues = other.values;
      ArrayMath.requireSameLength(values, otherValues);
      for (int i = 0; i < values.length; i++) {
         values[i] += otherValues[i];
      }
   }

   public void multiply(double factor) {
      ArrayMath.multiply(values, factor);
   }

   public void multiply(int index, double factor) {
      int i = 2 * index;
      values[i] *= factor;
      values[i + 1] *= factor;
   }

   public void multiply(int index, double factorRe, double factorIm) {
      int i = 2 * index;
      double a = values[i];
      double b = values[i + 1];
      // double c = factorRe
      // double d = factorIm
      // (a + ib)(c + id) = ac - bd + i(ad + bc)
      values[i] = a * factorRe - b * factorIm;
      values[i + 1] = a * factorIm + b * factorRe;
   }

   public void multiply(int index, Complex factor) {
      multiply(index, factor.getReal(), factor.getImaginary());
   }

   public void multiply(double[] factors, int length) {
      for (int i = 0; i < length; i++) {
         multiply(i, factors[i]);
      }
   }

   public void multiply(ComplexArray other) {
      double[] otherValues = other.values;
      ArrayMath.requireSameLength(values, otherValues);
      for (int i = 0; i < values.length; i += 2) {
         // (a + ib)(c + id) = ac - bd + i(ad + bc)
         double a = values[i];
         double b = values[i + 1];
         double c = otherValues[i];
         double d = otherValues[i + 1];
         values[i] = a * c - b * d;
         values[i + 1] = a * d + b * c;
      }
   }

   public void divide(double divisor) {
      ArrayMath.divide(values, divisor);
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ComplexArray that
            && Arrays.equals(values, that.values);
      // Skip `length` since it is derived from `values`.
   }

   @Override
   public int hashCode() {
      return Arrays.hashCode(values);
      // Skip `length` since it is derived from `values`.
   }
}
