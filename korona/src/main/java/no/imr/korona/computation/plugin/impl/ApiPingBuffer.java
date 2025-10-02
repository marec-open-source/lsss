package no.imr.korona.computation.plugin.impl;

import no.marec.api.korona.PingBuffer;

import java.util.ArrayList;
import java.util.List;

public final class ApiPingBuffer implements PingBuffer {
   private final int bufferRadius;
   private final List<ApiPing> pings = new ArrayList<>();
   private int centerIndex = -1;

   public ApiPingBuffer(int bufferRadius) {
      this.bufferRadius = bufferRadius;
   }

   @Override
   public int getBeginIndex() {
      return -centerIndex;
   }

   @Override
   public int getEndIndex() {
      return pings.size() - centerIndex;
   }

   @Override
   public ApiPing getPing(int index) {
      return pings.get(centerIndex + index);
   }

   public void add(ApiPing apiPing) {
      pings.addFirst(apiPing);
      centerIndex++;
   }

   public void advanceCenter() {
      centerIndex--;
      if (getEndIndex() > bufferRadius + 1) {
         pings.removeLast();
      }
   }

   public boolean isBufferingComplete() {
      return centerIndex >= bufferRadius;
   }

   public boolean isDone() {
      return centerIndex < 0;
   }
}
