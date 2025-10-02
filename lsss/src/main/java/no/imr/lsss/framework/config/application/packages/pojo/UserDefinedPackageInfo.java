package no.imr.lsss.framework.config.application.packages.pojo;

import java.util.ArrayList;
import java.util.List;

public final class UserDefinedPackageInfo {
   public String label = "";
   public String version = "";
   public String description = "";
   public List<CallbackInfo> callbacks = new ArrayList<>();
   public List<KeyStrokeInfo> keyStrokes = new ArrayList<>();
   public List<ToolbarButtonInfo> toolbarButtons = new ArrayList<>();
   public List<MenuItemInfo> menus = new ArrayList<>();

   public UserDefinedPackageInfo() {
   }

   @Override
   public String toString() {
      return "UserDefinedPackageInfo{" +
            "label='" + label + '\'' +
            ", version='" + version + '\'' +
            ", description='" + description + '\'' +
            '}';
   }
}
