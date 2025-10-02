package no.imr.korona.util.ts;

public record TSDetection(
      int beginIndex,
      int peakIndex,
      int endIndex
) {
   public int getSampleCount() {
      return endIndex - beginIndex;
   }
}
