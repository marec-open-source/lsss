package no.imr.tools.plot;

import org.jspecify.annotations.Nullable;

public interface XYInfoContainer {
   default @Nullable XYInfo getXYInfo(int series) {
      return null;
   }
}
