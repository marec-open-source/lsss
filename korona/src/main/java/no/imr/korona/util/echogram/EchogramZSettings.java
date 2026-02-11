package no.imr.korona.util.echogram;

import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.FloatRange;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.awt.geom.Point2D;

/**
 * The vertical configuration of an echogram.
 */
public abstract class EchogramZSettings {
   public static final float MIN_DELTA_Z = 0.1f;

   public final FloatParameter minZ = new FloatParameter(
         new Name("MinZ", "Min z"),
         0, Unit.METER,
         "Minimum z");

   public final FloatParameter maxZ = new FloatParameter(
         new Name("MaxZ", "Max z"),
         MIN_DELTA_Z, Unit.METER,
         "Maximum z");

   public final FloatParameter minZoomedZ = new FloatParameter(
         new Name("MinZoomedZ", "Min zoomed z"),
         0, Unit.METER, ValueConstraints.gteLte(0f, 0f),
         "Current minimum z");

   public final FloatParameter maxZoomedZ = new FloatParameter(
         new Name("MaxZoomedZ", "Max zoomed z"),
         MIN_DELTA_Z, Unit.METER, ValueConstraints.gteLte(MIN_DELTA_Z, MIN_DELTA_Z),
         "Current maximum z");

   private int height;
   private float zToYFactor;
   private boolean skipNotify;
   private final ChangeManager zoomedChangeManager = new ChangeManager();

   protected EchogramZSettings() {
      minZ.subscribe(z -> {
         maxZ.setAtLeastTo(z + MIN_DELTA_Z);
         updateAllowedZoomedZRange();
      });
      maxZ.subscribe(z -> {
         minZ.setAtMostTo(z - MIN_DELTA_Z);
         updateAllowedZoomedZRange();
      });
      minZoomedZ.subscribe(zoomedZ -> {
         float z = Math.max(maxZoomedZ.getFloatValue(), zoomedZ + MIN_DELTA_Z);
         if (maxZoomedZ.getConstraint().isValid(z)) {
            maxZoomedZ.setFloatValue(z);
         }
         zoomedZRangeChanged();
      });
      maxZoomedZ.subscribe(zoomedZ -> {
         float z = Math.min(minZoomedZ.getFloatValue(), zoomedZ - MIN_DELTA_Z);
         if (minZoomedZ.getConstraint().isValid(z)) {
            minZoomedZ.setFloatValue(z);
         }
         zoomedZRangeChanged();
      });
   }

   public abstract DepthTransform getDepthTransform();

   public ChangeManager getZoomedChangeManager() {
      return zoomedChangeManager;
   }

   private void updateAllowedZoomedZRange() {
      minZoomedZ.setRange(minZ.getFloatValue(), maxZ.getFloatValue() - MIN_DELTA_Z);
      maxZoomedZ.setRange(minZ.getFloatValue() + MIN_DELTA_Z, maxZ.getFloatValue());
   }

   public int getHeight() {
      return height;
   }

   public void setHeight(int height) {
      this.height = height;
      zoomedZRangeChanged();
   }

   private void updateZToYFactor() {
      zToYFactor = height / (maxZoomedZ.getFloatValue() - minZoomedZ.getFloatValue());
   }

   public FloatRange getMaxZRange() {
      return FloatRange.of(minZ.getFloatValue(), maxZ.getFloatValue());
   }

   public void setMaxZ(FloatRange zRange) {
      setMaxZ(zRange.min(), zRange.max());
   }

   public void setMaxZ(float zMin, float zMax) {
      minZ.setFloatValue(zMin);
      maxZ.setFloatValue(zMax);
   }

   public float getMinZoomedZ() {
      return minZoomedZ.getFloatValue();
   }

   public float getMaxZoomedZ() {
      return maxZoomedZ.getFloatValue();
   }

   public FloatRange getZoomedZRange() {
      return FloatRange.of(minZoomedZ.getFloatValue(), maxZoomedZ.getFloatValue());
   }

   public void setZ(FloatRange zRange) {
      setZ(zRange.min(), zRange.max());
   }

   public void setZ(float zMin, float zMax) {
      zMin = Math.clamp(zMin, minZ.getFloatValue(), maxZ.getFloatValue() - MIN_DELTA_Z);
      zMax = Math.clamp(zMax, minZ.getFloatValue() + MIN_DELTA_Z, maxZ.getFloatValue());

      FloatRange currentZoom = getZoomedZRange();

      skipNotify = true;
      try {
         minZoomedZ.setFloatValue(zMin);
         maxZoomedZ.setFloatValue(zMax);
      } finally {
         skipNotify = false;
      }

      if (!getZoomedZRange().equals(currentZoom)) {
         zoomedZRangeChanged();
      }
   }

   private void zoomedZRangeChanged() {
      if (skipNotify) {
         return;
      }
      updateZToYFactor();
      zoomedChangeManager.notifyListeners();
   }

   public boolean isZoomedVertically() {
      return !getMaxZRange().equals(getZoomedZRange());
   }

   public void zoomOut() {
      setZ(getMaxZRange());
   }

   public void zoom(Point2D point, double zoomFactor) {
      zoom(yToZ(point.getY()), zoomFactor);
   }

   public void zoom(float referenceZ, double zoomFactor) {
      float size = getMaxZoomedZ() - getMinZoomedZ();
      float newSize = (float) (size / zoomFactor);
      float fraction = (referenceZ - getMinZoomedZ()) / size;
      setZ(referenceZ - newSize * fraction, referenceZ + newSize * (1 - fraction));
   }

   public void zoom(Point2D p1, Point2D p2) {
      float z1 = yToZ(p1.getY());
      float z2 = yToZ(p2.getY());
      setZ(Math.min(z1, z2), Math.max(z1, z2));
   }

   public void stepUp() {
      shift(-1);
   }

   public void stepDown() {
      shift(1);
   }

   public void shift(float shift) {
      FloatRange shiftedRange = getZoomedZRange().shift(shift);
      setZ(shiftedRange.shiftToBeContainedIn(getMaxZRange()));
   }

   public FloatRange getDepthRange(PingIndex pingIndex) {
      return getDepthTransform().zToDepth(getZoomedZRange(), pingIndex);
   }

   public int zToYIndex(float z) {
      float y = zToY(z);
      return Math.round(y);
   }

   public float zToY(float z) {
      return (z - minZoomedZ.getFloatValue()) * zToYFactor;
   }

   public float zToDepth(double z, PingIndex pingIndex) {
      return getDepthTransform().zToDepth((float) z, pingIndex);
   }

   public float yToZ(double y) {
      return (float) (minZoomedZ.getFloatValue() + y / zToYFactor);
   }

   public float yToDepth(double y, PingIndex pingIndex) {
      float z = yToZ(y);
      return zToDepth(z, pingIndex);
   }

   public float yToClampedDepth(double y, PingIndex pingIndex) {
      float depth = yToDepth(y, pingIndex);
      return getDepthRange(pingIndex).clamp(depth);
   }

   public float depthToZ(float depth, PingIndex pingIndex) {
      return getDepthTransform().depthToZ(depth, pingIndex);
   }

   public float depthToY(float depth, PingIndex pingIndex) {
      float z = depthToZ(depth, pingIndex);
      return zToY(z);
   }

   public int depthToYIndex(float depth, PingIndex pingIndex) {
      float z = depthToZ(depth, pingIndex);
      return zToYIndex(z);
   }
}
