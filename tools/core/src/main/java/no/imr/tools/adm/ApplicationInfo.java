package no.imr.tools.adm;

import java.awt.Image;

public record ApplicationInfo(
      String shortName,
      String longName,
      String appName,
      String version,
      Image image,
      Image bigImage,
      String installationSubDir,
      String applicationDataSubDir
) {
   public ApplicationInfo {
      assert shortName.matches("\\w+") : shortName;
   }

   public String getStartupScriptName() {
      return appName;
   }
}
