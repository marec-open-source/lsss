package no.imr.lsss.util;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.region.WorkFileException;
import no.imr.korona.test.data.ConstantSyntheticData;
import no.imr.lsss.LSSS;
import no.imr.lsss.test.LsssTestUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class PingExclusionTest {
   private LSSS lsss;
   private DataManager dataManager;

   @BeforeEach
   void beforeEach() {
      lsss = LsssTestUtils.start(List.of(), List.of());
      dataManager = lsss.getDataManager();

      LsssTestUtils.open(lsss.getDataSetManager(),
            new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1000).toSegmentHandle(),
            new ConstantSyntheticData().withFirstAndLastPingNumber(1001, 1500).toSegmentHandle()
      );
   }

   @AfterEach
   void afterEach() {
      lsss.close();
   }

   @Test
   void xml() throws WorkFileException {
      PingExclusion pingExclusion = new PingExclusion(dataManager);

      PingIndex pingIndex1 = dataManager.getDataFileSet().getPingIndex(1);
      PingIndex pingIndex2 = dataManager.getDataFileSet().getPingIndex(100);
      PingIndex pingIndex3 = dataManager.getDataFileSet().getPingIndex(1001);
      pingExclusion.excludePing(pingIndex1);
      pingExclusion.excludePing(pingIndex2);
      pingExclusion.excludePing(pingIndex3);

      //Check that only the first two excluded pings are stored to the work file corresponding to the first datafile
      DataFile dataFile0 = dataManager.getDataFileSet().getDataFiles().getFirst();
      Element element0 = pingExclusion.toXml(dataFile0.getPingRange());
      assertEquals(1, element0.elements().size());
      Element e0 = element0.elements().getFirst();
      assertEquals(2, e0.elements().size());

      //Check that the last excluded ping is stored to the work file corresponding to the seconf datafile
      DataFile dataFile1 = dataManager.getDataFileSet().getDataFiles().get(1);
      Element element1 = pingExclusion.toXml(dataFile1.getPingRange());
      assertEquals(1, element1.elements().size());
      Element e1 = element1.elements().getFirst();
      assertEquals(1, e1.elements().size());

      pingExclusion.includePing(pingIndex1);
      pingExclusion.includePing(pingIndex2);
      pingExclusion.includePing(pingIndex3);
      assertFalse(pingExclusion.isExcluded(pingIndex1));
      assertFalse(pingExclusion.isExcluded(pingIndex2));
      assertFalse(pingExclusion.isExcluded(pingIndex3));

      Element rootElement0 = DocumentHelper.createElement("root0");
      rootElement0.add(element0);
      pingExclusion.fromXml(rootElement0);
      assertTrue(pingExclusion.isExcluded(pingIndex1));
      assertTrue(pingExclusion.isExcluded(pingIndex2));
      Element rootElement1 = DocumentHelper.createElement("root1");
      rootElement1.add(element1);
      pingExclusion.fromXml(rootElement1);
      assertTrue(pingExclusion.isExcluded(pingIndex3));
   }
}
