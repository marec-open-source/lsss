package no.imr.korona.data.ping;

import org.jspecify.annotations.Nullable;

/**
 * Abstract helper base class for ping indices.
 */
public abstract class AbstractPingIndex implements PingIndex {
   AbstractPingIndex() {
   }

   /**
    * Tests for equality using ping number only.
    *
    * @param obj another PingIndex
    * @return {@code true} if the ping numbers are equal
    */
   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof PingIndex that
            && getPingNumber() == that.getPingNumber();
   }

   @Override
   public int hashCode() {
      return Long.hashCode(getPingNumber());
   }

   @Override
   public String toString() {
      return getClass().getSimpleName() + " " + getPingNumber();
   }
}
