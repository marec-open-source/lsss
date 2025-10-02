package no.imr.lsss.framework.extensions;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.listening.ListenableProperty;
import no.marec.lsss.api.data.Ping;
import no.marec.lsss.api.data.PingIndex;
import no.marec.lsss.api.data.PingRange;
import no.marec.lsss.api.echogram.EchogramData;
import no.marec.lsss.api.util.observing.Observable;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.util.List;

final class EchogramDataImpl implements EchogramData {
   private final LSSS lsss;
   private final ListenableProperty<Integer> channel = new ListenableProperty<>(1);
   private final ListenableProperty<PingRange> pingRange = new ListenableProperty<>(no.imr.korona.data.ping.PingRange.EMPTY_RANGE);

   EchogramDataImpl(LSSS lsss) {
      this.lsss = lsss;
   }

   void setup() {
      InterpretationSettings interpretationSettings = lsss.getInterpretationSettings();
      interpretationSettings.getChannelChangeManager().addListener(channel::setValue);
      interpretationSettings.getPingRangeChangeManager().addListener(pingRange::setValue);
   }

   @Override
   public ObservableValue<Integer> channel() {
      return channel;
   }

   @Override
   public ObservableValue<? extends PingRange> pingRange() {
      return pingRange;
   }

   @Override
   public List<? extends PingIndex> subsampledPingIndices() {
      return lsss.getInterpretationSettings().getPingSampler().getRequestedPingIndices();
   }

   @Override
   public List<? extends Ping> currentlyLoadedPings() {
      return lsss.getInterpretationSettings().getPingSampler().getAvailablePings();
   }

   @Override
   public Observable<? extends List<? extends Ping>> newlyLoadedPings() {
      return lsss.getInterpretationSettings().getPingSampler().getNewPingsChangeManager();
   }
}
