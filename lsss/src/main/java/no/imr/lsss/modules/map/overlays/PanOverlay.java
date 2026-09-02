package no.imr.lsss.modules.map.overlays;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.listening.ListenerRegistry;
import no.marec.lsss.api.util.GeoPoint;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public final class PanOverlay extends BaseMapOverlay {
   private static final double PAN_FRACTION = 0.5;
   private static final int MARGIN = 15;

   public PanOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, MapModule mapModule) {
      super(moduleInfo, mapModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(getMapModule().getSizeChangeManager(), createRecomputeListener());
   }

   @Override
   protected OverlayDisplayData recomputeDisplayData() {
      return new DisplayData(getWidth(), getHeight());
   }

   private Direction findDirection(Point2D mousePosition) {
      double x = mousePosition.getX() / getWidth();
      double y = mousePosition.getY() / getHeight();
      boolean upperLeft = x + y < 1;
      if (x > y) { // Upper right
         return upperLeft ? Direction.NORTH : Direction.EAST;
      } else { // Lower left
         return upperLeft ? Direction.WEST : Direction.SOUTH;
      }
   }

   @Override
   public void onActivate() {
      updateCursor();
   }

   private void updateCursor() {
      Point2D mousePosition = getMapModule().getMousePosition();
      if (mousePosition != null) {
         setCursor(findDirection(mousePosition).cursor);
      }
   }

   @Override
   public void mouseMoved(MouseEvent mouseEvent) {
      updateCursor();
   }

   @Override
   public void mouseDragged(MouseEvent mouseEvent) {
      updateCursor();
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      Direction direction = findDirection(mouseEvent.getPoint());
      Rectangle2D geoRect = getMapModule().getGeoRect();
      double x = geoRect.getCenterX() + direction.x * PAN_FRACTION * geoRect.getWidth();
      double y = geoRect.getCenterY() + direction.y * PAN_FRACTION * geoRect.getHeight();
      getMapModule().setGeoCenter(new GeoPoint(x, y));
   }

   private record DisplayData(int width, int height) implements OverlayDisplayData {
      @Override
      public boolean intersects(Rectangle2D rectangle) {
         double x = rectangle.getCenterX();
         double y = rectangle.getCenterY();
         return x < MARGIN || x > width - MARGIN ||
               y < MARGIN || y > height - MARGIN;
      }
   }

   private enum Direction {
      EAST(LsssCursors.FORWARD, 1, 0),
      WEST(LsssCursors.BACK, -1, 0),
      NORTH(LsssCursors.UP, 0, 1),
      SOUTH(LsssCursors.DOWN, 0, -1);

      private final Cursor cursor;
      private final int x;
      private final int y;

      Direction(Cursor cursor, int x, int y) {
         this.cursor = cursor;
         this.x = x;
         this.y = y;
      }
   }
}
