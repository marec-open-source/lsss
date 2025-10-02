package no.imr.tools;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class VersionTest {
   @Test
   void test() {
      testSame("1", "1.0");
      testSame("1", "1.0.0");
      testSame("1.0-rc1", "1.0.0-rc1");

      testDifferent("1.2", "1.3");
      testDifferent("1.2", "1.2.1");
      testDifferent("1.2.9", "1.2.10");
      testDifferent("1.2-alpha1", "1.2-alpha2");
      testDifferent("1.2-alpha2", "1.2-beta1");
      testDifferent("1.2-beta2", "1.2-rc1");
      testDifferent("1.2-rc1", "1.2-rc2");
      testDifferent("1.2-rc3", "1.2");
   }

   private static void testSame(String a, String b) {
      Version versionA = new Version(a);
      Version versionB = new Version(b);

      assertEquals(0, versionA.compareTo(versionB));
      assertEquals(0, versionB.compareTo(versionA));

      assertFalse(versionA.isOlderThan(versionB));
      assertFalse(versionB.isOlderThan(versionA));

      assertFalse(versionA.isNewerThan(versionB));
      assertFalse(versionB.isNewerThan(versionA));
   }

   private static void testDifferent(String less, String more) {
      Version lessVersion = new Version(less);
      Version moreVersion = new Version(more);

      assertTrue(lessVersion.compareTo(moreVersion) < 0);
      assertTrue(moreVersion.compareTo(lessVersion) > 0);

      assertFalse(moreVersion.isOlderThan(lessVersion));
      assertTrue(lessVersion.isOlderThan(moreVersion));

      assertTrue(moreVersion.isNewerThan(lessVersion));
      assertFalse(lessVersion.isNewerThan(moreVersion));
   }
}
