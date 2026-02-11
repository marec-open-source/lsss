package no.imr.korona.computation.dataquality;

import no.imr.korona.Korona;
import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.ModuleConfigurationException;
import no.imr.korona.computation.ModuleUtils;
import no.imr.korona.computation.SimplePingModuleComputation;
import no.imr.korona.computation.netcdf.Nc;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.netcdf.NcWrite;
import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;
import ucar.nc2.write.NetcdfFormatWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

final class DataQualityModuleComputation extends SimplePingModuleComputation {
   private final Path ncFile;
   private final int referenceChannel;
   private final List<DataQualityIndicator> dataQualityIndicators = new ArrayList<>();
   private final List<Long> timeInMillisList = new ArrayList<>();
   private final List<Float> bottomDepthList = new ArrayList<>();

   DataQualityModuleComputation(DataQualityModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      Path koronaDirectory = computationContext.getAssociatedKoronaDirectory();
      if (koronaDirectory == null) {
         throw new ModuleConfigurationException(module, "No destination directory configured");
      }
      Path ncDir = koronaDirectory.resolve(module.dirName.getValue());
      FileUtils.createDirectories(ncDir);
      String ncFileName = FileUtils.baseName(computationContext.getPingReader().getFile()) + ".nc";
      ncFile = ncDir.resolve(ncFileName);

      referenceChannel = ModuleUtils.getMainChannelOrThrow(this, module.mainFrequency.getValue());

      dataQualityIndicators.add(new RollIndicator(referenceChannel));
      dataQualityIndicators.add(new ImpedanceIndicator(referenceChannel));
   }

   @Override
   protected void processPing(Ping ping) {
      timeInMillisList.add(ping.getTimeInMillis());
      bottomDepthList.add((float) ping.getBot0Datagram().getChannelDepths()[referenceChannel - 1]);
      for (DataQualityIndicator dataQualityIndicator : dataQualityIndicators) {
         dataQualityIndicator.processPing(ping);
      }
   }

   @Override
   protected void endOfInput() throws IOException {
      NetcdfFormatWriter.Builder fileBuilder = NcWrite.newBuilder(ncFile)
            .addAttribute(new Attribute("content_type_name", "CRIMAC-quality-control"))
            .addAttribute(new Attribute("content_type_version", "0.1"))
            .addAttribute(new Attribute("content_type_description", "Data quality indicators"))
            .addAttribute(new Attribute("producer_name", "KORONA"))
            .addAttribute(new Attribute("producer_version", Korona.VERSION))
            .addAttribute(new Attribute("producer_git_commit", Utils.GIT_COMMIT))
            .addAttribute(new Attribute("creation_time", Instant.now().truncatedTo(ChronoUnit.SECONDS).toString()));

      long referenceTimeInMillis = getPingConfiguration().getRawFileConfiguration().getTimeInMillis();

      Dimension pingTimeDim = fileBuilder.addDimension(Nc.PING_TIME, timeInMillisList.size());

      fileBuilder.addVariable(Nc.PING_TIME, DataType.LONG, List.of(pingTimeDim))
            .addAttribute(new Attribute(CF.CALENDAR, "proleptic_gregorian"))
            .addAttribute(new Attribute(CF.UNITS, "nanoseconds since " + Instant.ofEpochMilli(referenceTimeInMillis)));

      for (DataQualityIndicator dataQualityIndicator : dataQualityIndicators) {
         NcVariableInfo info = dataQualityIndicator.variableInfo();
         Variable.Builder<?> variableBuilder = fileBuilder.addVariable(info.name(), DataType.FLOAT, List.of(pingTimeDim));
         if (!info.unit().isEmpty()) {
            variableBuilder.addAttribute(new Attribute(CF.UNITS, info.unit()));
         }
      }

      try (NetcdfFormatWriter writer = fileBuilder.build()) {
         long[] timeInMillis = Utils.toLongs(timeInMillisList);
         long[] pingTimes = LongStream.of(timeInMillis)
               .map(t -> (t - referenceTimeInMillis) * 1_000_000)
               .toArray();
         float[] bottomDepths = Utils.toFloats(bottomDepthList);

         writer.write(writer.findVariable(Nc.PING_TIME), new int[]{0}, Array.makeFromJavaArray(pingTimes));

         for (DataQualityIndicator dataQualityIndicator : dataQualityIndicators) {
            float[] values = dataQualityIndicator.computeResult(timeInMillis, bottomDepths);
            writer.write(writer.findVariable(dataQualityIndicator.variableInfo().name()), new int[]{0}, Array.makeFromJavaArray(values));
         }

      } catch (InvalidRangeException e) {
         throw new IOException("Error writing to " + ncFile, e);
      }
   }
}
