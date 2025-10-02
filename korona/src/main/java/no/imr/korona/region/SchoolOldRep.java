package no.imr.korona.region;

import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.data.util.mask.MaskUtils;
import no.imr.tools.range.FloatRangeSet;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableMap;

final class SchoolOldRep {
   static final String XML_SCHOOL_REP = "schoolRep";
   private static final String XML_BOUNDARY_POINTS = "boundaryPoints";

   private SchoolOldRep() {
   }

   static NavigableMap<PingIndex, FloatRangeSet> createSchoolMaskFromXml(PingContainer pingContainer, PingIndex referencePingIndex, Element element) {
      String s = element.element(XML_BOUNDARY_POINTS).getText().trim();
      String[] tokens = s.split("\\s+");
      List<EchogramPoint> points = new ArrayList<>();
      for (int i = 0; i < tokens.length; i += 2) {
         long pingOffset = Long.parseLong(tokens[i]);
         PingIndex pingIndex = pingContainer.getPingIndex(pingOffset + referencePingIndex.getPingNumber());
         float depth = Float.parseFloat(tokens[i + 1]);
         EchogramPoint point = new EchogramPoint(pingIndex, depth);
         points.add(point);
      }
      return MaskUtils.incompleteBoundaryToMask(points, pingContainer, IdentityDepthTransform.INSTANCE);
   }
}
