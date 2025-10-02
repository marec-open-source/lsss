package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

public final class DeepVisionFrames {
   @XmlAttribute
   public String fileformat = "";
   @XmlElement(name = "frame")
   public List<DeepVisionFrame> frames = new ArrayList<>();

   public DeepVisionFrames() {
   }
}
