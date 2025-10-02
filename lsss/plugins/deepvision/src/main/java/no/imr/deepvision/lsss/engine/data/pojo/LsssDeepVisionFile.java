package no.imr.deepvision.lsss.engine.data.pojo;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = LsssDeepVisionFile.XML_ROOT_ELEMENT_NAME)
public final class LsssDeepVisionFile {
   public static final String XML_ROOT_ELEMENT_NAME = "lsss-deepvision";

   public LsssDeepVisionFrames frames = new LsssDeepVisionFrames();

   public LsssDeepVisionFile() {
   }
}
