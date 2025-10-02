package no.imr.korona.computation.tracking.data;

import no.imr.korona.data.ping.items.channel.PowerData;

public record Measurement(float range, float alongshipAngle, float athwartshipAngle, float ts) {

   public Measurement(PowerData powerData, int sampleIndex, float ts) {
      this(powerData.getSampleRange(sampleIndex),
            (float) Math.toRadians(powerData.getMechanicalAlongAngle(sampleIndex)),
            (float) Math.toRadians(powerData.getMechanicalAthwartAngle(sampleIndex)),
            ts);
   }
}
