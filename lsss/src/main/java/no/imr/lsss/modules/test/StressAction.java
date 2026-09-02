package no.imr.lsss.modules.test;

public record StressAction(String name, Runnable action) implements Comparable<StressAction> {
   @Override
   public String toString() {
      return name;
   }

   @Override
   public int compareTo(StressAction other) {
      return name.compareTo(other.name);
   }
}
