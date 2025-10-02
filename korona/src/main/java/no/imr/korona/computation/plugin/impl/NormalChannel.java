package no.imr.korona.computation.plugin.impl;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import org.jspecify.annotations.Nullable;

final class NormalChannel extends ApiChannel {
   private final Ping ping;
   private final PowerData powerData;
   private float @Nullable [] modifiedLogSv;

   NormalChannel(Ping ping, PowerData powerData) {
      this.ping = ping;
      this.powerData = powerData;
   }

   @Override
   void applyValidArray() {
      if (modifiedLogSv != null) {
         System.arraycopy(modifiedLogSv, 0, powerData.getLogSv(), 0, modifiedLogSv.length);
         powerData.setLogSv(powerData.getLogSv());
      }
   }

   @Override
   public int getChannelNumber() {
      return powerData.getChannel();
   }

   @Override
   public double getFrequency() {
      return ping.getRawFileConfiguration().getTransducers().get(powerData.getChannel() - 1).getFrequency();
   }

   @Override
   public int getBeginSampleIndex() {
      return 0;
   }

   @Override
   public int getEndSampleIndex() {
      return powerData.getCount();
   }

   @Override
   public double getSv(int sampleIndex) {
      return powerData.getSv()[sampleIndex];
   }

   @Override
   public void setSv(int sampleIndex, double sv) {
      setLogSv(sampleIndex, PowerData.svToLogSv((float) sv));
   }

   @Override
   public double getLogSv(int sampleIndex) {
      return powerData.getLogSv()[sampleIndex];
   }

   @Override
   public void setLogSv(int sampleIndex, double logSv) {
      if (modifiedLogSv == null) {
         modifiedLogSv = powerData.getLogSv().clone();
      }
      modifiedLogSv[sampleIndex] = (float) logSv;
   }
}
