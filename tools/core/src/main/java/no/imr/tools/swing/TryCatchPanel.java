package no.imr.tools.swing;

import no.imr.tools.logging.Log;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.util.logging.Level;

public final class TryCatchPanel extends JPanel {
   public TryCatchPanel(BorderLayout layout) {
      super(layout);
   }

   @Override
   public void paint(Graphics g) {
      try {
         super.paint(g);
      } catch (Exception e) {
         String info = GuiUtils.getInfoProperty(this);
         Log.global.log(Level.WARNING, "Error painting " + info, e);
      }
   }
}
