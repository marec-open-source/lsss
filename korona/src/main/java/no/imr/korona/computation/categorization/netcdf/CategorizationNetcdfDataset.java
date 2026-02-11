package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.computation.netcdf.NcAnnotation;
import ucar.ma2.DataType;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFiles;
import ucar.nc2.Variable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class CategorizationNetcdfDataset {
   private static final Object LOCK = new Object();
   private static final Map<Path, CategorizationNetcdfDataset> FILE_TO_DATASET = new HashMap<>();

   private final Path ncFile;
   private final NetcdfFile dataset;
   final NcTimeVariable ncTimeVariable;
   final int[] categories;
   final double[] ranges;
   final Variable annotationVar;

   private int openCounter;

   private CategorizationNetcdfDataset(Path ncFile) throws IOException {
      this.ncFile = ncFile;
      dataset = NetcdfFiles.open(ncFile.toString());

      try {
         Variable pingTimeVar = findVariable(NcAnnotation.PING_TIME);
         ncTimeVariable = new NcTimeVariable(pingTimeVar);

         Variable rangeVar = findVariable(NcAnnotation.RANGE);
         ranges = (double[]) rangeVar.read().get1DJavaArray(DataType.DOUBLE);

         Variable categoryVar = findVariable(NcAnnotation.CATEGORY);
         categories = (int[]) categoryVar.read().get1DJavaArray(DataType.INT);

         annotationVar = findVariable(NcAnnotation.ANNOTATION);

      } catch (Exception e) {
         try {
            dataset.close();
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
   }

   static CategorizationNetcdfDataset open(Path ncFile) throws IOException {
      synchronized (LOCK) {
         CategorizationNetcdfDataset dataset = FILE_TO_DATASET.get(ncFile);
         if (dataset == null) {
            dataset = new CategorizationNetcdfDataset(ncFile);
            FILE_TO_DATASET.put(ncFile, dataset);
         }
         dataset.openCounter++;
         return dataset;
      }
   }

   private Variable findVariable(String name) throws IOException {
      Variable variable = dataset.findVariable(name);
      if (variable == null) {
         throw new IOException("No variable " + name + " in " + ncFile);
      }
      return variable;
   }

   void close() throws IOException {
      synchronized (LOCK) {
         openCounter--;
         if (openCounter == 0) {
            FILE_TO_DATASET.remove(ncFile);
            dataset.close();
         }
      }
   }
}
