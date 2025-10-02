package no.imr.lsss.server.pojo.ping;

import no.imr.korona.data.ping.items.channel.PowerData;
import org.jspecify.annotations.Nullable;

import java.util.List;

public final class PojoChannel {
   public String id;
   public float frequency;
   public float sampleDistance;
   public int offset;
   public float @Nullable [] sv;
   public float @Nullable [] tsc;
   public float @Nullable [] tsu;
   public float @Nullable [] alongshipAngle;
   public float @Nullable [] athwartshipAngle;
   public @Nullable PojoComplexArray pulseCompressed;
   public @Nullable List<PojoComplexArray> complex;

   public PojoChannel(PowerData powerData) {
      id = powerData.getTransducer().getChannelId();
      frequency = powerData.getFrequency();
      sampleDistance = powerData.getSampleDistance();
      offset = powerData.getOffset();
   }

   @Override
   public String toString() {
      return "PojoChannel{" +
            "id='" + id + '\'' +
            ", frequency=" + frequency +
            ", sampleDistance=" + sampleDistance +
            ", offset=" + offset +
            '}';
   }
}
