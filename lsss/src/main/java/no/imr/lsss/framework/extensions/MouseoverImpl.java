package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.Mouseover;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.util.Optional;

final class MouseoverImpl implements Mouseover {
   private final InterpretationSettings interpretationSettings;

   MouseoverImpl(LSSS lsss) {
      interpretationSettings = lsss.getInterpretationSettings();
   }

   @Override
   public ObservableValue<Boolean> frozen() {
      return interpretationSettings.mouseover().frozen();
   }

   @Override
   public ObservableValue<? extends Optional<GeoPoint>> geoLocation() {
      return interpretationSettings.mouseover().geoPos();
   }

   @Override
   public ObservableValue<? extends Optional<? extends PingIndex>> pingIndex() {
      return interpretationSettings.mouseover().pingIndex();
   }

   @Override
   public ObservableValue<Optional<Float>> depth() {
      return interpretationSettings.mouseover().depth();
   }

   @Override
   public ObservableValue<Optional<Float>> kHz() {
      return interpretationSettings.mouseover().kHz();
   }
}
