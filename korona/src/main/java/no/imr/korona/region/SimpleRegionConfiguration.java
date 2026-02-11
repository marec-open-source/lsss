package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeSet;
import no.imr.tools.range.RangeUtils;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public final class SimpleRegionConfiguration implements RegionConfiguration {
   private final Supplier<PingContainer> pingContainer;
   private final DataConfiguration dataConfiguration;
   private IntSupplier nextObjectNumberSupplier = new AtomicInteger()::incrementAndGet;

   public SimpleRegionConfiguration(Supplier<PingContainer> pingContainer, DataConfiguration dataConfiguration) {
      this.pingContainer = pingContainer;
      this.dataConfiguration = dataConfiguration;
   }

   public SimpleRegionConfiguration(DataManager dataManager) {
      this(dataManager::getDataFileSet, dataManager.getDataConfiguration());
   }

   public SimpleRegionConfiguration setNextObjectNumberSupplier(IntSupplier nextObjectNumberSupplier) {
      this.nextObjectNumberSupplier = nextObjectNumberSupplier;
      return this;
   }

   @Override
   public PingContainer getPingContainer() {
      return pingContainer.get();
   }

   @Override
   public DataConfiguration getDataConfiguration() {
      return dataConfiguration;
   }

   @Override
   public PingRange getVisiblePingRange() {
      return getPingContainer().getTotalRange();
   }

   @Override
   public ToFloatFunction<PingIndex> initialUpperDepth() {
      return _ -> 0;
   }

   @Override
   public ToFloatFunction<PingIndex> initialLowerDepth() {
      return _ -> 1000;
   }

   @Override
   public FloatRange getDefaultThresholds() {
      return FloatRange.of(-82, -30);
   }

   @Override
   public int nextObjectNumber() {
      return nextObjectNumberSupplier.getAsInt();
   }

   @Override
   public Executor getBackgroundExecutor() {
      return Exec.FORK_JOIN_POOL;
   }

   @Override
   public RangeSet<PingIndex> getReadOnlyPings() {
      return RangeUtils.emptyRangeSet();
   }
}
