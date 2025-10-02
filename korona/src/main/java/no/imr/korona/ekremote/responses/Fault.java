package no.imr.korona.ekremote.responses;

import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public record Fault(
      int errorCode,
      @Nullable String message
) {
   static Fault parse(@Nullable Element element) {
      if (element == null) {
         return new Fault(0, null);
      }
      Element detailElement = element.element("detail");
      if (detailElement == null) {
         return new Fault(0, null);
      }
      String errorCodeText = detailElement.elementText("errorcode");
      int errorCode = errorCodeText != null ? Integer.parseInt(errorCodeText) : 0;
      String message = detailElement.elementText("message");
      return new Fault(errorCode, message);
   }

   public boolean isError() {
      return errorCode != 0;
   }
}
