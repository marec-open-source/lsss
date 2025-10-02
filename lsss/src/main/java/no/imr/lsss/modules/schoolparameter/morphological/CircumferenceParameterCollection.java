package no.imr.lsss.modules.schoolparameter.morphological;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolBoundaryObject;
import no.imr.lsss.modules.schoolparameter.SchoolParameter;
import no.imr.tools.Utils;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class CircumferenceParameterCollection implements MorphologicalParameterCollection {
   private static final SchoolParameter CIRCUMFERENCE = new SchoolParameter(new Name("circumference", "Circumference"), Unit.METER);

   public CircumferenceParameterCollection() {
   }

   @Override
   public List<SchoolParameter> getParameters() {
      return List.of(CIRCUMFERENCE);
   }

   @Override
   public Map<String, Float> computeValues(DataFileSet dataFileSet, RegionManager regionManager, School school) {
      return Map.of(CIRCUMFERENCE.getPersistentName(), computeCircumference(school));
   }

   private static float computeCircumference(School school) {
      double circumference = 0;
      for (SchoolBoundaryObject boundaryObject : school.getBoundaryObjects()) {
         DistanceAccumulator distanceAccumulator = new DistanceAccumulator();
         for (EchogramPoint echogramPoint : boundaryObject.getBoundary()) {
            distanceAccumulator.accumulate(echogramPoint);
         }
         distanceAccumulator.finish();
         circumference += distanceAccumulator.getDistance();
      }
      return (float) circumference;
   }

   private static final class DistanceAccumulator {
      private double distance;
      private @Nullable EchogramPoint firstEchogramPoint;
      private @Nullable EchogramPoint lastEchogramPoint;

      private DistanceAccumulator() {
      }

      private void accumulate(EchogramPoint echogramPoint) {
         if (lastEchogramPoint == null) {
            firstEchogramPoint = echogramPoint;
            lastEchogramPoint = echogramPoint;
            return;
         }
         distance += computeDist(echogramPoint, lastEchogramPoint);
         lastEchogramPoint = echogramPoint;
      }

      private void finish() {
         if (firstEchogramPoint == null || lastEchogramPoint == null) {
            return;
         }
         distance += computeDist(lastEchogramPoint, firstEchogramPoint);
      }

      private static double computeDist(EchogramPoint echogramPoint, EchogramPoint lastEchogramPoint) {
         double horDist = Utils.nmiToMeter(echogramPoint.pingIndex().getVesselDistance() - lastEchogramPoint.pingIndex().getVesselDistance());
         double vertDist = echogramPoint.depth() - lastEchogramPoint.depth();
         return Utils.hypot(horDist, vertDist);
      }

      private double getDistance() {
         return distance;
      }
   }
}
