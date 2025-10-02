package no.imr.lsss.framework.export;

import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.Region;
import no.imr.korona.util.KoronaUtils;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.ProgressHandler;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.ConfigurableContainer;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.FloatRangeSet;
import no.imr.tools.range.RangeSet;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Base class for exporters.
 */
public abstract class Exporter extends Configurable {
   private static final char SEPARATOR = ',';
   protected static final String MISSING_LAT_LON = "NaN";

   private final LSSS lsss;
   private final FeaturePlugin plugin;
   public final Name name;
   public final BooleanParameter enabled = new BooleanParameter(new Name("Enabled"), false);
   public final FileParameter outputDirectory;

   private String fileNamePrefix = "";

   protected Exporter(FeaturePlugin plugin, Name name, String description) {
      super(name);

      lsss = plugin.getLSSS();
      this.plugin = plugin;
      this.name = name;
      outputDirectory = new FileParameter(new Name("Directory", name.displayName()),
            null, FileParameter.Mode.DIRECTORY,
            description);
   }

   Configurable getSettingsConfigurable() {
      return new ConfigurableContainer(new Name("Settings", name.displayName()), getSettingsParameters());
   }

   protected LSSS getLSSS() {
      return lsss;
   }

   public FeaturePlugin getPlugin() {
      return plugin;
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return List.of(enabled, outputDirectory, getSettingsConfigurable());
   }

   protected List<? extends BaseParameter<?>> getSettingsParameters() {
      return List.of();
   }

   protected void initExportFiles(PingRange pingRange, ExportFile... exportFiles) {
      String baseInfix = getFileNameInfix(pingRange);
      initExportFiles(baseInfix, exportFiles);
   }

   protected void initExportFiles(String baseInfix, ExportFile... exportFiles) {
      for (int i = 0; true; i++) {
         String infix = baseInfix + (i == 0 ? "" : "_" + Utils.format("%02d", i));
         if (!doAnyExist(infix, exportFiles)) {
            break;
         }
      }
   }

   private boolean doAnyExist(String infix, ExportFile... exportFiles) {
      Path dir = outputDirectory.getFile();
      if (dir == null) {
         return false;
      }
      for (ExportFile exportFile : exportFiles) {
         exportFile.setInfix(dir, fileNamePrefix, infix);
         if (Files.exists(exportFile.getFile())) {
            return true;
         }
      }
      return false;
   }

   protected static String getFileNameInfix(PingRange pingRange) {
      DatabaseTime beginDatabaseTime = new DatabaseTime(pingRange.begin().getTimeInMillis());
      DatabaseTime endDatabaseTime = new DatabaseTime(pingRange.end().getTimeInMillis());
      return "_T" + beginDatabaseTime.getDate() + "_" + Utils.format("%08d", beginDatabaseTime.getTime())
            + "-" + endDatabaseTime.getDate() + "_" + Utils.format("%08d", endDatabaseTime.getTime());
   }

   protected static ExportFile createCdsFileWrapper() {
      return new ExportFile("KoronaModuleSetup", KoronaUtils.CDS_FILE_TYPE.suffix());
   }

   protected void writeModuleSetup(ExportFile exportFile, DataFileSet dataFileSet, PingRange pingRange) throws IOException {
      PingConfiguration pingConfiguration = dataFileSet.getDataFile(pingRange.begin()).getPingConfiguration();
      List<Cds0Datagram> cdsDatagrams = pingConfiguration.getConfigurationItems(Cds0Datagram.class).toList();
      if (cdsDatagrams.isEmpty()) {
         return;
      }
      ModuleContainer moduleContainer = new ModuleContainer(lsss.getKorona());
      for (Cds0Datagram cds0Datagram : cdsDatagrams) {
         moduleContainer.appendXml(cds0Datagram.getDocument().getRootElement());
      }
      moduleContainer.writeConfiguration(exportFile.getFile());
   }

   protected static void writeStandardHeader(PrintWriter out, String formatSinceLsssVersion) {
      out.println("# Lines starting with # are comments");
      out.println("# The format of and definitions in this file may change in future versions of LSSS,"
            + " last changed in LSSS version " + formatSinceLsssVersion);
      out.println("# Export time: " + new Date() + ", LSSS version " + LSSS.VERSION);
      out.println("#");
   }

   protected static void writeColumnNames(PrintWriter out, List<String> columns) {
      out.print("# ");
      writeValues(out, columns);
      List<String> columnsWithoutUnit = columns.stream()
            .map(column -> column.replaceAll("\\[.*]$", ""))
            .toList();
      writeValues(out, columnsWithoutUnit);
   }

   protected static void writeValues(PrintWriter out, List<String> values) {
      Utils.write(out, values, SEPARATOR);
   }

   protected FloatRangeSet getDepthRanges(Collection<? extends Region> regions, Ping ping, int channel) {
      FloatRangeSet depthRanges = FloatRangeSet.of();
      for (Region region : regions) {
         depthRanges = depthRanges.add(lsss.getRegionManager().getDepthRangesForChannel(region, ping, channel));
      }
      return depthRanges;
   }

   protected RangeSet<PingIndex> getVisiblePingRangeSet(Collection<? extends Region> regions) {
      PingRange currentPingRange = lsss.getInterpretationSettings().getPingRange();
      RangeSet<PingIndex> visiblePingRangeSet = new ArrayRangeSet<>();
      for (Region region : regions) {
         PingRange pingRange = region.getPingRange();
         visiblePingRangeSet.add(currentPingRange.intersection(pingRange));
      }
      return visiblePingRangeSet;
   }

   protected List<PingIndex> getVisiblePingIndexes(Collection<? extends Region> regions) {
      RangeSet<PingIndex> visiblePingRangeSet = getVisiblePingRangeSet(regions);
      return visiblePingRangeSet.stream()
            .flatMap(lsss.getInterpretationSettings().getDataFileSet()::getPingIndexStream)
            .toList();
   }

   void export(String fileNamePrefix, AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException {
      this.fileNamePrefix = fileNamePrefix;
      Path dir = outputDirectory.getFile();
      if (dir == null) {
         return;
      }
      FileUtils.createDirectories(dir);
      doExport(asyncHandle, progressHandler);
   }

   protected abstract void doExport(AsyncHandle asyncHandle, ProgressHandler progressHandler) throws IOException;
}
