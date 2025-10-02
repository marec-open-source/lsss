package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

public final class DeepVisionFrame {
   @XmlAttribute
   public long time;
   @XmlAttribute
   public String id = "";
   @XmlAttribute
   public String folder = "";
   @XmlAttribute
   public double lat = Double.NaN;
   @XmlAttribute
   public double lon = Double.NaN;
   @XmlAttribute
   public float depth = -1000;
   @XmlAttribute
   public boolean active = true;

   @XmlElement(name = "framework")
   public List<DeepVisionFramework> frameworks = new ArrayList<>();

   public DeepVisionFrame() {
   }

   @Override
   public String toString() {
      return "DeepVisionFrame{" +
            "time=" + time +
            ", id='" + id + '\'' +
            ", folder='" + folder + '\'' +
            ", lat=" + lat +
            ", lon=" + lon +
            ", depth=" + depth +
            ", active=" + active +
            ", frameworks=" + frameworks +
            '}';
   }
}
