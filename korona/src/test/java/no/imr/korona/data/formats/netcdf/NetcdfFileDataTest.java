package no.imr.korona.data.formats.netcdf;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.computation.ModuleContainerComputation;
import no.imr.korona.computation.netcdf.NetcdfWriterModule;
import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.AngleData;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.test.UniqueTmpDir;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ucar.nc2.ffi.netcdf.NetcdfClibrary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class NetcdfFileDataTest {
   @BeforeAll
   static void beforeAll() {
      Assumptions.assumeTrue(NetcdfClibrary.isLibraryPresent(), "NetCDF-C library not available");
   }

   @Test
   void gridded() throws IOException {
      SyntheticData syntheticData = new SyntheticData() {
         @Override
         public void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
            float[] sv = new float[10];
            Arrays.fill(sv, PowerData.logSvToSv(-50));
            powerData.setSv(sv);

            float[] electricalAngles = new float[sv.length * 2];
            Arrays.fill(electricalAngles, 1);
            powerData.setElectricAngles(electricalAngles);
         }
      };
      SyntheticDataFile syntheticDataFile = syntheticData.withFirstAndLastPingNumber(1, 3);

      ModuleContainer moduleContainer = new ModuleContainer(new Korona());
      Path koronaDir = UniqueTmpDir.newSubDir("NetcdfFileDataTest");
      moduleContainer.setAssociatedKoronaDirectory(koronaDir);

      NetcdfWriterModule netcdfWriterModule = moduleContainer.addModule(new NetcdfWriterModule());
      netcdfWriterModule.dirName.setValue("gridded");
      netcdfWriterModule.writerType.setValue(NetcdfWriterModule.WriterType.GRIDDED);
      netcdfWriterModule.griddedOutputType.setValue(NetcdfWriterModule.GriddedOutputType.SV_AND_ANGLES);

      try (ModuleContainerComputation computation = moduleContainer.createComputation(syntheticDataFile.toPingReader())) {
         while (true) {
            Ping ping = computation.nextPing();
            if (ping == null) {
               break;
            }
         }
      }

      String ncBaseName = FileUtils.baseName(syntheticDataFile.toFile());
      Path ncFile = koronaDir.resolve(netcdfWriterModule.dirName.getValue()).resolve(ncBaseName + ".nc");

      try (NetcdfSegmentData netcdfSegmentData = new NetcdfSegmentData(ncFile)) {
         check(syntheticDataFile.getRawFileConfiguration().getTransducers(), netcdfSegmentData.getRawFileConfiguration().getTransducers());

         List<PingIndex> pingIndexes = netcdfSegmentData.getPingIndices();
         assertEquals(3, pingIndexes.size());

         int transducerCount = syntheticDataFile.getRawFileConfiguration().getTransducerCount();
         for (PingIndex pingIndex : pingIndexes) {
            Ping ping = netcdfSegmentData.loadPing(pingIndex, new AsyncHandle());
            for (int channel = 1; channel <= transducerCount; channel++) {
               PowerData powerData = ping.getPowerData(channel);
               Ping expectedPing = syntheticDataFile.createPing(pingIndex);
               check(expectedPing.getPowerData(channel), powerData);
            }
         }
      }

      FileUtils.deleteRecursively(koronaDir);
   }

   private static void check(List<RawFileTransducer> actualTransducers, List<RawFileTransducer> expectedTransducers) {
      assertEquals(expectedTransducers.size(), actualTransducers.size());
      for (int i = 0; i < expectedTransducers.size(); i++) {
         RawFileTransducer expectedTransducer = expectedTransducers.get(i);
         RawFileTransducer actualTransducer = actualTransducers.get(i);
         assertEquals(expectedTransducer.getChannelId(), actualTransducer.getChannelId());
         assertEquals(expectedTransducer.getBeamType(), actualTransducer.getBeamType());
         assertEquals(expectedTransducer.getFrequency(), actualTransducer.getFrequency());
         assertEquals(expectedTransducer.getEquivalentBeamAngle(), actualTransducer.getEquivalentBeamAngle());
         assertEquals(expectedTransducer.getBeamWidthAlongship(), actualTransducer.getBeamWidthAlongship());
         assertEquals(expectedTransducer.getBeamWidthAthwartship(), actualTransducer.getBeamWidthAthwartship());
         assertEquals(expectedTransducer.getAngleSensitivityAlongship(), actualTransducer.getAngleSensitivityAlongship());
         assertEquals(expectedTransducer.getAngleSensitivityAthwartship(), actualTransducer.getAngleSensitivityAthwartship());
         assertEquals(expectedTransducer.getAngleOffsetAlongship(), actualTransducer.getAngleOffsetAlongship());
         assertEquals(expectedTransducer.getAngleOffsetAthwartship(), actualTransducer.getAngleOffsetAthwartship());
      }
   }

   private static void check(@Nullable PowerData expected, @Nullable PowerData actual) {
      if (expected == null) {
         assertNull(actual);
         return;
      }
      assertNotNull(actual);

      assertArrayEquals(expected.getSv(), actual.getSv());
      assertArrayEquals(expected.computeNoisePowerIndex(), actual.computeNoisePowerIndex());
      assertArrayEquals(expected.computeShortPower(), actual.computeShortPower());

      AngleData expectedAngleData = expected.getAngleData();
      AngleData actualAngleData = actual.getAngleData();
      if (expectedAngleData == null) {
         assertNull(actualAngleData);
      } else {
         assertNotNull(actualAngleData);
         assertArrayEquals(expectedAngleData.getElectricalAngles(), actualAngleData.getElectricalAngles());
      }
   }
}
