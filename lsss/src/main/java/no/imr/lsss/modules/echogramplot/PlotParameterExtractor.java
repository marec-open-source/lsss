package no.imr.lsss.modules.echogramplot;

import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterConfigSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.plot.pojo.PlotParameterConfig;
import no.imr.lsss.framework.config.survey.data.DataSetManager;
import no.imr.lsss.modules.echogramplot.functions.PingFunction;
import no.imr.tools.Utils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PlotParameterExtractor {
   private Map<String, PingFunction> pingFunctions = Map.of();

   public PlotParameterExtractor() {
   }

   public void update(EchogramPlotModule echogramPlotModule, DataSetManager dataSetManager, Function<PlotParameterConfig, PingFunction> configToFunction) {
      Map<String, PingFunction> newPingFunctions = getPlotParameterConfigs(dataSetManager)
            .map(config -> {
               PingFunction pingFunction = pingFunctions.get(config.id);
               PingFunction newPingFunction = configToFunction.apply(config);
               if (pingFunction != null) {
                  newPingFunction.selected.setValue(pingFunction.selected.getValue());
               }
               return newPingFunction;
            })
            .collect(Collectors.toMap(PingFunction::getPersistentName, Function.identity()));

      pingFunctions.values().forEach(echogramPlotModule::remove);
      newPingFunctions.values().stream()
            .sorted(Utils.comparingIgnoringCase(f -> f.getName().displayName()))
            .forEach(echogramPlotModule::add);
      pingFunctions = newPingFunctions;
   }

   private static Stream<PlotParameterConfig> getPlotParameterConfigs(DataSetManager dataSetManager) {
      return Stream.of(dataSetManager.getDataManager(), dataSetManager.getOtherDataManager())
            .flatMap(dataManager -> dataManager.getDataFileSet().getDataFiles().stream())
            .flatMap(dataFile -> dataFile.getPingConfiguration().getConfigurationItems(PlotParameterConfigSubDatagram.class))
            .flatMap(configSubDatagram -> configSubDatagram.getPlotParameterConfigs().stream())
            .filter(Utils.distinctBy(plotParameterConfig -> plotParameterConfig.id));
   }
}
