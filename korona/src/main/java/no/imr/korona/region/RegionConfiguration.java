package no.imr.korona.region;

import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.misc.ToFloatFunction;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.RangeSet;

import java.util.concurrent.Executor;

/**
 * The dependency of {@link RegionManager} to configuration set elsewhere.
 */
public interface RegionConfiguration {
   PingContainer getPingContainer();

   DataConfiguration getDataConfiguration();

   PingRange getVisiblePingRange();

   ToFloatFunction<PingIndex> initialUpperDepth();

   ToFloatFunction<PingIndex> initialLowerDepth();

   FloatRange getDefaultThresholds();

   int nextObjectNumber();

   Executor getBackgroundExecutor();

   default void setReadOnlyPingsEnabled(boolean readOnlyPingsEnabled) {
   }

   RangeSet<PingIndex> getReadOnlyPings();
}
