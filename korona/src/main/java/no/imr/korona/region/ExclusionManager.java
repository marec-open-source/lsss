package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.range.CopyOnWriteRangeSet;
import no.imr.tools.range.RangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public final class ExclusionManager {
   private static final String XML_EXCLUSION_RANGES = "exclusionRanges";

   private final RegionManager regionManager;
   private final RangeSet<PingIndex> exclusions = new CopyOnWriteRangeSet<>();
   private final ArgChangeManager<PingRange> changeManager = new ArgChangeManager<>();
   private final ArgChangeManager<PingRange> manualExclusionChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<PingRange> manualInclusionChangeManager = new ArgChangeManager<>();

   ExclusionManager(RegionManager regionManager) {
      this.regionManager = regionManager;
   }

   void notifyListeners(PingRange pingRange) {
      if (regionManager.isSkipNotifications()) {
         return;
      }
      changeManager.notifyListeners(pingRange);
      regionManager.notifyAllRegionsInPingRange(pingRange);
   }

   void clear() {
      exclusions.clear();
      notifyListeners(regionManager.getPingContainer().getTotalRange());
   }

   void reset(PingRange pingRange) {
      exclusions.remove(pingRange);
      notifyListeners(pingRange);
   }

   public RangeSet<PingIndex> getExclusions() {
      return exclusions;
   }

   public ArgChangeManager<PingRange> getChangeManager() {
      return changeManager;
   }

   public ArgChangeManager<PingRange> getManualExclusionChangeManager() {
      return manualExclusionChangeManager;
   }

   public ArgChangeManager<PingRange> getManualInclusionChangeManager() {
      return manualInclusionChangeManager;
   }

   public boolean isExcluded(PingIndex pingIndex) {
      return exclusions.contains(pingIndex);
   }

   public boolean isPartiallyExcluded(PingRange pingRange) {
      return exclusions.containsAny(pingRange);
   }

   public void excludeRange(PingRange pingRange) {
      regionManager.writeablePingRanges(pingRange).stream()
            .forEach(exclusions::add);
      notifyListeners(pingRange);
      manualExclusionChangeManager.notifyListeners(pingRange);
   }

   public void exclude(RangeSet<PingIndex> pingRangeSet) {
      pingRangeSet.stream()
            .map(PingRange::of)
            .forEach(this::excludeRange);
   }

   public void includeRange(PingRange pingRange) {
      regionManager.writeablePingRanges(pingRange).stream()
            .forEach(exclusions::remove);
      notifyListeners(pingRange);
      manualInclusionChangeManager.notifyListeners(pingRange);
   }

   Element toXml(PingRange pingRange) {
      Element rootElement = DocumentHelper.createElement(XML_EXCLUSION_RANGES);
      exclusions.stream(pingRange).forEach(range -> {
         Element rangeElement = RegionManager.rangeToXml(range);
         rootElement.add(rangeElement);
      });
      return rootElement;
   }

   void fromXml(Element parentElement) throws WorkFileException {
      Element rootElement = parentElement.element(XML_EXCLUSION_RANGES);
      if (rootElement == null) {
         return;
      }
      PingContainer pingContainer = regionManager.getPingContainer();

      for (Element rangeElement : rootElement.elements()) {
         PingRange range = RegionManager.xmlToRange(rangeElement, pingContainer);
         exclusions.add(range);
      }
   }

   void join(ExclusionManager left, ExclusionManager right) {
      exclusions.addAll(left.exclusions);
      exclusions.addAll(right.exclusions);
   }
}
