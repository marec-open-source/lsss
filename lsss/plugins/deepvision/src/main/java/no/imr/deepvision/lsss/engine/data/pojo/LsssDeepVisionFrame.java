package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlAttribute;

public final class LsssDeepVisionFrame {
   @XmlAttribute
   public long time;
   @XmlAttribute
   public String color = "";
   @XmlAttribute
   public float size = 10;

   public LsssDeepVisionFrame() {
   }

   @Override
   public String toString() {
      return "LsssDeepVisionFrame{" +
            "time=" + time +
            ", color='" + color + '\'' +
            ", size=" + size +
            '}';
   }
}
