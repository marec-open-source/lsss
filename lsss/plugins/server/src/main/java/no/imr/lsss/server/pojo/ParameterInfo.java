package no.imr.lsss.server.pojo;

import com.google.common.base.Strings;
import no.imr.lsss.server.pojo.values.ObjectValue;
import no.imr.lsss.server.util.LsssServerUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.ValueParameter;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class ParameterInfo {
   public String path;
   public @Nullable String unit;
   public String description;
   public @Nullable List<String> allowedValues;
   public @Nullable String allowedValuesDescription;
   public @Nullable Object value;

   private ParameterInfo(String path, BaseParameter<?> parameter, ObjectValue parameterValue) {
      this.path = path;
      unit = Strings.emptyToNull(parameter.getUnit().formalName());
      description = parameter.getDescription();
      value = parameterValue.value;
   }

   public ParameterInfo(String path, ValueParameter<?> parameter) {
      this(path, parameter, LsssServerUtils.getParameterValue(parameter));

      allowedValues = parameter.getAllowedStringValues();
      if (allowedValues == null) {
         allowedValuesDescription = parameter.getAllowedValuesDescription();
      }
   }

   public ParameterInfo(String path, RangeParameter parameter) {
      this(path, parameter, LsssServerUtils.getParameterValue(parameter));

      allowedValuesDescription = parameter.getAllowedValuesDescription();
   }

   public ParameterInfo(String path, DynamicListParameter<?> parameter) {
      this(path, parameter, LsssServerUtils.getParameterValue(parameter));

      allowedValuesDescription = parameter.getAllowedValuesDescription();
   }

   public ParameterInfo(String path, ButtonParameter parameter) {
      this(path, parameter, LsssServerUtils.getButtonParameterValue());
   }

   @Override
   public String toString() {
      return "ParameterInfo{" +
            "path='" + path + '\'' +
            ", unit='" + unit + '\'' +
            ", description='" + description + '\'' +
            ", allowedValues=" + allowedValues +
            ", allowedValuesDescription='" + allowedValuesDescription + '\'' +
            ", value=" + value +
            '}';
   }
}
