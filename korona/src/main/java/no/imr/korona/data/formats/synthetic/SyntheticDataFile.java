package no.imr.korona.data.formats.synthetic;

import no.imr.korona.data.DataException;
import no.imr.tools.Utils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A file that represents synthetic data.
 * File name syntax:
 * <pre>{@code
 * <class name>[@<parameter name>=<value>]*[@<first ping number>-<last ping number>].lsss-ss
 * }</pre>
 * Ping range is {@code [<first ping number>, <last ping number> + 1)}.
 *
 * @see SyntheticData
 */
final class SyntheticDataFile {
   private final Path file;
   private final SyntheticData syntheticData;

   SyntheticDataFile(Path file) throws DataException {
      this.file = file;
      syntheticData = toSyntheticData(file);
   }

   SyntheticDataFile(SyntheticData syntheticData) {
      file = toFile(syntheticData);
      this.syntheticData = syntheticData;
   }

   Path getFile() {
      return file;
   }

   SyntheticData getSyntheticData() {
      return syntheticData;
   }

   private static SyntheticData toSyntheticData(Path file) throws DataException {
      String name = file.getFileName().toString();
      if (!name.endsWith(SyntheticDataFormatPlugin.LSSS_SS_SUFFIX)) {
         throw new DataException("Unknown suffix: " + file);
      }
      name = name.substring(0, name.length() - SyntheticDataFormatPlugin.LSSS_SS_SUFFIX.length());
      String[] parts = name.split("@");
      try {
         String className = parts[0];
         Class<? extends SyntheticData> clazz = Class.forName(className).asSubclass(SyntheticData.class);
         SyntheticData syntheticData = clazz.getDeclaredConstructor().newInstance();

         for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            if (part.matches(".+=.+")) {
               String[] nameAndVal = part.split("=", 2);
               syntheticData.setParameter(nameAndVal[0], nameAndVal[1]);
            } else if (part.matches("\\d+-\\d+")) {
               String[] range = part.split("-");
               long firstPingNumber = Long.parseLong(range[0]);
               long lastPingNumber = Long.parseLong(range[1]);
               syntheticData.setFirstAndLastPingNumber(firstPingNumber, lastPingNumber);
            } else {
               throw new DataException("Cannot parse " + part + " in " + file);
            }
         }
         return syntheticData;
      } catch (Exception e) {
         throw new DataException("Error creating data definition: " + file, e);
      }
   }

   private static Path toFile(SyntheticData syntheticData) {
      StringBuilder fileName = new StringBuilder(syntheticData.getClass().getName());
      Map<String, String> parameters = new LinkedHashMap<>();
      syntheticData.addParameters(parameters);
      parameters.forEach((key, value) -> {
         fileName.append("@" + key + "=" + value);
      });
      fileName.append("@" + syntheticData.getFirstPingNumber() + "-" + syntheticData.getLastPingNumber() + SyntheticDataFormatPlugin.LSSS_SS_SUFFIX);
      return Utils.getTmpDir().resolve(fileName.toString());
   }
}
