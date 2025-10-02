package no.imr.lsss.server.pojo;

import no.imr.lsss.framework.packages.LsssAction;

public final class ApiLsssAction {
   public String id;
   public String label;
   public boolean enabled;

   public ApiLsssAction(LsssAction action) {
      id = action.getId();
      label = action.getLabel();
      enabled = action.isEnabled();
   }

   @Override
   public String toString() {
      return "ApiLsssAction{" +
            "id='" + id + '\'' +
            ", label='" + label + '\'' +
            ", enabled=" + enabled +
            '}';
   }
}
