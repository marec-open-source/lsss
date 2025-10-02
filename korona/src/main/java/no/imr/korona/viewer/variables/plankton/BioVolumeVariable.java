package no.imr.korona.viewer.variables.plankton;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.plankton.PlanktonInversionModule;
import no.imr.korona.computation.plankton.PlanktonScatterer;
import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.Cds0Datagram;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.imr.tools.range.FloatRange;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * BioVolume.
 */
public final class BioVolumeVariable extends ContinuousPlanktonVariable {
   private static final Unit UNIT = new Unit("mm^3/m^3");

   private Map<Pic0Datagram.PlanktonCategory, BackscatterModel> categoryToModel = Map.of();

   BioVolumeVariable() {
      super(new Name("bioVolume", "Bio volume"), new ContinuousVariableSettings(FloatRange.of(0, 100), FloatRange.of(1, 90), 0.1, true),
            UNIT, ExportTransform.round(100));
   }

   @Override
   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      super.updateEvaluationContext(configurationItems, configFileSettings);

      categoryToModel = makeCategoryToModel(configurationItems, configFileSettings);
   }

   private static Map<Pic0Datagram.PlanktonCategory, BackscatterModel> makeCategoryToModel(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      List<Cds0Datagram> cds0Datagrams = Utils.getAllOfType(configurationItems, Cds0Datagram.class).toList();
      if (cds0Datagrams.isEmpty()) {
         return Map.of();
      }
      ModuleContainer moduleContainer = new ModuleContainer(new Korona(), configFileSettings);
      for (Cds0Datagram cds0Datagram : cds0Datagrams) {
         moduleContainer.appendXml(cds0Datagram.getDocument().getRootElement());
      }
      PlanktonInversionModule planktonInversionModule = moduleContainer.getModule(PlanktonInversionModule.class);
      if (planktonInversionModule == null) {
         return Map.of();
      }
      return planktonInversionModule.getSelectedScatterers().stream()
            .collect(Collectors.toUnmodifiableMap(PlanktonScatterer::getPlanktonCategory, PlanktonScatterer::getBackscatterModel));
   }

   @Override
   public boolean isUsableInContext() {
      return super.isUsableInContext() && !categoryToModel.isEmpty();
   }

   @Override
   public ContinuousVariableResult evaluate(int channel, Ping ping) {
      Pic0Datagram pic0Datagram = getPic0Datagram();
      Pid0Datagram pid0Datagram = ping.getPingItem(Pid0Datagram.class);
      if (pic0Datagram == null || pid0Datagram == null) {
         return ContinuousVariableResult.EMPTY;
      }

      List<Pid0Datagram.PlanktonSample> planktonSamples = pid0Datagram.getPlanktonSamples(pic0Datagram);
      float[] floatData = new float[planktonSamples.size()];
      for (int i = 0; i < floatData.length; i++) {
         Pid0Datagram.PlanktonData planktonData = planktonSamples.get(i).getBestPlanktonData();
         Pic0Datagram.PlanktonCategory planktonCategory = planktonData.getPlanktonCategory();
         BackscatterModel backscatterModel = planktonCategory != null ? categoryToModel.get(planktonCategory) : null;
         float bioVolume = backscatterModel != null ? (float) backscatterModel.getBioVolume(planktonData.getLengthDistribution()) : 0;
         bioVolume *= 1e9f; // Convert from m^3/m^3 to mm^3/m^3
         floatData[i] = bioVolume;
      }
      return new ContinuousVariableResult(floatData, pid0Datagram.getDepthRange());
   }
}
