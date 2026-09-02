package no.imr.korona.computation.region;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.RegionBorderDatagram;
import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datagrams.RegionTableOfContentsDatagram;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.Utils;
import no.imr.tools.test.JUnitUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

final class SchoolDetectionModuleTest {
   @Test
   void rectangularSchool() throws IOException {
      rectangularSchool(5);
      rectangularSchool(0);
   }

   private static void rectangularSchool(int pingMargin) throws IOException {
      SyntheticSchoolData syntheticData = new SyntheticSchoolData(SyntheticSchoolData.CASE_RECTANGULAR_SCHOOL);
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(
            SyntheticSchoolData.RECTANGULAR_SCHOOL_START_PING - pingMargin,
            SyntheticSchoolData.RECTANGULAR_SCHOOL_START_PING + SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH - 1 + pingMargin
      );
      List<RegionInfoDatagram> infos = run(syntheticDataFile, schoolDetectionModule -> {
         schoolDetectionModule.boundarySmoothingIterations.setIntValue(1);
      });
      assertEquals(1, infos.size());
      RegionInfoDatagram info = infos.getFirst();
      assertEquals(1, info.getBorderIds().length);

      RegionInfoDatagram.BoundingBox boundingBox = info.getBoundingBox();
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_START_PING,
            syntheticDataFile.getFirstPingNumber() + boundingBox.x - 1);
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_MIN_DEPTH, boundingBox.y, 1e-7);
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH, boundingBox.width);
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_HEIGHT, boundingBox.height);

      double sampleCount = SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH * SyntheticSchoolData.RECTANGULAR_SCHOOL_HEIGHT / SyntheticSchoolData.SAMPLE_DISTANCE;
      double sampleArea = SyntheticSchoolData.SAMPLE_DISTANCE * SyntheticSchoolData.METER_PER_PING;
      double length = SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH * SyntheticSchoolData.METER_PER_PING;
      double height = SyntheticSchoolData.RECTANGULAR_SCHOOL_HEIGHT;

      RegionInfoDatagram.Values values = info.getValues();
      assertEqualsRelative(sampleCount * sampleArea, values.area, 1e-6);
      assertEqualsRelative(sampleCount, values.sampleCount);
      assertEqualsRelative(sampleCount * PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.sumLogSv);
      assertEqualsRelative(sampleCount * SyntheticSchoolData.SV_INSIDE, values.sumSv, 1e-7);
      assertEqualsRelative(PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.logMeanSv);
      assertEqualsRelative(SyntheticSchoolData.SV_INSIDE * height, values.sa, 1e-7);
      assertEqualsRelative(2 * length + 2 * height, values.perimeter);
      assertEqualsRelative(length, values.length);
      assertEqualsRelative(height, values.maxHeight, 1e-7);
   }

   @Test
   void rectangularSchoolWithHoleFilled() throws IOException {
      rectangularSchoolWithHole(true);
   }

   @Test
   void rectangularSchoolWithHoleUnfilled() throws IOException {
      rectangularSchoolWithHole(false);
   }

   private static void rectangularSchoolWithHole(boolean fillHoles) throws IOException {
      SyntheticSchoolData syntheticData = new SyntheticSchoolData(SyntheticSchoolData.CASE_RECTANGULAR_SCHOOL_WITH_HOLE);
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(
            SyntheticSchoolData.RECTANGULAR_SCHOOL_START_PING - 5,
            SyntheticSchoolData.RECTANGULAR_SCHOOL_START_PING + SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH + 4
      );
      List<RegionInfoDatagram> infos = run(syntheticDataFile, schoolDetectionModule -> {
         schoolDetectionModule.fillHoles.setBooleanValue(fillHoles);
      });
      assertEquals(1, infos.size());
      RegionInfoDatagram info = infos.getFirst();
      assertEquals(1, info.getBorderIds().length);

      RegionInfoDatagram.BoundingBox boundingBox = info.getBoundingBox();
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_START_PING,
            syntheticDataFile.getFirstPingNumber() + boundingBox.x - 1);
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_MIN_DEPTH, boundingBox.y, 1e-7);
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH, boundingBox.width);
      assertEqualsRelative(SyntheticSchoolData.RECTANGULAR_SCHOOL_HEIGHT, boundingBox.height);

      float rectangleSampleCount = SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH * SyntheticSchoolData.RECTANGULAR_SCHOOL_HEIGHT / SyntheticSchoolData.SAMPLE_DISTANCE;
      float holeSampleCount = SyntheticSchoolData.RECTANGULAR_SCHOOL_HOLE_PING_WIDTH * SyntheticSchoolData.RECTANGULAR_SCHOOL_HOLE_HEIGHT / SyntheticSchoolData.SAMPLE_DISTANCE;
      double sampleCount = rectangleSampleCount - holeSampleCount;
      double sampleArea = SyntheticSchoolData.SAMPLE_DISTANCE * SyntheticSchoolData.METER_PER_PING;
      double length = SyntheticSchoolData.RECTANGULAR_SCHOOL_PING_WIDTH * SyntheticSchoolData.METER_PER_PING;
      double height = SyntheticSchoolData.RECTANGULAR_SCHOOL_HEIGHT;
      double holeLength = SyntheticSchoolData.RECTANGULAR_SCHOOL_HOLE_PING_WIDTH * SyntheticSchoolData.METER_PER_PING;
      double holeHeight = SyntheticSchoolData.RECTANGULAR_SCHOOL_HOLE_HEIGHT;

      RegionInfoDatagram.Values values = info.getValues();
      if (fillHoles) {
         assertEqualsRelative(rectangleSampleCount * sampleArea, values.area, 1e-6);
      } else {
         assertEqualsRelative(sampleCount * sampleArea, values.area, 1e-6);
      }
      assertEqualsRelative(sampleCount, values.sampleCount);
      assertEqualsRelative(sampleCount * PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.sumLogSv);
      assertEqualsRelative(sampleCount * SyntheticSchoolData.SV_INSIDE, values.sumSv, 1e-7);
      assertEquals(PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.logMeanSv);
      assertEqualsRelative(SyntheticSchoolData.SV_INSIDE * sampleCount * sampleArea / length, values.sa, 1e-7);
      if (fillHoles) {
         assertEqualsRelative(2 * length + 2 * height, values.perimeter);
      } else {
         assertEqualsRelative(2 * length + 2 * height + 2 * holeLength + 2 * holeHeight, values.perimeter);
      }
      assertEqualsRelative(length, values.length);
      assertEqualsRelative(height, values.maxHeight, 1e-7);
   }

   @Test
   void circularSchool() throws IOException {
      double r = SyntheticSchoolData.CIRCULAR_SCHOOL_RADIUS;
      long pFirst = SyntheticSchoolData.CIRCULAR_SCHOOL_PING_CENTER - (long) (r / SyntheticSchoolData.METER_PER_PING);
      long pLast = SyntheticSchoolData.CIRCULAR_SCHOOL_PING_CENTER + (long) (r / SyntheticSchoolData.METER_PER_PING);
      double pingCount = pLast - pFirst + 1;

      SyntheticSchoolData syntheticData = new SyntheticSchoolData(SyntheticSchoolData.CASE_CIRCULAR_SCHOOL);
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(pFirst - 5, pLast + 4);
      List<RegionInfoDatagram> infos = run(syntheticDataFile);
      assertEquals(1, infos.size());
      RegionInfoDatagram info = infos.getFirst();
      assertEquals(1, info.getBorderIds().length);

      RegionInfoDatagram.BoundingBox boundingBox = info.getBoundingBox();
      assertEqualsRelative(pFirst, syntheticDataFile.getFirstPingNumber() + boundingBox.x - 1);
      assertEqualsRelative(SyntheticSchoolData.CIRCULAR_SCHOOL_CENTER_DEPTH - r, boundingBox.y, 1e-7);
      assertEqualsRelative(pingCount, boundingBox.width);
      assertEqualsRelative(2 * r, boundingBox.height, 1e-6);

      double circleArea = Math.PI * r * r;
      double sampleArea = SyntheticSchoolData.SAMPLE_DISTANCE * SyntheticSchoolData.METER_PER_PING;
      double sampleCount = circleArea / sampleArea;
      double length = pingCount * SyntheticSchoolData.METER_PER_PING;

      RegionInfoDatagram.Values values = info.getValues();
      assertEqualsRelative(circleArea, values.area, 1e-3);
      assertEqualsRelative(sampleCount, values.sampleCount, 1e-3);
      assertEqualsRelative(sampleCount * PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.sumLogSv, 1e-3);
      assertEqualsRelative(sampleCount * SyntheticSchoolData.SV_INSIDE, values.sumSv, 1e-3);
      assertEqualsRelative(PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.logMeanSv);
      assertEqualsRelative(circleArea * SyntheticSchoolData.SV_INSIDE / length, values.sa, 1e-3);
      assertEqualsRelative(2 * Math.PI * r, values.perimeter, 0.015);
      assertEqualsRelative(length, values.length);
      assertEqualsRelative(2 * r, values.maxHeight, 1e-6);
   }

   @Test
   void complicatedSchool() throws IOException {
      SyntheticSchoolData syntheticData = new SyntheticSchoolData(SyntheticSchoolData.CASE_COMPLICATED_SCHOOL_DEFINITION);
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(
            SyntheticSchoolData.COMPLICATED_SCHOOL_FIRST_PING - 5,
            SyntheticSchoolData.COMPLICATED_SCHOOL_FIRST_PING + SyntheticSchoolData.COMPLICATED_SCHOOL_PING_WIDTH + 4
      );
      List<RegionInfoDatagram> infos = run(syntheticDataFile);
      assertEquals(1, infos.size());
      RegionInfoDatagram info = infos.getFirst();
      assertEquals(11, info.getBorderIds().length);

      double pingCount = SyntheticSchoolData.COMPLICATED_SCHOOL_DEFINITION.getFirst().length();
      double height = SyntheticSchoolData.COMPLICATED_SCHOOL_DEFINITION.size() * SyntheticSchoolData.SAMPLE_DISTANCE;

      RegionInfoDatagram.BoundingBox boundingBox = info.getBoundingBox();
      assertEqualsRelative(SyntheticSchoolData.COMPLICATED_SCHOOL_FIRST_PING,
            syntheticDataFile.getFirstPingNumber() + boundingBox.x - 1);
      assertEqualsRelative(SyntheticSchoolData.COMPLICATED_SCHOOL_FIRST_DEPTH, boundingBox.y, 1e-7);
      assertEqualsRelative(pingCount, boundingBox.width);
      assertEqualsRelative(height, boundingBox.height);

      double sampleArea = SyntheticSchoolData.SAMPLE_DISTANCE * SyntheticSchoolData.METER_PER_PING;
      double sampleCount = SyntheticSchoolData.COMPLICATED_SCHOOL_DEFINITION.stream()
            .flatMapToInt(String::chars)
            .filter(c -> c == '█')
            .count();
      double length = pingCount * SyntheticSchoolData.METER_PER_PING;

      RegionInfoDatagram.Values values = info.getValues();
      assertEqualsRelative((sampleCount + SyntheticSchoolData.COMPLICATED_SCHOOL_HOLE_SAMPLE_COUNT) * sampleArea, values.area, 1e-5);
      assertEqualsRelative(sampleCount, values.sampleCount);
      assertEqualsRelative(sampleCount * PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.sumLogSv);
      assertEqualsRelative(sampleCount * SyntheticSchoolData.SV_INSIDE, values.sumSv, 1e-7);
      assertEqualsRelative(PowerData.svToLogSv(SyntheticSchoolData.SV_INSIDE), values.logMeanSv);
      assertEqualsRelative(sampleCount * sampleArea * SyntheticSchoolData.SV_INSIDE / length, values.sa, 1e-7);
      //assertEqualsRelative(??, values.perimeter);
      assertEqualsRelative(length, values.length);
      assertEqualsRelative(height, values.maxHeight, 1e-7);
   }

   @Test
   void randomSchool() throws IOException {
      JUnitUtils.runWithRandom(random -> {
         SyntheticData syntheticData = new SyntheticData() {
            @Override
            public int getTransducerCount() {
               return 1;
            }

            @Override
            public float getFrequency(int channel) {
               return 38_000;
            }

            @Override
            public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
               float[] sv = new float[100];
               int n = random.nextInt(5);
               for (int i = 0; i < n; i++) {
                  int a = random.nextInt(sv.length);
                  int b = random.nextInt(a, sv.length);
                  Arrays.fill(sv, a, b, SyntheticSchoolData.SV_INSIDE);
               }
               powerData.setSv(sv);
            }
         };
         run(syntheticData.withFirstAndLastPingNumber(1, random.nextInt(1, 100)));
      });
   }

   private static void assertEqualsRelative(double expected, double actual) {
      assertEquals(expected, actual, () -> "Relative error: " + Math.abs((actual - expected) / expected));
   }

   private static void assertEqualsRelative(double expected, double actual, double relativeDelta) {
      Supplier<String> messageSupplier = () -> "Relative error: " + Math.abs((actual - expected) / expected);
      assertEquals(expected, actual, Math.abs(expected) * relativeDelta, messageSupplier);
      // Optionally check that the relative delta is as small as possible:
      //assertTrue(Math.abs((actual - expected) / expected) > relativeDelta / 10, messageSupplier);
   }

   private static List<RegionInfoDatagram> run(SyntheticDataFile syntheticDataFile) throws IOException {
      return run(syntheticDataFile, Utils.emptyConsumer());
   }

   private static List<RegionInfoDatagram> run(SyntheticDataFile syntheticDataFile, Consumer<SchoolDetectionModule> config) throws IOException {
      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      moduleContainer.getConfigFileSettings().restoreInstallationLocations();
      SchoolDetectionModule schoolDetectionModule = moduleContainer.addModule(new SchoolDetectionModule());
      schoolDetectionModule.length.setMin(0);
      schoolDetectionModule.thickness.setMin(0);
      schoolDetectionModule.area.setMin(0);
      config.accept(schoolDetectionModule);

      List<RegionInfoDatagram> regionInfoDatagrams = new ArrayList<>();
      List<RegionTableOfContentsDatagram> regionTableOfContentsDatagrams = new ArrayList<>();

      Set<Integer> finishedBorderIds = new HashSet<>();
      Set<Integer> activeBorderIds = new HashSet<>();
      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
            ping.getPingItems(RegionBorderDatagram.class).forEach(border -> {
               for (RegionBorderDatagram.BorderInfo borderInfo : border.getBorderInfos()) {
                  activeBorderIds.add(borderInfo.id());
                  assertFalse(finishedBorderIds.contains(borderInfo.id()));
               }
            });
            for (PingItem pingItem : ping.getPingItems()) {
               switch (pingItem) {
                  case RegionInfoDatagram info -> {
                     regionInfoDatagrams.add(info);
                     for (Integer id : info.getBorderIds()) {
                        assertTrue(activeBorderIds.remove(id));
                        assertTrue(finishedBorderIds.add(id));
                     }
                  }
                  case RegionTableOfContentsDatagram toc -> {
                     regionTableOfContentsDatagrams.add(toc);
                  }
                  default -> {
                  }
               }
            }
         }
      }

      if (regionInfoDatagrams.isEmpty()) {
         assertEquals(0, regionTableOfContentsDatagrams.size());
      } else {
         assertEquals(1, regionTableOfContentsDatagrams.size());
         RegionTableOfContentsDatagram toc = regionTableOfContentsDatagrams.getFirst();
         List<Instant> infoInstants = regionInfoDatagrams.stream()
               .map(BaseDatagram::getInstant)
               .distinct()
               .toList();
         assertEquals(toc.getInstants(), infoInstants);
      }
      assertEquals(0, activeBorderIds.size());

      return regionInfoDatagrams;
   }
}
