package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public final class Mask {
   public static final FloatRange ENTIRE_PING_DEPTH_RANGE = FloatRange.of(-1e6f, 1e6f);

   private static final String XML_MASK = "mask";
   private static final String XML_PING = "ping";
   private static final String XML_PING_OFFSET = "pingOffset";

   private final Map<PingIndex, FloatRangeSet> pingToDepthRanges = new ConcurrentHashMap<>();

   Mask() {
   }

   public boolean isEmpty() {
      return pingToDepthRanges.isEmpty();
   }

   public FloatRangeSet get(PingIndex pingIndex) {
      FloatRangeSet depthSet = pingToDepthRanges.get(pingIndex);
      return depthSet != null ? depthSet : FloatRangeSet.of();
   }

   void add(PingIndex pingIndex, FloatRangeSet depthRangeSet) {
      if (depthRangeSet.isEmpty()) {
         return;
      }
      pingToDepthRanges.merge(pingIndex, depthRangeSet, FloatRangeSet::add);
   }

   void remove(PingIndex pingIndex, FloatRangeSet depthRangeSet) {
      pingToDepthRanges.computeIfPresent(pingIndex, (key, currentDepthRangeSet) -> {
         FloatRangeSet result = currentDepthRangeSet.subtract(depthRangeSet);
         return result.isEmpty() ? null : result;
      });
   }

   void remove(PingRange pingRange) {
      pingToDepthRanges.keySet().removeIf(pingRange::contains);
   }

   @Nullable Element toXml(PingRange pingRange) {
      NavigableMap<PingIndex, FloatRangeSet> sortedMap = new TreeMap<>(pingToDepthRanges);
      NavigableMap<PingIndex, FloatRangeSet> subMap = sortedMap.subMap(pingRange.begin(), true, pingRange.end(), false);
      if (subMap.isEmpty()) {
         return null;
      }

      long startPingNumber = pingRange.begin().getPingNumber();
      Element mask = DocumentHelper.createElement(XML_MASK);

      StringBuilder sb = new StringBuilder();
      for (Map.Entry<PingIndex, FloatRangeSet> setEntry : subMap.entrySet()) {
         sb.setLength(0);
         float lastDepth = 0;
         List<FloatRange> depthRanges = setEntry.getValue().getFloatRanges();
         for (int i = 0; i < depthRanges.size(); i++) {
            FloatRange depthRange = depthRanges.get(i);
            float min = depthRange.min();
            float max = depthRange.max();
            if (i > 0) {
               sb.append(' ');
            }
            sb.append(min - lastDepth)
                  .append(' ')
                  .append(max - min);
            lastDepth = max;
         }
         mask.addElement(XML_PING)
               .addAttribute(XML_PING_OFFSET, Long.toString(setEntry.getKey().getPingNumber() - startPingNumber))
               .addText(sb.toString());
      }

      return mask;
   }

   void fromXml(Element element, PingIndex referenceIndex, PingContainer pingContainer) {
      for (Element pingElement : element.elements(XML_PING)) {
         int offset = Integer.parseInt(pingElement.attributeValue(XML_PING_OFFSET));
         PingIndex pingIndex = pingContainer.getClosestPingIndex(referenceIndex, offset, PingMapping.NUMBER);
         String s = pingElement.getText();
         String[] tokens = s.split("\\s");
         float lastDepth = 0;
         List<FloatRange> depthRanges = new ArrayList<>(tokens.length / 2);
         for (int i = 0; i < tokens.length; i += 2) {
            float d1 = Float.parseFloat(tokens[i]) + lastDepth;
            float d2 = Float.parseFloat(tokens[i + 1]) + d1;
            lastDepth = d2;
            FloatRange depthRange = FloatRange.of(d1, d2);
            if (depthRange.equals(ENTIRE_PING_DEPTH_RANGE)) {
               depthRange = ENTIRE_PING_DEPTH_RANGE;
            }
            depthRanges.add(depthRange);
         }
         add(pingIndex, FloatRangeSet.of(depthRanges));
      }
   }

   void join(Mask left, Mask right) {
      pingToDepthRanges.putAll(left.pingToDepthRanges);
      pingToDepthRanges.putAll(right.pingToDepthRanges);
   }
}
