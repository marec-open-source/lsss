package no.imr.korona.data.formats.ek60;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class EK60FileSetTest {
   @Test
   void xyz() {
      Path dir = Path.of("EK60FileSetTest");
      Path raw = dir.resolve("cruise2046-D20201211-T132053.raw");
      Path idx = dir.resolve("cruise2046-D20201211-T132053.idx");
      Path bot = dir.resolve("cruise2046-D20201211-T132053.bot");
      Path xyz = dir.resolve("cruise2046-D20201211-T132053-ES38-7 Serial No  326 - Narrow.XYZ");
      Path xyz1 = dir.resolve("cruise2046-D20201211-T085515-ES38-7 Serial No  326 - Narrow.XYZ"); // Sorts before
      Path xyz2 = dir.resolve("cruise2046-D20201211-T132303-ES38-7 Serial No  326 - Narrow.XYZ"); // Sorts after
      Set<Path> allFiles = Set.of(raw, idx, /* bot not here */ xyz, xyz1, xyz2);
      EK60FileSet ek60FileSet = new EK60FileSet(raw, XyzUtils.toXyzFiles(allFiles));
      assertEquals(List.of(xyz), ek60FileSet.getXyz());
      assertEquals(List.of(raw, idx, bot, xyz), ek60FileSet.getFiles());
   }
}
