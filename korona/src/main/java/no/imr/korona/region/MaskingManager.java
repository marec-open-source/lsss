package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public final class MaskingManager {
   private static final String XML_MASKING = "masking";
   private static final String XML_CHANNEL_ID = "channelID";

   private final RegionManager regionManager;
   private final Map<Integer, Mask> channelToMaskMap = new ConcurrentHashMap<>();
   private final ArgChangeManager<PingRange> changeManager = new ArgChangeManager<>();

   MaskingManager(RegionManager regionManager) {
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
      channelToMaskMap.clear();
      notifyListeners(regionManager.getPingContainer().getTotalRange());
   }

   void reset(PingRange pingRange) {
      for (Mask mask : channelToMaskMap.values()) {
         mask.remove(pingRange);
      }
      notifyListeners(pingRange);
   }

   public ArgChangeManager<PingRange> getChangeManager() {
      return changeManager;
   }

   public Mask getMask(int channel) {
      return channelToMaskMap.computeIfAbsent(channel, _ -> new Mask());
   }

   public void mask(Map<PingIndex, FloatRangeSet> mask, Collection<Integer> channels) {
      if (mask.keySet().stream().anyMatch(regionManager::isReadOnly)) {
         throw new IllegalEditException();
      }
      channels.forEach(channel -> {
         Mask channelMask = getMask(channel);
         mask.forEach(channelMask::add);
      });
      PingRange pingRange = PingRange.from(mask.keySet(), regionManager.getPingContainer());
      notifyListeners(pingRange);
   }

   public void mask(Map<PingIndex, FloatRangeSet> mask, int channel) {
      mask(mask, List.of(channel));
   }

   public void maskAllChannels(Map<PingIndex, FloatRangeSet> mask) {
      mask(mask, allChannels());
   }

   public void unmask(Map<PingIndex, FloatRangeSet> mask, Collection<Integer> channels) {
      if (mask.keySet().stream().anyMatch(regionManager::isReadOnly)) {
         throw new IllegalEditException();
      }
      channels.forEach(channel -> {
         Mask channelMask = getMask(channel);
         mask.forEach(channelMask::remove);
      });
      PingRange pingRange = PingRange.from(mask.keySet(), regionManager.getPingContainer());
      notifyListeners(pingRange);
   }

   public void unmask(Map<PingIndex, FloatRangeSet> mask, int channel) {
      unmask(mask, List.of(channel));
   }

   public void unmaskAllChannels(Map<PingIndex, FloatRangeSet> mask) {
      unmask(mask, allChannels());
   }

   private List<Integer> allChannels() {
      return IntStream.rangeClosed(1, regionManager.getPingContainer().getTransducerCount())
            .boxed()
            .toList();
   }

   Element toXml(PingRange pingRange) {
      Element rootElement = DocumentHelper.createElement(XML_MASKING);

      Map<Integer, Mask> sortedMap = new TreeMap<>(channelToMaskMap);
      for (Map.Entry<Integer, Mask> entry : sortedMap.entrySet()) {
         Element channelMaskElement = entry.getValue().toXml(pingRange);
         if (channelMaskElement != null) {
            channelMaskElement.addAttribute(XML_CHANNEL_ID, Integer.toString(entry.getKey()));
            rootElement.add(channelMaskElement);
         }
      }

      return rootElement;
   }

   void fromXml(Element parentElement, PingIndex referencePingIndex) {
      Element rootElement = parentElement.element(XML_MASKING);
      if (rootElement == null) {
         return;
      }
      PingContainer pingContainer = regionManager.getPingContainer();
      for (Element channelMaskElement : rootElement.elements()) {
         int channelNumber = Integer.parseInt(channelMaskElement.attributeValue(XML_CHANNEL_ID));
         getMask(channelNumber).fromXml(channelMaskElement, referencePingIndex, pingContainer);
      }
   }

   void join(MaskingManager left, MaskingManager right) {
      int transducerCount = regionManager.getPingContainer().getTransducerCount();
      for (int channel = 1; channel <= transducerCount; channel++) {
         getMask(channel).join(left.getMask(channel), right.getMask(channel));
      }
   }
}
