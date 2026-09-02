package no.imr.tools.adm;

import no.imr.tools.logging.LoggingManager;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenu;
import java.awt.Component;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface AdmService {
   AdmService INSTANCE = ServiceLoader.load(AdmService.class)
         .findFirst()
         .orElseGet(() -> new AdmService() {
         });

   default void addUpdateLicenseMenuItem(JMenu menu) {
   }

   default void addCheckForUpdateMenuItem(JMenu menu, ApplicationInfo applicationInfo) {
   }

   default List<JComponent> errorHandlerButtons(LoggingManager loggingManager, JFrame frame) {
      return List.of();
   }

   default void addToInfoPanel(Consumer<Component> container, ApplicationInfo applicationInfo) {
   }

   default AppEventHandler sendToServerAppEventHandler(Path file) {
      return AppEventHandler.ignore();
   }

   default @Nullable LicenseInfo getLicenseInfo() {
      return null;
   }

   default void smokeTest() {
   }
}
