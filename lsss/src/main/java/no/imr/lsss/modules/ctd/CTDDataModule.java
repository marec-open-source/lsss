package no.imr.lsss.modules.ctd;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Level;

public final class CTDDataModule extends BaseDataModule {
   private static final Predicate<Path> CTD_FILE_FILTER = file -> {
      String name = file.getFileName().toString();
      return name.length() == 11 && Utils.startsWithIgnoringCase(name, "sta") && Utils.endsWithIgnoringCase(name, "cnv");
   };
   private static final DateTimeFormatter CNV_DATE_TIME_FORMATTER = Utils.createUTCDateTimeFormatter("MMM dd yyy HH:mm:ss"); // Nov 24 2002 05:48:25

   private final ChangeManager changeManager = new ChangeManager();
   private List<CTDData> ctdDatas = List.of();

   public CTDDataModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   ChangeManager getChangeManager() {
      return changeManager;
   }

   List<CTDData> getCTDDatas() {
      return ctdDatas;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::updateDataDir), List.of(
            getConfigurationManager().getDataConf().getDir(DataConfLSSS.CTD_SUB_DIR),
            getInterpretationSettings().getReloadChangeManager()
      ));
   }

   private void updateDataDir() {
      List<CTDData> newCtdDatas = new ArrayList<>();
      getConfigurationManager().getDataConf().getDir(DataConfLSSS.CTD_SUB_DIR).getValue().ifPresent(dir -> {
         new WorkerDialog(getLSSS()::getReferenceComponent, "Loading CTD data from\n" + dir)
               .setModalDialog(false)
               .start(asyncHandle -> {
                  for (Path file : FileUtils.listFiles(dir, asyncHandle, CTD_FILE_FILTER)) {
                     if (asyncHandle.isCancelled()) {
                        break;
                     }
                     try (BufferedReader reader = Files.newBufferedReader(file, Utils.ISO_8859_1)) {
                        newCtdDatas.add(loadCnvFile(file, reader));
                     } catch (Exception e) {
                        Log.global.log(Level.WARNING, "Error loading CTD file " + file + ": " + e.getMessage(), e);
                     }
                  }
                  newCtdDatas.sort(Comparator.comparingLong(CTDData::timeInMillis));
               });
      });
      ctdDatas = List.copyOf(newCtdDatas);
      changeManager.notifyListeners();
   }

   static CTDData loadCnvFile(@Nullable Path file, BufferedReader reader) throws IOException {
      long timeInMillis = 0;
      double longitude = Double.NaN;
      double latitude = Double.NaN;
      String stationNumber = "";
      List<String> columnNames = new ArrayList<>();

      while (true) {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         if (line.startsWith("* System UpLoad Time = ")) { // * System UpLoad Time = Nov 24 2002 06:17:31
            timeInMillis = CNV_DATE_TIME_FORMATTER.parse(trimmedStringAfter(line, '='), Instant::from).toEpochMilli();
         }
         if (line.startsWith("** Station: ")) { // ** Station: 1014
            stationNumber = trimmedStringAfter(line, ':');
         }
         if (line.startsWith("* NMEA Latitude = ")) { // * NMEA Latitude = 68 21.94 N
            String[] parts = trimmedStringAfter(line, '=').split(" ");
            if (parts.length > 2) {
               latitude = Integer.parseInt(parts[0]) + Double.parseDouble(parts[1]) / 60;
               if (parts[2].equalsIgnoreCase("S")) {
                  latitude = -latitude;
               }
            }
         }
         if (line.startsWith("* NMEA Longitude = ")) { // * NMEA Longitude = 016 05.84 E
            String[] parts = trimmedStringAfter(line, '=').split(" ");
            if (parts.length > 2) {
               longitude = Integer.parseInt(parts[0]) + Double.parseDouble(parts[1]) / 60;
               if (parts[2].equalsIgnoreCase("W")) {
                  longitude = -longitude;
               }
            }
         }
         if (line.startsWith("# name ")) { // # name 1 = pr: pressure [db]
            columnNames.add(trimmedStringAfter(line, ':'));
         }
         if (line.equalsIgnoreCase("*END*")) {
            break;
         }
      }

      List<float[]> rows = new ArrayList<>();
      while (true) {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         line = line.trim();
         if (line.isEmpty()) {
            continue;
         }
         String[] parts = line.split("\\s+");
         if (parts.length != columnNames.size()) {
            Log.global.warning("Wrong number of values in CTD file " + file + ": " + line);
            continue;
         }
         float[] row = new float[columnNames.size()];
         try {
            for (int i = 0; i < row.length; i++) {
               row[i] = Float.parseFloat(parts[i]);
            }
         } catch (NumberFormatException e) {
            Log.global.warning("Error parsing values in CTD file " + file + ": " + line);
            continue;
         }
         rows.add(row);
      }

      int depthColumn = findColumnIndex(columnNames, "pressure", 1);
      return new CTDData(file, timeInMillis, stationNumber, new GeoPoint(longitude, latitude), depthColumn,
            List.copyOf(columnNames), List.copyOf(rows));
   }

   private static String trimmedStringAfter(String line, char ch) {
      int i = line.indexOf(ch);
      if (i < 0) {
         return line;
      }
      return Utils.trimmedSubstring(line, i + 1, line.length());
   }

   static int findColumnIndex(List<String> columnNames, String namePart, int defaultValue) {
      for (int i = 0; i < columnNames.size(); i++) {
         if (Utils.containsIgnoringCase(columnNames.get(i), namePart)) {
            return i;
         }
      }
      return defaultValue;
   }
}
