package no.imr.korona.data.util.geometry;

import no.imr.korona.data.ping.PingIndex;

/**
 * A location specified by a PingIndex and a depth.
 */
public record EchogramPoint(PingIndex pingIndex, float depth) implements Comparable<EchogramPoint> {

   public EchogramPoint withDepth(float depth) {
      return new EchogramPoint(pingIndex, depth);
   }

   @Override
   public String toString() {
      return "{pingNumber=" + pingIndex.getPingNumber() + ", depth=" + depth + '}';
   }

   /**
    * Compare this point to another first by depth (shallow to deep)
    * secondly by PingIndex.
    *
    * @param other another echogram point
    * @return the comparison result
    */
   @Override
   public int compareTo(EchogramPoint other) {
      int depthCompare = Float.compare(depth, other.depth);
      return depthCompare != 0 ? depthCompare : pingIndex.compareTo(other.pingIndex);
   }
}
