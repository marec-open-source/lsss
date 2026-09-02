package no.imr.korona.computation.noise;

import no.imr.korona.data.formats.synthetic.SyntheticDataFile;
import no.imr.korona.test.data.ConstantSyntheticData;
import org.dom4j.Document;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

final class NoiseFileTest {
   @Test
   void update() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1);
      NoiseFile noiseFile = new NoiseFile(syntheticDataFile.getRawFileConfiguration(), null, 2);

      Instant t0 = Instant.EPOCH;
      assertEquals(Map.of(), noiseFile.timeToNoiseMap(t0));

      noiseFile.update(new NoiseFile.NoiseData(10, 20, 30), 1, t0);
      assertEquals(Map.of(18, new NoiseFile.NoiseData(10, 20, 30)), noiseFile.timeToNoiseMap(t0));
      assertEquals(Map.of(), noiseFile.timeToNoiseMap(t0.plusSeconds(3600)));

      noiseFile.update(new NoiseFile.NoiseData(10, 20, 40), 1, t0);
      assertEquals(Map.of(18, new NoiseFile.NoiseData(10, 20, 40)), noiseFile.timeToNoiseMap(t0));

      noiseFile.update(new NoiseFile.NoiseData(8, 20, 40), 1, t0);
      assertEquals(Map.of(18, new NoiseFile.NoiseData(8, 20, 40)), noiseFile.timeToNoiseMap(t0));

      noiseFile.update(new NoiseFile.NoiseData(7, 20, 39), 1, t0);
      noiseFile.update(new NoiseFile.NoiseData(9, 20, 40), 1, t0);
      assertEquals(Map.of(18, new NoiseFile.NoiseData(8, 20, 40)), noiseFile.timeToNoiseMap(t0));

      Instant t1 = Instant.EPOCH.plusSeconds(3600);
      noiseFile.update(new NoiseFile.NoiseData(41, 21, 51), 1, t1);
      assertEquals(Map.of(18, new NoiseFile.NoiseData(8, 20, 40)), noiseFile.timeToNoiseMap(t0));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(8, 20, 40)), noiseFile.timeToNoiseMap(t0.plusSeconds(3599)));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(41, 21, 51)), noiseFile.timeToNoiseMap(t1));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(41, 21, 51)), noiseFile.timeToNoiseMap(t1.plusSeconds(3599)));
      assertEquals(Map.of(), noiseFile.timeToNoiseMap(t1.plusSeconds(3600)));

      Instant t2 = Instant.EPOCH.plusSeconds(2 * 3600);
      noiseFile.update(new NoiseFile.NoiseData(42, 22, 52), 1, t2);
      assertEquals(Map.of(), noiseFile.timeToNoiseMap(t0));
      assertEquals(Map.of(), noiseFile.timeToNoiseMap(t0.plusSeconds(3599)));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(41, 21, 51)), noiseFile.timeToNoiseMap(t1));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(41, 21, 51)), noiseFile.timeToNoiseMap(t1.plusSeconds(3599)));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(42, 22, 52)), noiseFile.timeToNoiseMap(t2));
      assertEquals(Map.of(18, new NoiseFile.NoiseData(42, 22, 52)), noiseFile.timeToNoiseMap(t2.plusSeconds(3599)));
      assertEquals(Map.of(), noiseFile.timeToNoiseMap(t2.plusSeconds(3600)));
   }

   @Test
   void xml() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1);
      NoiseFile noiseFile = new NoiseFile(syntheticDataFile.getRawFileConfiguration(), null, 2);

      noiseFile.update(new NoiseFile.NoiseData(11, 21, 31), 1, Instant.EPOCH);
      noiseFile.update(new NoiseFile.NoiseData(21, 31, 41), 1, Instant.EPOCH.plusSeconds(3700));
      noiseFile.update(new NoiseFile.NoiseData(22, 32, 42), 2, Instant.EPOCH.plusSeconds(3700));
      assertEquals(Map.of(
                  Instant.EPOCH, Map.of(18, new NoiseFile.NoiseData(11, 21, 31)),
                  Instant.EPOCH.plusSeconds(3600), Map.of(
                        18, new NoiseFile.NoiseData(21, 31, 41),
                        38, new NoiseFile.NoiseData(22, 32, 42)
                  )
            ),
            noiseFile.getTimeToNoiseMap()
      );

      Document doc = noiseFile.toXML();

      NoiseFile noiseFile2 = new NoiseFile(syntheticDataFile.getRawFileConfiguration(), null, 2);
      noiseFile2.fromXML(doc);
      assertEquals(noiseFile.getTimeToNoiseMap(), noiseFile2.getTimeToNoiseMap());
   }

   @Test
   void mergedNoiseDocument() {
      SyntheticDataFile syntheticDataFile = new ConstantSyntheticData().withFirstAndLastPingNumber(1, 1);
      NoiseFile a = new NoiseFile(syntheticDataFile.getRawFileConfiguration(), null, 2);
      a.update(new NoiseFile.NoiseData(11, 21, 31), 1, Instant.EPOCH);
      a.update(new NoiseFile.NoiseData(21, 31, 41), 1, Instant.EPOCH.plusSeconds(3600));

      NoiseFile b = new NoiseFile(syntheticDataFile.getRawFileConfiguration(), null, 2);
      b.update(new NoiseFile.NoiseData(21, 31, 42), 1, Instant.EPOCH.plusSeconds(3600));
      b.update(new NoiseFile.NoiseData(31, 41, 51), 1, Instant.EPOCH.plusSeconds(2 * 3600));

      Document doc = b.mergedNoiseDocument(a.toXML());

      NoiseFile c = new NoiseFile(syntheticDataFile.getRawFileConfiguration(), null, 2);
      c.fromXML(doc);
      assertEquals(Map.of(
                  Instant.EPOCH.plusSeconds(3600), Map.of(18, new NoiseFile.NoiseData(21, 31, 42)),
                  Instant.EPOCH.plusSeconds(2 * 3600), Map.of(18, new NoiseFile.NoiseData(31, 41, 51))
            ),
            c.getTimeToNoiseMap()
      );
   }
}
