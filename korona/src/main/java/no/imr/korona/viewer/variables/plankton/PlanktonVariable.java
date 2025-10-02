package no.imr.korona.viewer.variables.plankton;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.viewer.variables.DiscreteVariable;
import no.imr.korona.viewer.variables.DiscreteVariableResult;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Plankton.
 */
public final class PlanktonVariable extends DiscreteVariable {
   private @Nullable Pic0Datagram pic0Datagram;

   public PlanktonVariable() {
      super(PlanktonVariableFactory.PLANKTON_VARIABLE_GROUP, new Name("Plankton", "Plankton"));
   }

   @Override
   public boolean isUsableInContext() {
      return pic0Datagram != null;
   }

   @Override
   public void updateEvaluationContext(List<PingItem> configurationItems, ConfigFileSettings configFileSettings) {
      pic0Datagram = Utils.getFirstOrNull(configurationItems, Pic0Datagram.class);
      getSettings().update(pic0Datagram != null ? pic0Datagram.getPlanktonCategories() : List.of());
   }

   @Override
   public DiscreteCategory getUnknownCategory() {
      return Pic0Datagram.getFillerCategory();
   }

   @Override
   public DiscreteVariableResult evaluate(Ping ping) {
      Pic0Datagram pic0Datagram = this.pic0Datagram;
      Pid0Datagram pid0Datagram = ping.getPingItem(Pid0Datagram.class);
      if (pic0Datagram == null || pid0Datagram == null) {
         return DiscreteVariableResult.EMPTY;
      }

      List<Pid0Datagram.PlanktonSample> planktonSamples = pid0Datagram.getPlanktonSamples(pic0Datagram);
      byte[] byteData = new byte[planktonSamples.size()];
      for (int i = 0; i < planktonSamples.size(); i++) {
         Pic0Datagram.PlanktonCategory planktonCategory = planktonSamples.get(i).getBestPlanktonData().getPlanktonCategory();
         if (planktonCategory != null && getSettings().isPlottable(planktonCategory)) {
            byteData[i] = planktonCategory.getNumber();
         } else {
            byteData[i] = Pic0Datagram.getFillerCategory().getNumber();
         }
      }

      return new DiscreteVariableResult(byteData, pid0Datagram.getDepthRange());
   }
}
