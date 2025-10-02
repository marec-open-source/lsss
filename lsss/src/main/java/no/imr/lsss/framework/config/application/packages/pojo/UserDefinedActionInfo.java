package no.imr.lsss.framework.config.application.packages.pojo;

import java.util.ArrayList;
import java.util.List;

public final class UserDefinedActionInfo {
   public String label = "";
   public String tooltip = "";
   public String icon = "";
   public boolean showDialog = true;
   public List<UserDefinedInputParameter> inputParameters = new ArrayList<>();

   public UserDefinedActionInfo() {
   }

   @Override
   public String toString() {
      return "UserDefinedActionInfo{" +
            "label='" + label + '\'' +
            ", tooltip='" + tooltip + '\'' +
            ", icon='" + icon + '\'' +
            ", showDialog=" + showDialog +
            '}';
   }
}
