package no.imr.korona.region.storing;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.WorkFileException;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.range.CopyOnWriteRangeMap;
import no.imr.tools.range.RangeMap;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public final class StoringConfigManager {
   private static final String XML_STORING = "databaseStoring";

   private final RegionManager regionManager;
   private final RangeMap<PingIndex, StoringIntervalConfig> configMap = new CopyOnWriteRangeMap<>();
   private final ChangeManager changeManager = new ChangeManager();

   public StoringConfigManager(RegionManager regionManager) {
      this.regionManager = regionManager;
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public void notifyListeners() {
      if (regionManager.isSkipNotifications()) {
         return;
      }
      changeManager.notifyListeners();
   }

   public void clear() {
      configMap.clear();
      notifyListeners();
   }

   public @Nullable StoringIntervalConfig get(PingIndex pingIndex) {
      return configMap.get(pingIndex);
   }

   public void put(PingRange pingRange, StoringIntervalConfig intervalConfig) {
      configMap.put(pingRange, intervalConfig);
      notifyListeners();
   }

   public void remove(PingRange pingRange) {
      configMap.remove(pingRange);
      notifyListeners();
   }

   public RangeMap<PingIndex, StoringIntervalConfig> getConfigMap() {
      return configMap;
   }

   public Element toXml(PingRange pingRange) {
      Element rootElement = DocumentHelper.createElement(XML_STORING);
      configMap.stream(pingRange).forEach(entry -> {
         Element rangeElement = RegionManager.rangeToXml(entry.range());
         entry.value().toXml(rangeElement);
         rootElement.add(rangeElement);
      });
      return rootElement;
   }

   public void fromXml(Element parentElement) throws WorkFileException, XmlParseException {
      Element rootElement = parentElement.element(XML_STORING);
      if (rootElement == null) {
         return;
      }
      PingContainer pingContainer = regionManager.getPingContainer();

      for (Element rangeElement : rootElement.elements()) {
         PingRange pingRange = RegionManager.xmlToRange(rangeElement, pingContainer);
         StoringIntervalConfig intervalConfig = StoringIntervalConfig.fromXml(rangeElement);
         configMap.put(pingRange, intervalConfig);
      }
   }

   public void join(StoringConfigManager left, StoringConfigManager right) {
      configMap.putAll(left.configMap);
      configMap.putAll(right.configMap);
   }
}
