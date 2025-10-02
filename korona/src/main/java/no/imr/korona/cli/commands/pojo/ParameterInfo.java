package no.imr.korona.cli.commands.pojo;

import org.jspecify.annotations.Nullable;

import java.util.List;

public final class ParameterInfo {
   public String id;
   public String description;
   public @Nullable String unit;
   public @Nullable List<String> allowedValues;
   public @Nullable String allowedValuesDescription;
   public @Nullable Object defaultValue;
   public @Nullable List<ParameterInfo> parameters; // Used by MultiParameter.

   public ParameterInfo(String id, String description) {
      this.id = id;
      this.description = description;
   }
}
