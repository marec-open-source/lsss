package no.imr.tools.plot;

import no.imr.tools.math.linalg.Vec2;
import no.imr.tools.swing.GuiUtils;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jspecify.annotations.Nullable;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * A graph as a collection of 2D points.
 */
public final class Graph {
   private static final Supplier<XYItemRenderer> DEFAULT_RENDERER = () -> PlotUtils.newStandardXYItemRenderer(StandardXYItemRenderer.LINES);

   private final String name;
   private List<Vec2> points;

   private @Nullable Color color;
   private Supplier<XYItemRenderer> renderer = DEFAULT_RENDERER;
   private BasicStroke stroke = GuiUtils.STROKE_1;
   private @Nullable XYInfo xyInfo;

   public Graph() {
      this("", 0);
   }

   public Graph(String name) {
      this(name, 0);
   }

   public Graph(String name, int initialCapacity) {
      this.name = name;
      points = new ArrayList<>(initialCapacity);
   }

   public String getName() {
      return name;
   }

   public @Nullable Color getColor() {
      return color;
   }

   public Graph setColor(Color color) {
      this.color = color;
      return this;
   }

   public Supplier<XYItemRenderer> getRenderer() {
      return renderer;
   }

   public Graph setRenderer(Supplier<XYItemRenderer> renderer) {
      this.renderer = renderer;
      return this;
   }

   public Stroke getStroke() {
      return stroke;
   }

   public Graph setStroke(BasicStroke stroke) {
      this.stroke = stroke;
      return this;
   }

   public Graph setDashed() {
      stroke = new BasicStroke(stroke.getLineWidth(), stroke.getEndCap(),
            stroke.getLineJoin(), stroke.getMiterLimit(),
            new float[]{5f, 5f}, stroke.getDashPhase());
      return this;
   }

   public Graph setLineWidth(float lineWidth) {
      stroke = new BasicStroke(lineWidth, stroke.getEndCap(),
            stroke.getLineJoin(), stroke.getMiterLimit(),
            stroke.getDashArray(), stroke.getDashPhase());
      return this;
   }

   public @Nullable XYInfo getXYInfo() {
      return xyInfo;
   }

   public Graph setXYInfo(@Nullable XYInfo xyInfo) {
      this.xyInfo = xyInfo;
      return this;
   }

   public List<Vec2> getPoints() {
      return points;
   }

   /**
    * Adds a separator for plotting several disjoint lines.
    */
   public void addSeparator() {
      if (!points.isEmpty()) {
         points.add(new Vec2(Float.NaN, Float.NaN));
      }
   }

   public void addPoint(float x, float y) {
      addPoint(new Vec2(x, y));
   }

   public void addPoint(double x, double y) {
      addPoint(new Vec2((float) x, (float) y));
   }

   public void addPoint(Vec2 point) {
      points.add(point);
   }

   public void addAll(Collection<Vec2> points) {
      this.points.addAll(points);
   }

   public void sortPointsByX() {
      points.sort((p1, p2) -> Float.compare(p1.x(), p2.x()));
   }

   public void smooth() {
      int n = points.size();
      List<Vec2> newPoints = new ArrayList<>(n);
      newPoints.add(points.getFirst());
      for (int i = 1; i < n - 1; i++) {
         float y = (points.get(i - 1).y() + 2 * points.get(i).y() + points.get(i + 1).y()) / 4;
         newPoints.add(new Vec2(points.get(i).x(), y));
      }
      newPoints.add(points.getLast());
      points = newPoints;
   }

   @Override
   public String toString() {
      return name;
   }
}
