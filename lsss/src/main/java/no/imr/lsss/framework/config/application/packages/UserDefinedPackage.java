package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.packages.pojo.ActionReference;
import no.imr.lsss.framework.config.application.packages.pojo.MenuItemInfo;
import no.imr.lsss.framework.config.application.packages.pojo.UiItemInfo;
import no.imr.lsss.framework.config.application.packages.pojo.UserDefinedPackageInfo;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.ActionExecutor;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.LsssPackage;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.swing.svg.SvgIcon;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;

import javax.swing.KeyStroke;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class UserDefinedPackage {
   static final String PACKAGE_INFO_JSON = "package-info.json";

   private final PackagesConf packagesConf;

   public String id;
   public final UserDefinedPackageInfo info;
   private final List<UserDefinedAction> actions = new ArrayList<>();

   private LsssPackage lsssPackage;

   UserDefinedPackage(PackagesConf packagesConf) {
      this.packagesConf = packagesConf;
      id = "";
      info = new UserDefinedPackageInfo();
      lsssPackage = new LsssPackage(id, this);
   }

   UserDefinedPackage(PackagesConf packagesConf, Path packageDir, UserDefinedPackageInfo info) {
      this.packagesConf = packagesConf;
      id = packageDir.getFileName().toString();
      this.info = info;
      lsssPackage = new LsssPackage(id, this);

      actions.addAll(loadActions());
      actions.forEach(UserDefinedAction::install);
   }

   public LsssPackage getLsssPackage() {
      return lsssPackage;
   }

   public PackagesConf getPackagesConf() {
      return packagesConf;
   }

   LSSS getLSSS() {
      return packagesConf.getLSSS();
   }

   public String getEffectiveLabel() {
      return info.label.isBlank() ? id : info.label;
   }

   public Path getDir() {
      return packagesConf.getPackagesDir().resolve(id);
   }

   public Path getActionsDir() {
      return getDir().resolve("actions");
   }

   public List<UserDefinedAction> getActions() {
      return actions;
   }

   void addUserDefinedAction(UserDefinedAction userDefinedAction) {
      userDefinedAction.install();
      actions.add(userDefinedAction);
      packagesConf.getChangeManager().notifyListeners();
   }

   public void deleteUserDefinedAction(UserDefinedAction userDefinedAction) {
      userDefinedAction.uninstall();
      actions.remove(userDefinedAction);
      Path dir = getActionsDir().resolve(userDefinedAction.id);
      try {
         FileUtils.deleteRecursively(dir);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error deleting " + dir, e);
         packagesConf.reloadPackages();
         return;
      }
      packagesConf.getChangeManager().notifyListeners();
   }

   public Optional<SvgIcon> uiInfoToIcon(ActionReference actionReference) {
      LsssAction action = uiInfoToLsssAction(actionReference);
      return action != null ? action.getIcon() : Optional.empty();
   }

   public String uiInfoToEffectiveText(UiItemInfo info) {
      if (!info.text.isEmpty()) {
         return info.text;
      }
      LsssAction action = uiInfoToLsssAction(info);
      return action != null ? action.getLabel() : info.actionId;
   }

   public String uiInfoToPackageId(ActionReference actionReference) {
      String packageId = actionReference.packageId;
      return packageId.isEmpty() ? id : packageId;
   }

   public @Nullable LsssAction uiInfoToLsssAction(ActionReference actionReference) {
      LsssPackage lsssPackage = getLSSS().getPackageManager().getPackage(uiInfoToPackageId(actionReference));
      return lsssPackage != null
            ? lsssPackage.getAction(actionReference.actionId)
            : null;
   }

   public String uiInfoToToolTip(ActionReference actionReference) {
      LsssAction action = uiInfoToLsssAction(actionReference);
      if (action != null) {
         return action.getToolTipText();
      }
      return new HtmlStringBuilder()
            .text(uiInfoToPackageId(actionReference)).text("/").text(actionReference.actionId)
            .html("<div style='color: red;'>Action not found!</div>")
            .build();
   }

   static Stream<MenuItemInfo> uiMenuItemInfos(List<MenuItemInfo> menuItemInfos) {
      return menuItemInfos.stream()
            .flatMap(menuItemInfo -> uiMenuItemInfos(menuItemInfo.items));
   }

   void install() {
      getLSSS().getPackageManager().removePackage(lsssPackage);
      LsssPackage newLsssPackage = new LsssPackage(id, this);
      lsssPackage.getActions().forEach(newLsssPackage::addAction);
      lsssPackage = newLsssPackage;
      getLSSS().getPackageManager().addPackage(lsssPackage);
      update();
   }

   void uninstall() {
      getLSSS().getPackageManager().removePackage(lsssPackage);
      actions.forEach(UserDefinedAction::uninstall);
      actions.clear();
   }

   void update() {
      Map<String, Map<KeyStroke, ActionExecutor>> keyStrokeMap = info.keyStrokes.stream()
            .collect(Collectors.groupingBy(
                  keyStrokeInfo -> keyStrokeInfo.context,
                  Collectors.groupingBy(
                        keyStrokeInfo -> KeyStroke.getKeyStroke(keyStrokeInfo.keyStroke),
                        Collectors.collectingAndThen(Collectors.toList(), this::toActionExecutor))));
      lsssPackage.setKeyStrokeMap(keyStrokeMap);
   }

   private ActionExecutor toActionExecutor(List<? extends ActionReference> actionReferences) {
      return argument -> {
         for (ActionReference actionReference : actionReferences) {
            LsssAction lsssAction = uiInfoToLsssAction(actionReference);
            if (lsssAction != null) {
               lsssAction.runIfEnabled(argument);
            }
         }
      };
   }

   public void runAction(ActionReference actionReference, ActionArgument argument) {
      LsssAction action = uiInfoToLsssAction(actionReference);
      if (action == null) {
         getLSSS().showError("No action with id " + uiInfoToPackageId(actionReference) + '/' + actionReference.actionId);
      } else {
         action.run(argument);
      }
   }

   static @Nullable UserDefinedPackageInfo loadPackageInfo(Path packageDir) {
      Path file = packageDir.resolve(PACKAGE_INFO_JSON);
      try {
         return JsonUtils.JSON_MAPPER.readValue(file, UserDefinedPackageInfo.class);
      } catch (JacksonException e) {
         if (!FileUtils.notExists(e, file)) {
            Log.global.log(Level.WARNING, "Error reading " + file, e);
         }
         return null;
      }
   }

   void savePackageInfo() {
      Path file = getDir().resolve(PACKAGE_INFO_JSON);
      try {
         JsonUtils.writeValuePrettily(file, info);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving " + file, e);
      }
   }

   private List<UserDefinedAction> loadActions() {
      List<FileInfo> fileInfos;
      try {
         fileInfos = FileUtils.listFilesWithAttributes(getActionsDir(), FileInfo::isDirectory);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error listing actions in " + getActionsDir(), e);
         return List.of();
      }
      return fileInfos.stream()
            .map(fileInfo -> new UserDefinedAction(this, fileInfo.file()))
            .toList();
   }
}
