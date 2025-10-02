package no.imr.lsss.modules.korona.region;

import no.imr.korona.data.KoronaRegion;
import no.imr.korona.data.datagrams.Cas0Datagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.FloatRangeSet;
import org.jspecify.annotations.Nullable;

import java.util.NavigableMap;

public final class KoronaRegionLSSS {
   private final RegionInfoDatagram regionInfoDatagram;
   private final NavigableMap<PingIndex, FloatRangeSet> mask;
   private final PingRange pingRange;
   private final @Nullable Cas0Datagram cas0Datagram;
   private boolean ignored;

   public KoronaRegionLSSS(RegionInfoDatagram regionInfoDatagram, Ping ping, PingContainer pingContainer) {
      this.regionInfoDatagram = regionInfoDatagram;
      mask = KoronaRegion.createMask(regionInfoDatagram, pingContainer);
      pingRange = PingRange.from(mask, pingContainer);
      cas0Datagram = KoronaRegion.findCas0Datagram(ping, regionInfoDatagram);
   }

   public @Nullable Cas0Datagram getCas0Datagram() {
      return cas0Datagram;
   }

   public RegionInfoDatagram getRegionInfoDatagram() {
      return regionInfoDatagram;
   }

   public NavigableMap<PingIndex, FloatRangeSet> getMask() {
      return mask;
   }

   public PingRange getPingRange() {
      return pingRange;
   }

   public boolean isIgnored() {
      return ignored;
   }

   public void setIgnored(boolean ignored) {
      this.ignored = ignored;
   }
}
