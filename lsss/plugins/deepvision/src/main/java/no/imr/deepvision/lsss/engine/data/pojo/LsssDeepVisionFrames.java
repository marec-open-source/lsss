package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlElement;

import java.util.ArrayList;
import java.util.List;

public final class LsssDeepVisionFrames {
   @XmlElement(name = "frame")
   public List<LsssDeepVisionFrame> frames = new ArrayList<>();

   public LsssDeepVisionFrames() {
   }
}
