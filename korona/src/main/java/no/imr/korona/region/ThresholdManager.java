package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.CopyOnWriteRangeMap;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.Range;
import no.imr.tools.range.RangeMap;
import no.imr.tools.range.RangeUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.NavigableSet;

public final class ThresholdManager {
   public static final String XML_THRESHOLDING = "thresholding";
   private static final String XML_UPPER_THRESHOLD = "upperThreshold";
   private static final String XML_LOWER_THRESHOLD = "lowerThreshold";
   private static final String XML_UPPER_THRESHOLD_ACTIVE = "upperThresholdActive";
   private static final String XML_VALUE = "value";

   private final RegionManager regionManager;
   private final RangeMap<PingIndex, Boolean> upperThresholdActive = new CopyOnWriteRangeMap<>();
   private final RangeMap<PingIndex, Float> logSvMin = new CopyOnWriteRangeMap<>();
   private final RangeMap<PingIndex, Float> logSvMax = new CopyOnWriteRangeMap<>();
   private final ChangeManager changeManager = new ChangeManager();

   ThresholdManager(RegionManager regionManager) {
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
      upperThresholdActive.clear();
      logSvMin.clear();
      logSvMax.clear();
      notifyListeners();
   }

   void reset(PingRange pingRange) {
      upperThresholdActive.put(pingRange, false);
      logSvMin.put(pingRange, regionManager.getRegionConfiguration().getDefaultThresholds().min());
      logSvMax.put(pingRange, regionManager.getRegionConfiguration().getDefaultThresholds().max());
      notifyListeners();
   }

   public void set(PingRange pingRange, @Nullable Boolean upperActive, @Nullable Float minLogSv, @Nullable Float maxLogSv) {
      regionManager.writeablePingRanges(pingRange).forEach(range -> {
         if (upperActive != null) {
            setUpperThresholdActive(range, upperActive);
         }
         if (minLogSv != null) {
            setLogSvMin(range, minLogSv);
         }
         if (maxLogSv != null) {
            setLogSvMax(range, maxLogSv);
         }
      });
      notifyListeners();
   }

   private void setUpperThresholdActive(Range<PingIndex> pingRange, boolean active) {
      upperThresholdActive.put(pingRange, active);
   }

   private void setLogSvMin(Range<PingIndex> pingRange, float min) {
      logSvMin.put(pingRange, min);
      RangeUtils.replaceValues(logSvMax, pingRange, min, value -> value < min);
   }

   private void setLogSvMax(Range<PingIndex> pingRange, float max) {
      logSvMax.put(pingRange, max);
      RangeUtils.replaceValues(logSvMin, pingRange, max, value -> value > max);
   }

   public FloatRange getLinearSvRange(PingIndex pingIndex) {
      FloatRange logSvRange = getLogSvRange(pingIndex);
      float min = PowerData.logSvToSv(logSvRange.min());
      float max = PowerData.logSvToSv(logSvRange.max());
      return FloatRange.of(min, max);
   }

   public FloatRange getLogSvRange(PingIndex pingIndex) {
      Float nullableMin = logSvMin.get(pingIndex);
      float min = nullableMin != null ? nullableMin : regionManager.getRegionConfiguration().getDefaultThresholds().min();

      float max;
      if (isUpperThresholdActive(pingIndex)) {
         Float nullableMax = logSvMax.get(pingIndex);
         max = nullableMax != null ? nullableMax : regionManager.getRegionConfiguration().getDefaultThresholds().max();
      } else {
         max = Float.POSITIVE_INFINITY;
      }

      return FloatRange.of(min, max);
   }

   public boolean isUpperThresholdActive(PingIndex pingIndex) {
      Boolean nullableActive = upperThresholdActive.get(pingIndex);
      return nullableActive != null ? nullableActive : false;
   }

   public float getMinLowerThreshold(PingRange pingRange) {
      return (float) logSvMin.stream(pingRange)
            .mapToDouble(RangeMap.Entry::value)
            .min()
            .orElse(Double.NEGATIVE_INFINITY);
   }

   public float getMaxUpperThreshold(PingRange pingRange) {
      return (float) logSvMax.stream(pingRange)
            .mapToDouble(RangeMap.Entry::value)
            .max()
            .orElse(Double.POSITIVE_INFINITY);
   }

