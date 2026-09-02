package no.imr.korona.util;

import no.imr.tools.math.WelfordsMethod;

public final class SvSum {
   private final WelfordsMethod[] welfordsMethod;

   public SvSum(int channelCount) {
      welfordsMethod = new WelfordsMethod[channelCount];
      reset();
   }

   public WelfordsMethod getWelfordsMethod(int channelIndex) {
      return welfordsMethod[channelIndex];
   }

   public void reset() {
      for (int i = 0; i < welfordsMethod.length; i++) {
         welfordsMethod[i] = new WelfordsMethod();
      }
   }

   public void accumulate(SvSum svSum) {
      for (int i = 0; i < welfordsMethod.length; i++) {
         welfordsMethod[i].update(svSum.welfordsMethod[i]);
      }
   }
}
