package no.imr.korona.computation.categorization;

import no.imr.korona.computation.categorization.apriori.PerPingAPriori;
import no.imr.korona.data.ping.items.channel.PowerData;

/**
 * Contains a pixel array, a sample distance and an offset.
 * The offset is relative to the sea surface.
 */
final class CategorizationPing {
   private final PowerData referenceDatagram;
   private final PerPingAPriori perPingAPriori;
   private final Pixel[] pixels;
   private final int offset;
   private final float sampleDistance;

   CategorizationPing(PowerData referenceDatagram, PerPingAPriori perPingAPriori) {
      this.referenceDatagram = referenceDatagram;
      this.perPingAPriori = perPingAPriori;
      float[] logSv = referenceDatagram.getLogSv();
      pixels = new Pixel[logSv.length];
      for (int i = 0; i < pixels.length; i++) {
         pixels[i] = new Pixel(logSv[i]);
      }

      offset = referenceDatagram.getOffset()
            + (int) (referenceDatagram.getHeaveCorrectedTransducerDepth() / referenceDatagram.getSampleDistance());

      sampleDistance = referenceDatagram.getSampleDistance();
   }

   PowerData getReferenceDatagram() {
      return referenceDatagram;
   }

   PerPingAPriori getPerPingAPriori() {
      return perPingAPriori;
   }

   Pixel[] getPixels() {
      return pixels;
   }

   int getOffset() {
      return offset;
   }

   float getSampleDistance() {
      return sampleDistance;
   }

   float getFirstDepth() {
      return offset * sampleDistance;
   }
}
