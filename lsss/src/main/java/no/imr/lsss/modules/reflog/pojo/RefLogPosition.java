package no.imr.lsss.modules.reflog.pojo;

import org.jspecify.annotations.Nullable;

import java.util.Arrays;

public final class RefLogPosition {
   public double @Nullable [] coordinates;

   public RefLogPosition() {
   }

   @Override
   public String toString() {
      return "RefLogPosition{" +
            "coordinates=" + Arrays.toString(coordinates) +
            '}';
   }
}
