package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.CopyOnWriteRangeMap;
import no.imr.tools.range.FloatRangeBuilder;
import no.imr.tools.range.RangeMap;
import no.imr.tools.range.RangeUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.NavigableSet;

public final class BubbleCorrectionManager {
   private static final String XML_BUBBLE_CORRECTION_RANGES = "bubbleCorrectionRanges";
   private static final String XML_BUBBLE_CORRECTION_VALUE = "bubbleCorrectionValue";

   private final RegionManager regionManager;
   private final RangeMap<PingIndex, Float> bubbleCorrectionMap = new CopyOnWriteRangeMap<>();
   private final ChangeManager changeManager = new ChangeManager();

   BubbleCorrectionManager(RegionManager regionManager) {
      this.regionManager = regionManager;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   void notifyListeners() {
      if (regionManager.isSkipNotifications()) {
         return;
      }
      changeManager.notifyListeners();
   }

   void clear() {
      bubbleCorrectionMap.clear();
      notifyListeners();
   }

   void reset(PingRange pingRange) {
      bubbleCorrectionMap.put(pingRange, 1f);
      notifyListeners();
   }

   public float getBubbleCorrection(PingIndex pingIndex) {
      return bubbleCorrectionMap.getOrDefault(pingIndex, 1f);
   }

   public float getBubbleCorrection(PingRange pingRange) {
      FloatRangeBuilder builder = new FloatRangeBuilder();
      bubbleCorrectionMap.stream(pingRange)
            .forEach(entry -> builder.expand(entry.value()));
      return builder.isInitialized()
            ? builder.toFloatRange().getCenter()
            : 1;
   }

   public float getWritableBubbleCorrection(PingRange pingRange) {
      FloatRangeBuilder builder = new FloatRangeBuilder();
      regionManager.writeablePingRanges(pingRange).stream()
            .flatMap(bubbleCorrectionMap::stream)
            .forEach(entry -> builder.expand(entry.value()));
      return builder.isInitialized()
            ? builder.toFloatRange().getCenter()
            : getBubbleCorrection(pingRange);
   }

   public NavigableSet<Float> getBubbleCorrections(PingRange pingRange) {
      return RangeUtils.getValueSet(bubbleCorrectionMap, pingRange);
   }

   public void setBubbleCorrection(PingRange pingRange, float value) {
      regionManager.writeablePingRanges(pingRange).stream()
            .forEach(range -> bubbleCorrectionMap.put(range, value));
      notifyListeners();
   }

   Element toXml(PingRange pingRange) {
      Element rootElement = DocumentHelper.createElement(XML_BUBBLE_CORRECTION_RANGES);
      bubbleCorrectionMap.stream(pingRange).forEach(entry -> {
         Element rangeElement = RegionManager.rangeToXml(entry.range())
               .addAttribute(XML_BUBBLE_CORRECTION_VALUE, Float.toString(entry.value()));
         rootElement.add(rangeElement);
      });
      return rootElement;
   }

   void fromXml(Element parentElement) throws WorkFileException {
      Element rootElement = parentElement.element(XML_BUBBLE_CORRECTION_RANGES);
      if (rootElement == null) {
         return;
      }
      PingContainer pingContainer = regionManager.getPingContainer();

      for (Element rangeElement : rootElement.elements()) {
         PingRange range = RegionManager.xmlToRange(rangeElement, pingContainer);
         float value = Float.parseFloat(rangeElement.attributeValue(XML_BUBBLE_CORRECTION_VALUE));
         bubbleCorrectionMap.put(range, value);
      }
   }

   void join(BubbleCorrectionManager left, BubbleCorrectionManager right) {
      bubbleCorrectionMap.putAll(left.bubbleCorrectionMap);
      bubbleCorrectionMap.putAll(right.bubbleCorrectionMap);
   }
}
