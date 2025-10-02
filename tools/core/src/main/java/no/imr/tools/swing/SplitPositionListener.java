package no.imr.tools.swing;

import org.jspecify.annotations.Nullable;

import javax.swing.JSplitPane;
import java.awt.Component;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerAdapter;
import java.awt.event.ContainerEvent;
import java.awt.event.HierarchyEvent;
import java.util.function.IntConsumer;

public final class SplitPositionListener {
   private final JSplitPane splitPane;
   private final IntConsumer dividerLocationListener;
   private final ComponentListener sizeListener = new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
         checkDividerLocation();
      }
   };
   private @Nullable Component leftComponent;
   private int dividerLocation = -1;

   public SplitPositionListener(JSplitPane splitPane, IntConsumer dividerLocationListener) {
      this.splitPane = splitPane;
      this.dividerLocationListener = dividerLocationListener;

      updateListener();

      splitPane.addContainerListener(new ContainerAdapter() {
         @Override
         public void componentAdded(ContainerEvent e) {
            updateListener();
         }

         @Override
         public void componentRemoved(ContainerEvent e) {
            updateListener();
         }
      });

      splitPane.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            updateListener();
         }
      });
   }

   private void updateListener() {
      if (!splitPane.isShowing()) {
         removeListener();
         return;
      }

      Component newLeftComponent = splitPane.getLeftComponent();
      if (leftComponent != newLeftComponent) {
         removeListener();
         leftComponent = newLeftComponent;
         if (leftComponent != null) {
            newLeftComponent.addComponentListener(sizeListener);
         }
      }
   }

   private void removeListener() {
      if (leftComponent != null) {
         leftComponent.removeComponentListener(sizeListener);
         leftComponent = null;
      }
   }

   private void checkDividerLocation() {
      int newDividerLocation = splitPane.getDividerLocation();
      if (dividerLocation != newDividerLocation) {
         dividerLocation = newDividerLocation;
         dividerLocationListener.accept(newDividerLocation);
      }
   }
}
