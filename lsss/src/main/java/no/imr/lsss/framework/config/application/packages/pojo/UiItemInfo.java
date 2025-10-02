package no.imr.lsss.framework.config.application.packages.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

public abstract class UiItemInfo extends ActionReference {
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String text = "";

   UiItemInfo() {
   }
}
