package no.imr.lsss.modules.schoolparameter;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.formats.synthetic.TestSyntheticData;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.region.School;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.schoolparameter.morphological.AreaParameterCollection;
import no.imr.lsss.test.LsssTestUtils;
import no.imr.tools.Utils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class SchoolParameterModuleTest {
   private LSSS lsss;
   private SchoolParameterModule schoolParameterModule;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(SchoolParameterModule.class), List.of());
      schoolParameterModule = lsss.getModuleManager().getModule(SchoolParameterModule.class);
      LsssTestUtils.open(lsss, new TestSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle());
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void test() {
      DataFileSet dataFileSet = lsss.getDataManager().getDataFileSet();
      PingRange totalRange = dataFileSet.getTotalRange();
      long firstPingNumber = totalRange.begin().getPingNumber();
      long lastPingNumber = totalRange.end().getPingNumber();
      float deltaDepth = 20;
      EchogramPoint p1 = new EchogramPoint(dataFileSet.getPingIndex(firstPingNumber + (lastPingNumber - firstPingNumber) / 4), 20);
      EchogramPoint p2 = new EchogramPoint(dataFileSet.getPingIndex(firstPingNumber + 2 * (lastPingNumber - firstPingNumber) / 4), 20 + deltaDepth);

      School school = lsss.getRegionManager().addSchool(p1, p2, IdentityDepthTransform.INSTANCE);
      assertNotNull(school);
      assertEquals(vesselDistanceInMeter(p1, p2) * deltaDepth, getArea(school), 0.1);

      ImmutableMap<String, Float> values = school.getParameters().values();
      assertEquals(10, values.size());
      assertEquals(46300.0f, values.get("area"));
      assertEquals(75.0f, values.get("bottomDepth"));
      assertEquals(4670.0f, values.get("circumference"));
      assertEquals(18.473217f, values.get("correctedHeight"));
      assertEquals(2310.6829f, values.get("correctedLength"));
      assertEquals(20.0f, values.get("height"));
      assertEquals(2315.0f, values.get("length"));
      assertEquals(40.0f, values.get("maxDepth"));
      assertEquals(20.0f, values.get("minDepth"));
      assertEquals(35.0f, values.get("minDistBottom"));

      ImmutableMap<String, Float> perChannelValues = school.getParameters().perChannelValues().get(1);
      assertNotNull(perChannelValues);
      assertEquals(12, perChannelValues.size());
      assertEquals(9190134.0f, perChannelValues.get("sA"));
      assertEquals(26250.0f, perChannelValues.get("sampleCount"));
      assertEquals(-0.9971263f, perChannelValues.get("svKurtosis"));
      assertEquals(768997.3f, perChannelValues.get("svMax"));
      assertEquals(458611.53f, perChannelValues.get("svMean"));
      assertEquals(457951.7f, perChannelValues.get("svMeanTruncated05"));
      assertEquals(456555.3f, perChannelValues.get("svMeanTruncated10"));
      assertEquals(455728.84f, perChannelValues.get("svMeanTruncated25"));
      assertEquals(458133.8f, perChannelValues.get("svMedian"));
      assertEquals(149615.22f, perChannelValues.get("svMin"));
      assertEquals(0.077493645f, perChannelValues.get("svSkewness"));
      assertEquals(2.3423609E10f, perChannelValues.get("svVariance"));

      // Split school
      EchogramPoint centerPoint = school.getCenterPoint();
      assertNotNull(centerPoint);
      lsss.getRegionManager().addVerticalDivider(centerPoint.pingIndex());

      School firstSchool = lsss.getRegionManager().getSchoolManager().getRegion(p1);
      assertNotNull(firstSchool);
      assertEquals(vesselDistanceInMeter(p1, centerPoint) * deltaDepth, getArea(firstSchool), 0.1);

      School secondSchool = lsss.getRegionManager().getSchoolManager().getRegion(centerPoint);
      assertNotNull(secondSchool);
      assertEquals(vesselDistanceInMeter(centerPoint, p2) * deltaDepth, getArea(secondSchool), 0.1);
   }

   private static double vesselDistanceInMeter(EchogramPoint from, EchogramPoint to) {
      return Utils.nmiToMeter(PingRange.ofUnsorted(from.pingIndex(), to.pingIndex()).getVesselDistance());
   }

   private double getArea(School school) {
      schoolParameterModule.compute(school);
      Float area = school.getParameters().values().get(AreaParameterCollection.AREA.getPersistentName());
      assertNotNull(area);
      return area;
   }
}
