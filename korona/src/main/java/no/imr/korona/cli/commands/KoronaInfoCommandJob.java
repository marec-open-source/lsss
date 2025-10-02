package no.imr.korona.cli.commands;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;
import no.imr.korona.Korona;
import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.cli.commands.pojo.KoronaInfo;
import no.imr.korona.cli.commands.pojo.KoronaModuleInfo;
import no.imr.korona.cli.commands.pojo.ParameterInfo;
import no.imr.korona.computation.BaseModule;
import no.imr.korona.computation.KoronaModulePlugin;
import no.imr.korona.computation.ModuleInfo;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DynamicListParameter;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.RangeParameter;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.VoidParameter;
import no.imr.tools.plugins.BasePlugin;
import no.imr.tools.range.FloatRange;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class KoronaInfoCommandJob extends CliCommandJob {
   private final Korona korona = new Korona();

   KoronaInfoCommandJob() {
   }

   @Override
   public void run(InputStream in, PrintStream out) throws Exception {
      KoronaInfo koronaInfo = new KoronaInfo();

      koronaInfo.activePlugins = korona.getModuleManager().getModuleInfos().stream()
            .map(ModuleInfo::modulePlugin)
            .map(BasePlugin::getPersistentName)
            .distinct()
            .sorted()
            .toList();

      koronaInfo.modules = korona.getModuleManager().getModuleInfos().stream()
            .map(this::toPojoModuleInfo)
            .sorted(Comparator.<KoronaModuleInfo, String>comparing(info -> Strings.nullToEmpty(info.plugin))
                  .thenComparing(info -> Strings.nullToEmpty(info.id)))
            .toList();

      JsonUtils.PRETTY_PRINTER.writeValue(out, koronaInfo);
   }

   private KoronaModuleInfo toPojoModuleInfo(ModuleInfo moduleInfo) {
      KoronaModuleInfo info = new KoronaModuleInfo(moduleInfo.getPersistentName(), moduleInfo.description());
      if (!(moduleInfo.modulePlugin() instanceof KoronaModulePlugin)) {
         info.plugin = moduleInfo.modulePlugin().getPersistentName();
      }

      try {
         BaseModule module = korona.getModuleManager().createModule(moduleInfo.getPersistentName());

         List<String> requiredConfigFiles = module.getRequiredConfigFileServiceNames().stream()
               .map(Name::persistentName)
               .toList();
         if (!requiredConfigFiles.isEmpty()) {
            info.requiredConfigFiles = requiredConfigFiles;
         }
         List<String> optionalConfigFiles = module.getOptionalConfigFileServiceNames().stream()
               .map(Name::persistentName)
               .toList();
         if (!optionalConfigFiles.isEmpty()) {
            info.optionalConfigFiles = optionalConfigFiles;
         }

         info.parameters = module.getParameters().stream()
               .map(KoronaInfoCommandJob::toParameterInfo)
               .flatMap(Optional::stream)
               .toList();
      } catch (Exception e) {
         info.error = e.toString();
      }
      return info;
   }

   private static Optional<ParameterInfo> toParameterInfo(BaseParameter<?> parameter) {
      ParameterInfo info = new ParameterInfo(parameter.getPersistentName(), parameter.getDescription());
      info.unit = Strings.emptyToNull(parameter.getUnit().formalName());

      switch (parameter) {
         case ValueParameter<?> valueParameter -> {
            info.allowedValues = valueParameter.getAllowedStringValues();
            if (info.allowedValues == null) {
               info.allowedValuesDescription = valueParameter.getAllowedValuesDescription();
            }
            info.defaultValue = getParameterValue(valueParameter);
         }
         case DynamicListParameter<?> listParameter -> {
            info.defaultValue = listParameter.getStringValues();
         }
         case RangeParameter rangeParameter -> {
            FloatRange value = rangeParameter.getValue();
            info.defaultValue = ImmutableMap.of(
                  "min", value.min(),
                  "max", value.max()
            );
         }
         case MultiParameter<?> multiParameter -> info.parameters = multiParameter.getParameters().stream()
               .map(KoronaInfoCommandJob::toParameterInfo)
               .flatMap(Optional::stream)
               .toList();
         case VoidParameter __ -> {
            return Optional.empty();
         }
      }
      return Optional.of(info);
   }

   private static Object getParameterValue(ValueParameter<?> valueParameter) {
      Object value = valueParameter.getValue();
      Class<?> c = value.getClass();
      if (Primitives.isWrapperType(c)) {
         return value;
      }
      return valueParameter.getStringValue();
   }
}
