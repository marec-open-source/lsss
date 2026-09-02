package no.imr.lsss.modules.misc;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

public final class MagnifierModule extends BaseViewModule {
   private final FloatParameter scale = new FloatParameter(new Name("Scale"),
         6, Unit.NONE,
         "Scale of magnification");

   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));
   private final AWTEventListener mouseMotionListener = this::onMouseMove;
   private final AWTEventListener activityListener = this::onActivity;

   public MagnifierModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            scale
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      SwingUtilities.invokeLater(() -> {
         Toolkit.getDefaultToolkit().addAWTEventListener(mouseMotionListener,
               AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
         Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
               AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
      });
   }

   @Override
   protected void onDisable() {
      SwingUtilities.invokeLater(() -> {
         Toolkit.getDefaultToolkit().removeAWTEventListener(mouseMotionListener);
         Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
      });
      viewHolder.removeView();
   }

   private void onMouseMove(AWTEvent event) {
      Component component = (Component) event.getSource();
      MouseEvent mouseEvent = (MouseEvent) event;
      Component topmostParent = mouseEvent.getID() != MouseEvent.MOUSE_EXITED
            ? GuiUtils.getTopmostParent(component)
            : null;
      Point point = topmostParent != null
            ? SwingUtilities.convertPoint(component, mouseEvent.getPoint(), topmostParent)
            : null;
      viewHolder.ifViewDelayed(mouseMotionListener, view -> view.setCurrent(topmostParent, point));
   }

   private void onActivity(AWTEvent event) {
      viewHolder.ifViewDelayed(activityListener, View::updateImage);
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseView {
      private final MagnifierModule module;
      private final JComponent component = new JComponent() {
         @Override
         protected void paintComponent(Graphics g) {
            ((Graphics2D) g).drawImage(image, null, 0, 0);
         }
      };
      private BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
      private @Nullable Component currentComponent;
      private @Nullable Point currentPoint;

      private View(MagnifierModule module) {
         super(module);

         this.module = module;
         component.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
               int width = Math.max(1, component.getWidth());
               int height = Math.max(1, component.getHeight());
               image = component.getGraphicsConfiguration().createCompatibleImage(width, height);
               updateImage();
            }
         });
      }

      @Override
      public JComponent getComponent() {
         return component;
      }

      private void setCurrent(@Nullable Component currentComponent, @Nullable Point currentPoint) {
         this.currentComponent = currentComponent;
         this.currentPoint = currentPoint;
         updateImage();
      }

      private void updateImage() {
         Graphics2D g = image.createGraphics();
         g.setClip(0, 0, image.getWidth(), image.getHeight());
         g.setBackground(Color.WHITE);
         g.clearRect(0, 0, image.getWidth(), image.getHeight());
         double scale = module.scale.getFloatValue();
         if (currentComponent != null && currentPoint != null && Math.abs(scale) > 1e-6) {
            g.translate(image.getWidth() / 2.0, image.getHeight() / 2.0);
            g.scale(scale, scale);
            g.translate(-currentPoint.x, -currentPoint.y);
            currentComponent.paint(g);
         }
         g.dispose();
         component.repaint();
      }
   }
}
