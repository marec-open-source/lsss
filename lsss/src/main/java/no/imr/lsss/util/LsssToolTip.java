package no.imr.lsss.util;

import no.imr.lsss.LSSS;
import no.imr.tools.listening.Listener;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ToolTipManagerState;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Function;

/**
 * Manages a tooltip for a component that is always showing or hidden
 * depending on {@link no.imr.lsss.framework.packages.Actions#showTooltip}.
 */
public final class LsssToolTip implements Listener {
   private final LSSS lsss;
   private final JComponent component;
   private final Function<Point, @Nullable String> toolTipFunction;
   private @Nullable Point mousePosition;

   public LsssToolTip(LSSS lsss, JComponent component, Function<Point, @Nullable String> toolTipFunction) {
      this.lsss = lsss;
      this.component = component;
      this.toolTipFunction = toolTipFunction;

      lsss.getActions().showTooltip.getChangeManager().addListener(GuiListeners.later(this::updateToolTipManagerState));

      ToolTipMouseListener mouseListener = new ToolTipMouseListener();
      component.addMouseListener(mouseListener);
      component.addMouseMotionListener(mouseListener);
      component.addFocusListener(new ToolTipFocusListener());
   }

   private void updateToolTipManagerState() {
      if (mousePosition == null) {
         // Mouse not inside component => do nothing
         return;
      }

      if (lsss.getActions().showTooltip.get()) {
         ToolTipManagerState.ALWAYS_ON.apply();
         update();
      } else {
         ToolTipManagerState.ALWAYS_OFF.apply();
      }
   }

   private void setMousePosition(MouseEvent e) {
      mousePosition = e.getPoint();
      update();
   }

   public void update() {
      GuiUtils.invokeNowOrLater(this::guiUpdate);
   }

   private void guiUpdate() {
      if (lsss.getActions().showTooltip.get() && mousePosition != null) {
         component.setToolTipText(toolTipFunction.apply(mousePosition));
         ToolTipManagerState.updateToolTip(component, mousePosition);
      }
   }

   @Override
   public void listen() {
      update();
   }

   /**
    * Mouse listener.
    */
   private final class ToolTipMouseListener extends MouseAdapter {
      private ToolTipMouseListener() {
      }

      @Override
      public void mouseEntered(MouseEvent e) {
         setMousePosition(e);
         updateToolTipManagerState();
      }

      @Override
      public void mouseExited(MouseEvent e) {
         mousePosition = null;
         component.setToolTipText(null);
         ToolTipManagerState.updateToolTip(component, e.getPoint());
         ToolTipManagerState.DEFAULT.apply();
      }

      @Override
      public void mouseDragged(MouseEvent e) {
         setMousePosition(e);
      }

      @Override
      public void mouseMoved(MouseEvent e) {
         setMousePosition(e);
      }
   }

   /**
    * See issue #386.
    */
   private final class ToolTipFocusListener extends FocusAdapter {
      private ToolTipFocusListener() {
      }

      @Override
      public void focusGained(FocusEvent e) {
         updateToolTipManagerState();
      }

      @Override
      public void focusLost(FocusEvent e) {
         ToolTipManagerState.DEFAULT.apply();
      }
   }
}
