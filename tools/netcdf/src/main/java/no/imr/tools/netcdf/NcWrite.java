package no.imr.tools.netcdf;

import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.Variable;
import ucar.nc2.constants.CDM;
import ucar.nc2.constants.CF;
import ucar.nc2.write.Nc4ChunkingDefault;
import ucar.nc2.write.NetcdfFileFormat;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class NcWrite {
   private NcWrite() {
   }

   public static NetcdfFormatWriter.Builder newBuilder(Path file) {
      return NetcdfFormatWriter.builder()
            .setNewFile(true)
            .setFormat(NetcdfFileFormat.NETCDF4)
            .setLocation(file.toString())
            .setChunker(new Nc4ChunkingDefault(5, true));
   }

   public static void addFloatVariable(Group.Builder builder, String name,
                                       List<Dimension> dimensions, List<String> coordinates) {
      Variable.Builder<?> variableBuilder = addVariable(builder, name, DataType.FLOAT, dimensions)
            .addAttribute(new Attribute(CDM.FILL_VALUE, Float.NaN));
      if (!coordinates.isEmpty()) {
         variableBuilder
               .addAttribute(new Attribute(CF.COORDINATES, String.join(" ", coordinates)));
      }
   }

   public static void addDoubleVariable(Group.Builder builder, String name,
                                        List<Dimension> dimensions, List<String> coordinates) {
      Variable.Builder<?> variableBuilder = addVariable(builder, name, DataType.DOUBLE, dimensions)
            .addAttribute(new Attribute(CDM.FILL_VALUE, Double.NaN));
      if (!coordinates.isEmpty()) {
         variableBuilder
               .addAttribute(new Attribute(CF.COORDINATES, String.join(" ", coordinates)));
      }
   }

   public static Variable.Builder<?> addVariable(Group.Builder builder, String name, DataType dataType, List<Dimension> dims) {
      Variable.Builder<?> variableBuilder = Variable.builder()
            .setName(name)
            .setDataType(dataType)
            .setDimensions(dims);
      builder.addVariable(variableBuilder);
      return variableBuilder;
   }

   public static Dimension addDimension(Group.Builder builder, String name, int length) {
      Dimension dimension = new Dimension(name, length);
      builder.addDimension(dimension);
      return dimension;
   }

   public static Dimension addUnlimitedDimension(Group.Builder builder, String name) {
      Dimension dimension = Dimension.builder()
            .setName(name)
            .setIsUnlimited(true)
            .build();
      builder.addDimension(dimension);
      return dimension;
   }

   public static void writeScalarFloat(NetcdfFormatWriter writer, Variable variable, float value) throws InvalidRangeException, IOException {
      ArrayFloat.D0 array = new ArrayFloat.D0();
      array.set(value);
      writer.write(variable, array);
   }
}
