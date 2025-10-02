package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = DeepVisionFile.XML_ROOT_ELEMENT_NAME)
public final class DeepVisionFile {
   public static final String XML_ROOT_ELEMENT_NAME = "deepvision";

   public DeepVisionFrames frames = new DeepVisionFrames();

   public DeepVisionFile() {
   }
}
