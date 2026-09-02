package no.imr.tools.netcdf;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

public final class NcBuild {
   private NcBuild() {
   }

   public static NetcdfFormatWriter.Builder newBuilder(Path file) {
      return NetcdfFormatWriter.builder()
            .setNewFile(true)
            .setFormat(NetcdfFileFormat.NETCDF4)
            .setLocation(file.toString())
            .setChunker(new Nc4ChunkingDefault(5, true));
   }

   public static Attribute coordinatesAttribute(List<String> coordinates) {
      return new Attribute(CF.COORDINATES, String.join(" ", coordinates));
   }

   public static Variable.Builder<?> floatVariable(String name, List<Dimension> dimensions) {
      return newVariable(name, DataType.FLOAT, dimensions)
            .addAttribute(new Attribute(CDM.FILL_VALUE, Float.NaN));
   }

   public static Variable.Builder<?> floatVariable(String name, List<Dimension> dimensions, List<String> coordinates) {
      return floatVariable(name, dimensions)
               .addAttribute(coordinatesAttribute(coordinates));
   }

   public static Variable.Builder<?> doubleVariable(String name, List<Dimension> dimensions) {
      return newVariable(name, DataType.DOUBLE, dimensions)
            .addAttribute(new Attribute(CDM.FILL_VALUE, Double.NaN));
   }

   public static Variable.Builder<?> timeVariable(String name, List<Dimension> dimensions, Instant referenceTime) {
      return newVariable(name, DataType.LONG, dimensions)
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + referenceTime));
   }

   public static Variable.Builder<?> newVariable(String name, DataType dataType, List<Dimension> dims) {
      return Variable.builder()
            .setName(name)
            .setDataType(dataType)
            .setDimensions(dims);
   }

   public static Dimension addDimension(Group.Builder groupBuilder, String name, int length) {
      Dimension dimension = new Dimension(name, length);
      groupBuilder.addDimension(dimension);
      return dimension;
   }

   public static Dimension addUnlimitedDimension(Group.Builder groupBuilder, String name) {
      Dimension dimension = Dimension.builder()
            .setName(name)
            .setIsUnlimited(true)
            .build();
      groupBuilder.addDimension(dimension);
      return dimension;
   }

   public static Dimension findDimensionLocal(Group.Builder groupBuilder, String name) {
      return groupBuilder.findDimensionLocal(name).orElseThrow(() -> {
         return new IllegalArgumentException("No dimension " + name + " in " + groupBuilder.makeFullName());
      });
   }
}
