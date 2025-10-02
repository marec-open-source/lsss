package no.imr.tools.jogl.volumetric;

import no.imr.tools.math.linalg.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class MarchingCubeTest {
   @Test
   void test() {
      Vec3[] coords = {
            new Vec3(-1, -1, -1),
            new Vec3(1, -1, -1),
            new Vec3(1, -1, 1),
            new Vec3(-1, -1, 1),
            new Vec3(-1, 1, -1),
            new Vec3(1, 1, -1),
            new Vec3(1, 1, 1),
            new Vec3(-1, 1, 1)
      };
      float[] param = {
            -1,
            -1,
            -1,
            -1,
            1,
            1,
            1,
            1
      };

      MarchingCube mc = new MarchingCube(0, coords, param);
      assertEquals(2, mc.getPolys().size());
      assertEquals(List.of(new Vec3(1, 0, 1), new Vec3(1, 0, -1), new Vec3(-1, 0, -1)), mc.getPolys().get(0).getPoints());
      assertEquals(List.of(new Vec3(1, 0, 1), new Vec3(-1, 0, -1), new Vec3(-1, 0, 1)), mc.getPolys().get(1).getPoints());
   }
}
