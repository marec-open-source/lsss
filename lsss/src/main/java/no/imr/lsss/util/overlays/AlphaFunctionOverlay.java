package no.imr.lsss.util.overlays;

import no.imr.korona.viewer.AlphaFunction;
import no.imr.korona.viewer.TransferFunction;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.lsss.resources.LsssCursors;
import no.imr.tools.Utils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.imr.tools.swing.overlay.Overlay;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

/**
 * An overlay showing the alpha curve.
 */
public final class AlphaFunctionOverlay extends Overlay {
   private final TransferFunction transferFunction;
   private final AlphaFunction alphaFunction;
   private final ArgChangeManager<Boolean> modifyingChangeManager = new ArgChangeManager<>();
   private final Path2D.Float curve = new Path2D.Float();
   private Mode mode = Mode.DRAW;
   private @Nullable Point point;

   public AlphaFunctionOverlay(TransferFunction transferFunction) {
      this.transferFunction = transferFunction;
      alphaFunction = transferFunction.getAlphaFunction();
      alphaFunction.getChangeManager().addListener(this::repaint);
   }

   public ArgChangeManager<Boolean> getModifyingChangeManager() {
      return modifyingChangeManager;
   }

   @Override
   public boolean overlaps(Rectangle2D rectangle2D) {
      return GuiUtils.intersects(curve, rectangle2D);
   }

   private void updateCursor() {
      setCursor(getCursor());
   }

   @Override
   public Cursor getCursor() {
      return switch (mode) {
         case DRAW -> LsssCursors.EDIT;
         case MOVE -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR);
      };
   }

   @Override
   public void draw(Graphics2D g2d) {
      updateCurve();

      g2d.setColor(Color.RED);
      g2d.draw(curve);
   }

   private void updateCurve() {
      curve.reset();

      LineStripBuilder curveBuilder = LineStripBuilders.coalescing(curve);

      int height = getHeight();
      int width = getWidth();

      SingleValueColorConverter singleValueConverter = (SingleValueColorConverter) transferFunction.getColorConverter();
      float delta = singleValueConverter.getDeltaValue(height);
      float value = singleValueConverter.getContinuousVariable().getSettings().getMaxRange().max();

      float alphaFactor = alphaFunction.getMaxAlpha();

      for (int y = 0; y < height; y++, value -= delta) {
         float scaledAlpha = alphaFunction.getAlpha(value) / alphaFactor;
         float x = scaledAlpha * (width - 1);
         curveBuilder.addPoint(x, y);
      }

      curveBuilder.endLineStrip();
   }

   @Override
   public void mousePressed(MouseEvent e) {
      modifyingChangeManager.notifyListeners(true);
      point = e.getPoint();
      if (mode == Mode.DRAW) {
         draw(point, point);
      }
   }

   @Override
   public void mouseDragged(MouseEvent e) {
      if (point != null) {
         Point nextPoint = e.getPoint();

         switch (mode) {
            case DRAW -> draw(point, nextPoint);
            case MOVE -> shift(point, nextPoint);
         }

         alphaFunction.alphaFunctionChanged();

         point = nextPoint;
      }
   }

   private void shift(Point p0, Point p1) {
      float value0 = yToValue(p0.y);
      float value1 = yToValue(p1.y);

      alphaFunction.shift(value0, value1);
   }

   private void draw(Point p0, Point p1) {
      float value0 = yToValue(p0.y);
      float value1 = yToValue(p1.y);

      float alpha0 = xToAlpha(p0.x);
      float alpha1 = xToAlpha(p1.x);

      alphaFunction.draw(value0, alpha0, value1, alpha1);
      alphaFunction.alphaFunctionChanged();
   }

   public float yToValue(int y) {
      SingleValueColorConverter singleValueConverter = (SingleValueColorConverter) transferFunction.getColorConverter();
      FloatRange maxRange = singleValueConverter.getContinuousVariable().getSettings().getMaxRange();
      int maxY = getHeight() - 1;
      return maxRange.fractionToValue((float) (maxY - y) / (float) maxY);
   }

   private float xToAlpha(int x) {
      int maxX = getWidth() - 1;
      return alphaFunction.getMaxAlpha() * (float) x / (float) maxX;
   }

   @Override
   public void mouseReleased(MouseEvent e) {
      point = null;
      alphaFunction.alphaFunctionChanged();
      modifyingChangeManager.notifyListeners(false);
   }

   @Override
   public void keyTyped(KeyEvent e) {
      switch (e.getKeyChar()) {
         case ' ' -> {
            mode = Utils.shift(mode, 1);
            updateCursor();
         }
         default -> {
         }
      }
   }

   private enum Mode {
      DRAW, MOVE
   }
}
