package no.imr.korona.cli.commands.pojo;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class KoronaModuleInfo {
   public String id;
   public String description;
   public @Nullable String plugin; // Set if different from the KORONA base system.
   public @Nullable List<String> requiredConfigFiles;
   public @Nullable List<String> optionalConfigFiles;
   public @Nullable List<ParameterInfo> parameters;
   public @Nullable String error; // Set if error on evaluating info for this module.

   public KoronaModuleInfo(String id, String description) {
      this.id = id;
      this.description = description;
   }
}
