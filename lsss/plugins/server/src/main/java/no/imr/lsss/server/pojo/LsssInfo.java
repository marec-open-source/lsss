package no.imr.lsss.server.pojo;

import no.imr.lsss.LSSS;
import no.imr.tools.Utils;
import no.imr.tools.adm.AdmService;
import no.imr.tools.adm.LicenseInfo;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class LsssInfo {
   public String version = LSSS.VERSION;
   public String buildTime = Utils.BUILD_TIME.toString();
   public String startTime = Utils.START_TIME.toString();
   public @Nullable License license;

   public LsssInfo() {
      LicenseInfo licenseInfo = AdmService.INSTANCE.getLicenseInfo();
      if (licenseInfo != null) {
         license = new License(licenseInfo);
      }
   }

   @Override
   public String toString() {
      return "LsssInfo{" +
            "version='" + version + '\'' +
            ", buildTime='" + buildTime + '\'' +
            ", startTime='" + startTime + '\'' +
            ", license=" + license +
            '}';
   }

   public static final class License {
      public String id;
      public String licensedTo;
      public String validUntil;
      public List<String> features;

      public License(LicenseInfo licenseInfo) {
         id = licenseInfo.id();
         licensedTo = licenseInfo.licensedTo();
         validUntil = licenseInfo.expiration().toString();
         features = licenseInfo.features();
      }

      @Override
      public String toString() {
         return "License{" +
               "id='" + id + '\'' +
               ", licensedTo='" + licensedTo + '\'' +
               ", validUntil='" + validUntil + '\'' +
               ", features=" + features +
               '}';
      }
   }
}
