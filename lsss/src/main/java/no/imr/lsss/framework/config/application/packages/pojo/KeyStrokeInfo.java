package no.imr.lsss.framework.config.application.packages.pojo;

import no.imr.lsss.framework.packages.LsssPackage;

public final class KeyStrokeInfo extends ActionReference {
   public String keyStroke = "";
   public String context = LsssPackage.KEY_STROKE_CONTEXT_MAIN_WINDOW;

   public KeyStrokeInfo() {
   }

   @Override
   public String toString() {
      return "KeyStrokeInfo{" +
            "packageId='" + packageId + '\'' +
            ", actionId='" + actionId + '\'' +
            ", keyStroke='" + keyStroke + '\'' +
            ", context='" + context + '\'' +
            '}';
   }
}
