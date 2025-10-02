package no.imr.lsss.database.ices;

import no.imr.lsss.framework.config.survey.misc.ices.IcesCode;
import no.imr.lsss.framework.config.survey.misc.ices.IcesUtils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.StringParameter;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class IdRefParameter extends StringParameter {
   private final String baseSchemaName;
   private List<IcesCode> icesCodes = List.of();

   IdRefParameter(Name name, String baseSchemaName) {
      super(name);

      this.baseSchemaName = baseSchemaName;
   }

   IdRefParameter(Name name) {
      this(name, name.persistentName());
   }

   public String getSchemaName() {
      return IcesUtils.AC_SCHEMA_PREFIX + baseSchemaName;
   }

   public void setIcesCodes(List<IcesCode> icesCodes) {
      this.icesCodes = icesCodes;
   }

   @Override
   public List<String> getSuggestedValues() {
      return icesCodes.stream()
            .map(IcesCode::key)
            .toList();
   }

   @Override
   public @Nullable String toTooltip(String value) {
      return icesCodes.stream()
            .filter(icesCode -> icesCode.key().equals(value))
            .findFirst()
            .map(IcesCode::description)
            .orElse(null);
   }
}
