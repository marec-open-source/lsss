package no.imr.lsss.util.phantom.echogram;

import no.imr.lsss.modules.echogram.EchogramKeys;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

final class PhantomEchogramKeyListener extends KeyAdapter {
   private final BasePhantomEchogramModule phantomEchogramModule;
   private final PhantomEchogramSettings phantomEchogramSettings;

   PhantomEchogramKeyListener(BasePhantomEchogramModule phantomEchogramModule) {
      this.phantomEchogramModule = phantomEchogramModule;
      phantomEchogramSettings = phantomEchogramModule.getPhantomEchogramSettings();
   }

   @Override
   public void keyPressed(KeyEvent e) {
      switch (e.getKeyCode()) {
         case KeyEvent.VK_TAB -> EchogramKeys.tab(e, phantomEchogramModule);
         case KeyEvent.VK_UP -> EchogramKeys.up(e, phantomEchogramSettings.getEchogramZSettings());
         case KeyEvent.VK_DOWN -> EchogramKeys.down(e, phantomEchogramSettings.getEchogramZSettings());
         case KeyEvent.VK_LEFT -> EchogramKeys.left(e, phantomEchogramModule.getLSSS().getInterpretationSettings());
         case KeyEvent.VK_RIGHT -> EchogramKeys.right(e, phantomEchogramModule.getLSSS().getInterpretationSettings());
         case KeyEvent.VK_HOME -> EchogramKeys.home(e, phantomEchogramModule.getLSSS().getInterpretationSettings(), phantomEchogramSettings.getEchogramZSettings());
         case KeyEvent.VK_PAGE_UP -> {
            if (e.getModifiersEx() == 0) {
               phantomEchogramSettings.shiftChannel(1);
            }
         }
         case KeyEvent.VK_PAGE_DOWN -> {
            if (e.getModifiersEx() == 0) {
               phantomEchogramSettings.shiftChannel(-1);
            }
         }
         case KeyEvent.VK_D -> {
            if (e.getModifiersEx() == 0) {
               phantomEchogramSettings.shiftChannel(-1);
            }
         }
         case KeyEvent.VK_F -> {
            if (e.getModifiersEx() == 0) {
               phantomEchogramSettings.shiftChannel(1);
            }
         }
         default -> {
         }
      }
   }
}
