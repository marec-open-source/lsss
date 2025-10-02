package no.imr.tools.swing.linestrip;

import no.imr.tools.math.linalg.Vec2;
import no.marec.lsss.api.util.LineStripBuilder;

import java.util.ArrayList;
import java.util.List;

final class ListBuilder implements LineStripBuilder {
   private final List<List<Vec2>> list = new ArrayList<>();
   private List<Vec2> points = new ArrayList<>();

   ListBuilder() {
   }

   List<List<Vec2>> getList() {
      return list;
   }

   @Override
   public boolean isEmpty() {
      return list.isEmpty();
   }

   @Override
   public void addPoint(double x, double y) {
      points.add(new Vec2((float) x, (float) y));
   }

   @Override
   public void endLineStrip() {
      if (!points.isEmpty()) {
         list.add(points);
         points = new ArrayList<>();
      }
   }
}
