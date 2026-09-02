package no.imr.tools.netcdf;

import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;
import ucar.ma2.Array;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayObject;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public final class NetcdfUtils {
   /**
    * NetcdfTime is unsigned "nanoseconds since 1601-01-01 00:00:00Z".
    * NetcdfTime as a signed long is negative since 0x8000_0000_0000_0000L, i.e., 1893-04-11T23:47:16.854775808Z.
    * NetcdfTime has a max value of 0xffff_ffff_ffff_ffffL, i.e., 2185-07-21T23:34:33.709551615Z.
    */
   private static final Instant START_NEGATIVE_NETCDF_TIME = Instant.ofEpochSecond(-2421101564L, 854775808); // 1893-04-11T23:47:16.854775808Z

   static {
      Log.global.info(NetcdfClibrary.isLibraryPresent()
            ? "NetCDF-C library version " + NetcdfClibrary.getVersion()
            : "NetCDF-C library not found.");
   }

   private NetcdfUtils() {
   }

   public static void logNetcdfCLibraryVersion() {
      // Logging done in static initializer.
   }

   public static Instant netcdfTimeToInstant(long netcdfTime) {
      return START_NEGATIVE_NETCDF_TIME.plusNanos(netcdfTime - 0x8000_0000_0000_0000L);
   }

   public static long instantToNetcdfTime(Instant instant) {
      try {
         return START_NEGATIVE_NETCDF_TIME.until(instant, ChronoUnit.NANOS) + 0x8000_0000_0000_0000L;
      } catch (ArithmeticException _) {
         throw new IllegalArgumentException(instant + " is out of range for \"nanoseconds since 1601-01-01 00:00:00Z\"");
      }
   }

   public static Group findGroup(Group parentGroup, String groupName) throws NetcdfDataException {
      Group group = parentGroup.findGroupLocal(groupName);
      if (group == null) {
         throw new NetcdfDataException("No subgroup '" + groupName + "' in group '" + parentGroup.getFullName() + "'");
      }
      return group;
   }

   public static String findAttributeString(Group group, String attributeName) throws NetcdfDataException {
      Attribute attribute = group.findAttribute(attributeName);
      if (attribute == null) {
         throw new NetcdfDataException("Attribute '" + attributeName + "' in group '" + group.getFullName() + "' is missing");
      }
      String stringValue = attribute.getStringValue();
      if (stringValue == null) {
         throw new NetcdfDataException("Attribute '" + attributeName + "' in group '" + group.getFullName() + "' is not string");
      }
      return stringValue;
   }

   public static Variable findVariable(Group group, String variableName) throws NetcdfDataException {
      Variable variable = group.findVariableLocal(variableName);
      if (variable == null) {
         throw new NetcdfDataException("No variable '" + variableName + "' in group '" + group.getFullName() + "'");
      }
      return variable;
   }

   public static @Nullable Variable findOptionalVariable(Group group, String variableName) {
      return group.findVariableLocal(variableName);
   }

   public static @Nullable Variable pathToVariable(Group group, String path) {
      String[] parts = path.split("/");
      int variableIndex = parts.length - 1;
      for (int i = 0; i < variableIndex; i++) {
         group = group.findGroupLocal(parts[i]);
         if (group == null) {
            return null;
         }
      }
      return group.findVariableLocal(parts[variableIndex]);
   }

   public static Array read(Variable variable, int[] origin, int[] shape) throws IOException {
      try {
         return variable.read(origin, shape);
      } catch (InvalidRangeException e) {
         throw new NetcdfDataException("Error reading variable " + variable.getFullName(), e);
      }
   }

   public static int readInt(Variable variable, int index) throws IOException {
      Array array = read(variable, new int[]{index}, new int[]{1});
      return array.getInt(0);
   }

   public static int[] readVariableLengthIntArray(Variable variable, int index) throws IOException {
      Array array = read(variable, new int[]{index}, new int[]{1, -1});
      ArrayInt.D1 object = (ArrayInt.D1) array.getObject(0);
      return (int[]) object.get1DJavaArray(DataType.INT);
   }

   public static int[] readVariableLengthIntArray(Variable variable, int index, int index2) throws IOException {
      Array array = read(variable, new int[]{index, index2}, new int[]{1, 1, -1});
      if (array instanceof ArrayObject.D2 array2D) {
         ArrayInt.D1 array1D = (ArrayInt.D1) array2D.get(0, 0);
         return (int[]) array1D.get1DJavaArray(DataType.INT);
      }
      if (array instanceof ArrayInt.D3) {
         return (int[]) array.get1DJavaArray(DataType.INT);
      }
      throw new NetcdfDataException("Unexpected array type for " + variable.getFullName() + ": " + array.getClass());
   }

   public static long[] readUnsignedLongArray(Variable variable) throws IOException {
      Array array = variable.read();
      return (long[]) array.get1DJavaArray(DataType.ULONG);
   }

   public static float readFloat(Variable variable) throws IOException {
      return variable.readScalarFloat();
   }

   public static float readFloat(Variable variable, int index) throws IOException {
      Array array = read(variable, new int[]{index}, new int[]{1});
      return array.getFloat(0);
   }

   public static float[] readFloatArray(Variable variable) throws IOException {
      Array array = variable.read();
      return (float[]) array.get1DJavaArray(DataType.FLOAT);
   }

   public static float[] readFloatArray(Variable variable, int index, int length) throws IOException {
      Array array = read(variable, new int[]{index, 0}, new int[]{1, length});
      return (float[]) array.get1DJavaArray(DataType.FLOAT);
   }

   public static float[] readFloatOptionalArray(Variable variable, int index, int length) throws IOException {
      if (variable.getRank() == 1) {
         float value = readFloat(variable, index);
         float[] array = new float[length];
         Arrays.fill(array, value);
         return array;
      }
      return readFloatArray(variable, index, length);
   }

   public static float[] readVariableLengthFloatArray(Variable variable, int index) throws IOException {
      Array array = read(variable, new int[]{index}, new int[]{1, -1});
      ArrayFloat.D1 object = (ArrayFloat.D1) array.getObject(0);
      return (float[]) object.get1DJavaArray(DataType.FLOAT);
   }

   public static float[] readVariableLengthFloatArray(Variable variable, int index, int index2) throws IOException {
      Array array = read(variable, new int[]{index, index2}, new int[]{1, 1, -1});
      if (array instanceof ArrayObject.D2 array2D) {
         ArrayFloat.D1 array1D = (ArrayFloat.D1) array2D.get(0, 0);
         return (float[]) array1D.get1DJavaArray(DataType.FLOAT);
      }
      if (array instanceof ArrayFloat.D3) {
         return (float[]) array.get1DJavaArray(DataType.FLOAT);
      }
      throw new NetcdfDataException("Unexpected array type for " + variable.getFullName() + ": " + array.getClass());
   }

   public static double readDouble(Variable variable, int index) throws IOException {
      Array array = read(variable, new int[]{index}, new int[]{1});
      return array.getDouble(0);
   }

   public static double readDouble(Variable variable, int index, int index2) throws IOException {
      Array array = read(variable, new int[]{index, index2}, new int[]{1, 1});
      return array.getDouble(0);
   }

   public static double[] readDoubleArray(Variable variable) throws IOException {
      Array array = variable.read();
      return (double[]) array.get1DJavaArray(DataType.DOUBLE);
   }
}
