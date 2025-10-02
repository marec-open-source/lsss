package no.imr.korona.computation.plugin.impl;

import no.imr.korona.data.ping.Ping;

final class NullChannel extends ApiChannel {
   private final Ping ping;
   private final int channel;

   NullChannel(Ping ping, int channel) {
      this.ping = ping;
      this.channel = channel;
   }

   @Override
   void applyValidArray() {
   }

   @Override
   public int getChannelNumber() {
      return channel;
   }

   @Override
   public double getFrequency() {
      return ping.getRawFileConfiguration().getTransducers().get(channel - 1).getFrequency();
   }

   @Override
   public int getBeginSampleIndex() {
      return 0;
   }

   @Override
   public int getEndSampleIndex() {
      return 0;
   }

   @Override
   public double getSv(int sampleIndex) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setSv(int sampleIndex, double sv) {
      throw new UnsupportedOperationException();
   }

   @Override
   public double getLogSv(int sampleIndex) {
      throw new UnsupportedOperationException();
   }

   @Override
   public void setLogSv(int sampleIndex, double logSv) {
      throw new UnsupportedOperationException();
   }
}
