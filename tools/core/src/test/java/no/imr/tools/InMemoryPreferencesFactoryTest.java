package no.imr.tools;

import org.junit.jupiter.api.Test;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

final class InMemoryPreferencesFactoryTest {
   @Test
   void test() throws BackingStoreException {
      InMemoryPreferencesFactory.checkInstallation();

      Preferences p = Preferences.userRoot();
      p.node("a/b").put("kab", "vab");
      assertTrue(p.nodeExists("a"));
      assertArrayEquals(new String[]{"a"}, p.childrenNames());
      p.node("a").removeNode();
      assertFalse(p.nodeExists("a"));
      assertArrayEquals(new String[]{}, p.childrenNames());
   }
}
