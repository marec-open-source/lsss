package no.imr.lsss.framework.config.application.packages.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;

public abstract class ActionReference {
   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String packageId = "";

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public String actionId = "";

   ActionReference() {
   }
}
