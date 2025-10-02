package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.logging.Log;

import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.Component;
import java.net.URL;

public final class StandardHyperlinkListener implements HyperlinkListener {
   public StandardHyperlinkListener() {
   }

   @Override
   public void hyperlinkUpdate(HyperlinkEvent e) {
      Component referenceComponent = e.getSource() instanceof Component c ? c : null;

      if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         URL url = e.getURL();
         switch (url.getProtocol()) {
            case "mailto" -> {
               GuiUtils.desktopMail(Utils.toURI(url), referenceComponent);
            }
            case "file", "http", "https" -> {
               GuiUtils.desktopBrowse(Utils.toURI(url), referenceComponent);
            }
            default -> {
               Log.global.warning("Unhandled protocol: " + url.getProtocol());
            }
         }
      }
   }
}
