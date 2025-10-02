package no.imr.korona.test;

import no.imr.korona.Korona;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.track.SegmentHandle;

import java.nio.file.Path;
import java.util.List;

public final class KoronaTestUtils {
   private KoronaTestUtils() {
   }

   public static Configurator createConfigurator(Korona korona) {
      ConfigFileSettings configFileSettings = korona.createConfigFileSettings();
      configFileSettings.restoreInstallationLocations();
      return new Configurator(configFileSettings, null);
   }

   public static SegmentHandle getSingleSegmentHandleInDir(Korona korona, Path dir) {
      List<SegmentHandle> segmentHandles = korona.getDataFormatManager().createSegmentHandlesInDirectory(dir);
      if (segmentHandles.size() != 1) {
         throw new IllegalStateException(dir + " contains " + segmentHandles.size() + " data files");
      }
      return segmentHandles.getFirst();
   }
}
