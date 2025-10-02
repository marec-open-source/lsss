package no.imr.lsss.server.pojo;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.packages.UserDefinedPackage;
import no.imr.lsss.framework.packages.LsssPackage;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class ApiLsssPackage {
   public String id;
   public String label;
   public String version;
   public @Nullable List<ApiLsssAction> actions;

   public ApiLsssPackage(LsssPackage lsssPackage) {
      id = lsssPackage.getId();
      UserDefinedPackage userDefinedPackage = lsssPackage.getUserDefinedPackage();
      if (userDefinedPackage != null) {
         label = userDefinedPackage.info.label;
         version = userDefinedPackage.info.version;
      } else {
         label = id;
         version = LSSS.VERSION;
      }
   }

   @Override
   public String toString() {
      return "ApiLsssPackage{" +
            "id='" + id + '\'' +
            ", label='" + label + '\'' +
            ", version='" + version + '\'' +
            '}';
   }
}
