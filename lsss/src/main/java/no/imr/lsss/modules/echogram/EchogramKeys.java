package no.imr.lsss.modules.echogram;

import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.modules.BaseOverlaidModule;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public final class EchogramKeys extends KeyAdapter {
   private EchogramKeys() {
   }

   public static void tab(KeyEvent e, BaseOverlaidModule<?> module) {
      if (!module.isAnyMouseButtonPressed()) {
         int step;
         if (e.getModifiersEx() == 0) {
            step = 1;
         } else if (e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
            step = -1;
         } else {
            return;
         }
         module.getLSSS().getInterpretationSettings().getEchogramSettings().workingMode.shiftValue(step);
      }
   }

   public static void up(KeyEvent e, EchogramZSettings zSettings) {
      if (e.getModifiersEx() == 0) {
         zSettings.stepUp();
      } else {
         zSettings.shift((float) -getShiftAmount(e));
      }
   }

   public static void down(KeyEvent e, EchogramZSettings zSettings) {
      if (e.getModifiersEx() == 0) {
         zSettings.stepDown();
      } else {
         zSettings.shift((float) getShiftAmount(e));
      }
   }

   public static void left(KeyEvent e, InterpretationSettings interpretationSettings) {
      if (e.getModifiersEx() == 0) {
         interpretationSettings.gotoPreviousPingRange();
      } else {
         interpretationSettings.shiftPingRange(-getShiftAmount(e));
      }
   }

   public static void right(KeyEvent e, InterpretationSettings interpretationSettings) {
      if (e.getModifiersEx() == 0) {
         interpretationSettings.gotoNextPingRange();
      } else {
         interpretationSettings.shiftPingRange(getShiftAmount(e));
      }
   }

   public static void home(KeyEvent e, InterpretationSettings interpretationSettings, EchogramZSettings zSettings) {
      if (e.getModifiersEx() == 0) {
         interpretationSettings.getNavigationHistory().zoomOut();
      } else if (e.getModifiersEx() == KeyEvent.SHIFT_DOWN_MASK) {
         zSettings.zoomOut();
      }
   }

   private static double getShiftAmount(KeyEvent keyEvent) {
      return switch (keyEvent.getModifiersEx()) {
         case KeyEvent.SHIFT_DOWN_MASK -> 0.5;
         case KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK -> 0.1;
         case KeyEvent.ALT_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK -> 0.01;
         default -> 0;
      };
   }
}
