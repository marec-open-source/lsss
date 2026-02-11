package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.packages.pojo.UserDefinedActionInfo;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.TaskLsssAction;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.JsonUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.swing.svg.SvgImage;
import org.jspecify.annotations.Nullable;
import tools.jackson.core.JacksonException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

public final class UserDefinedAction {
   private static final String INFO_JSON = "info.json";
   static final String MAIN_PY = "main.py";

   private final UserDefinedPackage userDefinedPackage;

   public String id;
   public final UserDefinedActionInfo info;
   private @Nullable SvgIcon icon;
   private @Nullable LsssAction lsssAction;

   UserDefinedAction(UserDefinedPackage userDefinedPackage) {
      this.userDefinedPackage = userDefinedPackage;
      id = "";
      info = new UserDefinedActionInfo();
   }

   UserDefinedAction(UserDefinedPackage userDefinedPackage, Path actionDir) {
      this.userDefinedPackage = userDefinedPackage;
      id = actionDir.getFileName().toString();
      info = loadActionInfo(actionDir);
   }

   private static UserDefinedActionInfo loadActionInfo(Path actionDir) {
      Path file = actionDir.resolve(INFO_JSON);
      try {
         return JsonUtils.JSON_MAPPER.readValue(file, UserDefinedActionInfo.class);
      } catch (JacksonException e) {
         if (Files.exists(file)) {
            Log.global.log(Level.WARNING, "Error reading " + file, e);
         }
         return new UserDefinedActionInfo();
      }
   }

   void saveActionInfo() {
      Path dir = getDir();
      Path file = dir.resolve(INFO_JSON);
      try {
         JsonUtils.writeValuePrettily(file, info);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving " + dir, e);
      }
   }

   UserDefinedPackage getUserDefinedPackage() {
      return userDefinedPackage;
   }

   private LSSS getLSSS() {
      return userDefinedPackage.getPackagesConf().getLSSS();
   }

   String getTotalId() {
      return userDefinedPackage.id + "/" + id;
   }

   String getEffectiveLabel() {
      return info.label.isBlank() ? id : info.label;
   }

   String getEffectiveTooltip() {
      return UserDefinedUtils.combinedText(getTotalId(), info.tooltip);
   }

   public Path getDir() {
      return userDefinedPackage.getActionsDir().resolve(id);
   }

   Optional<SvgIcon> getIcon() {
      if (icon == null && !info.icon.isEmpty()) {
         icon = createIcon(getDir(), info.icon);
      }
      return Optional.ofNullable(icon);
   }

   static SvgIcon createIcon(Path dir, String path) {
      return new SvgIcon(new SvgImage(dir.resolve(FileUtils.toNativeSeparatorChar(path)), 16));
   }

   Path getMainFile() {
      if (id.isEmpty()) {
         throw new IllegalStateException("No id");
      }
      return getDir().resolve(MAIN_PY);
   }

   void uninstall() {
      icon = null;
      if (lsssAction != null) {
         userDefinedPackage.getLsssPackage().removeAction(lsssAction);
         lsssAction = null;
      }
   }

   void install() {
      uninstall();
      lsssAction = new TaskLsssAction(id, getEffectiveLabel(), this::execute)
            .setToolTipText(getEffectiveTooltip())
            .setIcon(getIcon().orElse(null));
      userDefinedPackage.getLsssPackage().addAction(lsssAction);
   }

   void execute(ActionArgument argument) {
      if (getLSSS().getInterpretationSettings().isInteractiveMode() && argument.input().isEmpty() && !info.inputParameters.isEmpty()) {
         Optional<Map<String, Object>> input = GuiUtils.getNowOrWait(() -> {
            return UserDefinedUtils.showInputDialog(this, info.inputParameters, getLSSS().getReferenceComponent());
         });
         if (input.isEmpty()) {
            return;
         }
         argument = new ActionArgument(argument.modifiers(), input.get());
      }
      userDefinedPackage.getPackagesConf().executeActionScript(getLSSS()::getReferenceComponent, this, argument, info.showDialog);
   }
}
