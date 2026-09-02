package no.imr.korona.computation.categorization.netcdf;

import no.imr.korona.computation.netcdf.NcAnnotation;
import no.imr.tools.Utils;
import no.imr.tools.netcdf.NetcdfUtils;
import ucar.ma2.DataType;
import ucar.nc2.Group;
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
         Group group = dataset.getRootGroup();

         Variable pingTimeVar = NetcdfUtils.findVariable(group, NcAnnotation.PING_TIME);
         ncTimeVariable = new NcTimeVariable(pingTimeVar);

         Variable rangeVar = NetcdfUtils.findVariable(group, NcAnnotation.RANGE);
         ranges = (double[]) rangeVar.read().get1DJavaArray(DataType.DOUBLE);

         Variable categoryVar = NetcdfUtils.findVariable(group, NcAnnotation.CATEGORY);
         categories = (int[]) categoryVar.read().get1DJavaArray(DataType.INT);

         annotationVar = NetcdfUtils.findVariable(group, NcAnnotation.ANNOTATION);

      } catch (Exception e) {
         Utils.closeOrSuppress(e, dataset);
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