   public float getMinWritableLowerThreshold(PingRange pingRange) {
      return (float) regionManager.writeablePingRanges(pingRange).stream()
            .flatMap(logSvMin::stream)
            .mapToDouble(RangeMap.Entry::value)
            .min()
            .orElseGet(() -> getMinLowerThreshold(pingRange));
   }

   public float getMaxInteractiveUpperThreshold(PingRange pingRange) {
      return (float) regionManager.writeablePingRanges(pingRange).stream()
            .flatMap(logSvMax::stream)
            .mapToDouble(RangeMap.Entry::value)
            .max()
            .orElseGet(() -> getMaxUpperThreshold(pingRange));
   }

   public NavigableSet<Float> getLowerThresholds(PingRange pingRange) {
      return RangeUtils.getValueSet(logSvMin, pingRange);
   }

   public @Nullable Float getLowerThresholdIfSingleValue(PingRange pingRange) {
      return RangeUtils.getCompletelyMappedSingleValue(logSvMin, pingRange);
   }

   public NavigableSet<Float> getUpperThresholds(PingRange pingRange) {
      return RangeUtils.getValueSet(logSvMax, pingRange);
   }

   public NavigableSet<Boolean> getUpperThresholdActive(PingRange pingRange) {
      return RangeUtils.getValueSet(upperThresholdActive, pingRange);
   }

   Element toXml(PingRange pingRange) {
      Element rootElement = DocumentHelper.createElement(XML_THRESHOLDING);

      Element upperThresholdActiveElement = rootElement.addElement(XML_UPPER_THRESHOLD_ACTIVE);
      upperThresholdActive.stream(pingRange).forEach(entry -> {
         Element rangeElement = RegionManager.rangeToXml(entry.range())
               .addAttribute(XML_VALUE, Boolean.toString(entry.value()));
         upperThresholdActiveElement.add(rangeElement);
      });

      Element upperThresholdElement = rootElement.addElement(XML_UPPER_THRESHOLD);
      logSvMax.stream(pingRange).forEach(entry -> {
         Element rangeElement = RegionManager.rangeToXml(entry.range())
               .addAttribute(XML_VALUE, Float.toString(entry.value()));
         upperThresholdElement.add(rangeElement);
      });

      Element lowerThresholdElement = rootElement.addElement(XML_LOWER_THRESHOLD);
      logSvMin.stream(pingRange).forEach(entry -> {
         Element rangeElement = RegionManager.rangeToXml(entry.range())
               .addAttribute(XML_VALUE, Float.toString(entry.value()));
         lowerThresholdElement.add(rangeElement);
      });

      return rootElement;
   }

   void fromXml(Element parentElement) throws WorkFileException {
      Element rootElement = parentElement.element(XML_THRESHOLDING);
      if (rootElement == null) {
         return;
      }
      PingContainer pingContainer = regionManager.getPingContainer();

      for (Element rangeElement : rootElement.element(XML_UPPER_THRESHOLD_ACTIVE).elements(RegionManager.XML_TIME_RANGE)) {
         PingRange range = RegionManager.xmlToRange(rangeElement, pingContainer);
         boolean value = Boolean.parseBoolean(rangeElement.attributeValue(XML_VALUE));
         upperThresholdActive.put(range, value);
      }

      for (Element rangeElement : rootElement.element(XML_UPPER_THRESHOLD).elements(RegionManager.XML_TIME_RANGE)) {
         PingRange range = RegionManager.xmlToRange(rangeElement, pingContainer);
         float value = Float.parseFloat(rangeElement.attributeValue(XML_VALUE));
         logSvMax.put(range, value);
      }

      for (Element rangeElement : rootElement.element(XML_LOWER_THRESHOLD).elements(RegionManager.XML_TIME_RANGE)) {
         PingRange range = RegionManager.xmlToRange(rangeElement, pingContainer);
         float value = Float.parseFloat(rangeElement.attributeValue(XML_VALUE));
         logSvMin.put(range, value);
      }
   }

   void join(ThresholdManager left, ThresholdManager right) {
      upperThresholdActive.putAll(left.upperThresholdActive);
      upperThresholdActive.putAll(right.upperThresholdActive);

      logSvMax.putAll(left.logSvMax);
      logSvMax.putAll(right.logSvMax);

      logSvMin.putAll(left.logSvMin);
      logSvMin.putAll(right.logSvMin);
   }
}
