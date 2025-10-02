package no.imr.lsss.server.pojo;

import no.imr.lsss.framework.config.ConfigurationUnit;

public final class ConfigurationUnitInfo {
   public String id;

   public ConfigurationUnitInfo(ConfigurationUnit configurationUnit) {
      id = configurationUnit.getPersistentName();
   }

   @Override
   public String toString() {
      return "ConfigurationUnitInfo{" +
            "id='" + id + '\'' +
            '}';
   }
}
