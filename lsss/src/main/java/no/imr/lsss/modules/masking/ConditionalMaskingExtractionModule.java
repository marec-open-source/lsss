package no.imr.lsss.modules.masking;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.ConditionalPingMask;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.FloatRangeSet;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

public final class ConditionalMaskingExtractionModule extends BaseDataModule {
   private NavigableMap<PingIndex, FloatRangeSet> mask = new ConcurrentSkipListMap<>();
   private final ChangeManager changeManager = new ChangeManager();

   public ConditionalMaskingExtractionModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::recompute), List.of(
            getInterpretationSettings().getPingRangeChangeManager(),
            getRegionManager().getConditionalPingMaskChangeManager()
      ));
      registry.add(getInterpretationSettings().getPingSampler().getNewPingsChangeManager(), newExecListener(this::processPingsAndNotify));

      //---

      recompute();
   }

   @Override
   protected void onDisable() {
      mask.clear();
   }

   private void recompute() {
      NavigableMap<PingIndex, FloatRangeSet> newMask = new ConcurrentSkipListMap<>();
      ConditionalPingMask conditionalPingMask = getRegionManager().getConditionalPingMask();
      if (!conditionalPingMask.isEmpty()) {
         addToMask(getInterpretationSettings().getPingSampler().getAvailablePings(), newMask, conditionalPingMask);
      }
      mask = newMask;
      changeManager.notifyListeners();
   }

   private static void addToMask(List<Ping> pings, NavigableMap<PingIndex, FloatRangeSet> mask, ConditionalPingMask conditionalPingMask) {
      for (Ping ping : pings) {
         mask.put(ping.getPingIndex(), conditionalPingMask.getMask(ping));
      }
   }

   private void processPingsAndNotify(List<Ping> pings) {
      ConditionalPingMask conditionalPingMask = getRegionManager().getConditionalPingMask();
      if (!conditionalPingMask.isEmpty()) {
         addToMask(pings, mask, conditionalPingMask);
         changeManager.notifyListeners();
      }
   }

   public FloatRangeSet pingIndexToClosestDepthRanges(PingIndex pingIndex) {
      Map.Entry<PingIndex, FloatRangeSet> entry = mask.floorEntry(pingIndex);
      return entry != null ? entry.getValue() : FloatRangeSet.of();
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }
}
