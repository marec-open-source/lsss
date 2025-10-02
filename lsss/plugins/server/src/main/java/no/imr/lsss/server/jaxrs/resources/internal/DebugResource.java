package no.imr.lsss.server.jaxrs.resources.internal;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.data.ping.ReloadablePing;
import no.imr.lsss.LSSS;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.server.pojo.internal.DebugData;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.ConcurrentObject;

import javax.swing.JDialog;
import javax.swing.JFrame;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;

public final class DebugResource {
   private final LSSS lsss;

   DebugResource(LSSS lsss) {
      this.lsss = lsss;
   }

   @GET
   @Path("data")
   @Produces(MediaType.APPLICATION_JSON)
   public DebugData getDebugData() {
      DebugData debugData = new DebugData();

      debugData.reloadedPings = ReloadablePing.RELOAD_COUNTER.get();

      debugData.availablePings = Utils.getAllOfType(lsss.getInterpretationSettings().getPingSampler().getAvailablePings(), ReloadablePing.class)
            .filter(ping -> ping.getAvailablePingData() != null)
            .count();

      debugData.enabledModules = lsss.getModuleManager().getModules().stream()
            .filter(ConcurrentObject::isEnabled)
            .count();

      debugData.windows = Arrays.stream(Window.getWindows())
            .map(DebugResource::getWindowTitle)
            .toList();

      for (BaseLsssModule module : lsss.getModuleManager().getModules()) {
         if (module instanceof BaseOverlaidModule<?> baseOverlaidModule) {
            List<String> overlaysWithData = baseOverlaidModule.getOverlays().stream()
                  .filter(overlay -> overlay.getDisplayData() != null)
                  .map(BaseLsssModule::getPersistentName)
                  .sorted()
                  .toList();
            debugData.overlaysWithData.put(baseOverlaidModule.getPersistentName(), overlaysWithData);
         }
      }

      return debugData;
   }

   private static String getWindowTitle(Window window) {
      return switch (window) {
         case JFrame frame -> "Frame:  " + frame.getTitle();
         case JDialog dialog -> "Dialog: " + dialog.getTitle();
         default -> window.toString();
      };
   }
}
