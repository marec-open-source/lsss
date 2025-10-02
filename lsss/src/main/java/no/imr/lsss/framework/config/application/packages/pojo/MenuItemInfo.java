package no.imr.lsss.framework.config.application.packages.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class MenuItemInfo extends UiItemInfo {
   public @Nullable Character mnemonic;

   @JsonInclude(JsonInclude.Include.NON_EMPTY)
   public List<MenuItemInfo> items = new ArrayList<>();

   public MenuItemInfo() {
   }

   @JsonIgnore
   public boolean isMenu() {
      return actionId.isEmpty();
   }

   @Override
   public String toString() {
      return "MenuItemInfo{" +
            "packageId='" + packageId + '\'' +
            ", actionId='" + actionId + '\'' +
            ", text='" + text + '\'' +
            '}';
   }
}
