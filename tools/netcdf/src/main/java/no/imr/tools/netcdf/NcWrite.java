package no.imr.tools.netcdf;

import ucar.ma2.Array;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayLong;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Variable;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;

public final class NcWrite {
   private NcWrite() {
   }

   // --- Scalars

   public static void floatD0(NetcdfFormatWriter writer, Variable variable, float value) throws InvalidRangeException, IOException {
      ArrayFloat.D0 array = new ArrayFloat.D0();
      array.set(value);
      writer.write(variable, array);
   }

   // --- 1D arrays

   public static void uintD1(NetcdfFormatWriter writer, Variable variable, int i, int value) throws InvalidRangeException, IOException {
      ArrayInt.D1 array = new ArrayInt.D1(1, false);
      array.set(0, value);
      writer.write(variable, new int[]{i}, array);
   }

   public static void longD1(NetcdfFormatWriter writer, Variable variable, int i, long value) throws InvalidRangeException, IOException {
      ArrayLong.D1 array = new ArrayLong.D1(1, false);
      array.set(0, value);
      writer.write(variable, new int[]{i}, array);
   }

   public static void floatD1(NetcdfFormatWriter writer, Variable variable, int i, float value) throws InvalidRangeException, IOException {
      ArrayFloat.D1 array = new ArrayFloat.D1(1);
      array.set(0, value);
      writer.write(variable, new int[]{i}, array);
   }

   public static void doubleD1(NetcdfFormatWriter writer, Variable variable, int i, double value) throws InvalidRangeException, IOException {
      ArrayDouble.D1 array = new ArrayDouble.D1(1);
      array.set(0, value);
      writer.write(variable, new int[]{i}, array);
   }

   // --- 2D arrays

   public static void floatD2(NetcdfFormatWriter writer, Variable variable, int i0, int i1, float[] values) throws InvalidRangeException, IOException {
      writer.write(variable, new int[]{i0, i1}, Array.makeFromJavaArray(new float[][]{values}));
   }

   // --- 3D arrays

   public static void shortD3(NetcdfFormatWriter writer, Variable variable, int i0, int i1, int i2, short[] values) throws InvalidRangeException, IOException {
      writer.write(variable, new int[]{i0, i1, i2}, Array.makeFromJavaArray(new short[][][]{{values}}));
   }

   public static void floatD3(NetcdfFormatWriter writer, Variable variable, int i0, int i1, int i2, float[] values) throws InvalidRangeException, IOException {
      writer.write(variable, new int[]{i0, i1, i2}, Array.makeFromJavaArray(new float[][][]{{values}}));
   }
}
