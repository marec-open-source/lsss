package no.imr.lsss.util;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.WorkFileException;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.CopyOnWriteRangeSet;
import no.imr.tools.range.RangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public final class PingExclusion {
   private static final String XML_EXCLUSION_RANGES = "exclusionRanges";
   private static final String XML_EXCLUSION_PINGS = "exclusionPings";

   private final DataManager dataManager;
   private final RangeSet<PingIndex> exclusionRanges = new CopyOnWriteRangeSet<>();
   private final RangeSet<PingIndex> excludedSinglePings = new CopyOnWriteRangeSet<>();
   private final ChangeManager changeManager = new ChangeManager();
   private boolean skipNotify;

   public PingExclusion(DataManager dataManager) {
      this.dataManager = dataManager;
   }

   public void setup(LSSS lsss, PingIndexConverter pingIndexConverter) {
      lsss.getRegionManager().getExclusionManager().getManualExclusionChangeManager().addListener(lsssPingRange -> {
         excludeRange(pingIndexConverter.lsssToClosestOther(lsssPingRange));
      });
      lsss.getRegionManager().getExclusionManager().getManualInclusionChangeManager().addListener(lsssPingRange -> {
         includeRange(pingIndexConverter.lsssToClosestOther(lsssPingRange));
      });
   }

   public void clear() {
      excludedSinglePings.clear();
      exclusionRanges.clear();
      notifyListeners();
   }

   private void notifyListeners() {
      if (skipNotify) {
         return;
      }
      changeManager.notifyListeners();
   }

   public void setSkipNotify(boolean skipNotify) {
      this.skipNotify = skipNotify;
      notifyListeners();
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public RangeSet<PingIndex> getExclusionRanges() {
      return exclusionRanges;
   }

   public RangeSet<PingIndex> getExcludedSinglePings() {
      return excludedSinglePings;
   }

   public boolean isExcluded(PingIndex pingIndex) {
      return excludedSinglePings.contains(pingIndex) || exclusionRanges.contains(pingIndex);
   }

   public void excludePing(PingIndex pingIndex) {
      excludePing(PingRange.ofSinglePing(pingIndex, dataManager.getDataFileSet()));
   }

   public void excludePing(PingRange pingRange) {
      excludedSinglePings.add(pingRange);
      notifyListeners();
   }

   public void includePing(PingIndex pingIndex) {
      includePing(PingRange.ofSinglePing(pingIndex, dataManager.getDataFileSet()));
   }

   public void includePing(PingRange pingRange) {
      excludedSinglePings.remove(pingRange);
      notifyListeners();
   }

   public void excludeRange(PingRange pingRange) {
      exclusionRanges.add(pingRange);
      notifyListeners();
   }

   public void includeRange(PingRange pingRange) {
      exclusionRanges.remove(pingRange);
      notifyListeners();
   }

   public void fromXml(Element element) throws WorkFileException {
      Element exclusionElement = element.element(XML_EXCLUSION_RANGES);
      if (exclusionElement != null) {
         for (Element rangeElement : exclusionElement.elements(RegionManager.XML_TIME_RANGE)) {
            excludeRange(RegionManager.xmlToRange(rangeElement, dataManager.getDataFileSet()));
         }
         Element pingExclusions = exclusionElement.element(XML_EXCLUSION_PINGS);
         if (pingExclusions != null) {
            for (Element rangeElement : pingExclusions.elements(RegionManager.XML_TIME_RANGE)) {
               excludePing(RegionManager.xmlToRange(rangeElement, dataManager.getDataFileSet()));
            }
         }
      }
   }

   public Element toXml(PingRange pingRange) {
      Element exclusions = DocumentHelper.createElement(XML_EXCLUSION_RANGES);

      exclusionRanges.stream(pingRange)
            .map(RegionManager::rangeToXml)
            .forEach(exclusions::add);

      Element pingExclusions = DocumentHelper.createElement(XML_EXCLUSION_PINGS);
      excludedSinglePings.stream(pingRange)
            .map(RegionManager::rangeToXml)
            .forEach(pingExclusions::add);
      if (!pingExclusions.elements().isEmpty()) {
         exclusions.add(pingExclusions);
      }

      return exclusions;
   }
}
