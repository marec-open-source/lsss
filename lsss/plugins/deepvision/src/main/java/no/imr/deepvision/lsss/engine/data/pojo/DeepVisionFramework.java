package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

public final class DeepVisionFramework {
   @XmlAttribute
   public String name = "";
   @XmlAttribute
   public String version = "";
   @XmlAttribute
   public String species = "";
   @XmlAttribute
   public String labeltype = "";
   @XmlAttribute
   public long labeltime;
   @XmlAttribute
   public float confidence;
   @XmlAttribute
   public String annotator = "";
   @XmlAttribute
   public boolean groundtruth;

   @XmlElement(name = "object")
   public List<DeepVisionFrameworkObject> objects = new ArrayList<>();

   public DeepVisionFramework() {
   }

   @Override
   public String toString() {
      return "DeepVisionFramework{" +
            "name='" + name + '\'' +
            ", version='" + version + '\'' +
            ", species='" + species + '\'' +
            ", labeltype='" + labeltype + '\'' +
            ", labeltime=" + labeltime +
            ", confidence=" + confidence +
            ", annotator='" + annotator + '\'' +
            ", groundtruth=" + groundtruth +
            ", objects=" + objects +
            '}';
   }
}
