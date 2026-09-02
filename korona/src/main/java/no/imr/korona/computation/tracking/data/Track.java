package no.imr.korona.computation.tracking.data;

import no.imr.korona.computation.tracking.PositionFunction;
import no.imr.korona.computation.tracking.TrackIdGenerator;
import no.imr.korona.data.ping.Ping;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.range.FloatRange;

import java.util.ArrayList;
import java.util.List;

public final class Track {
   private final int id;
   private final List<TrackPoint> points = new ArrayList<>();

   public Track(TrackIdGenerator trackIdGenerator, Ping ping, PositionFunction positionFunction, Measurement measurement, FloatRange rangeRange, int sampleCount) {
      id = trackIdGenerator.nextId(ping, measurement.range());
      StateVector stateVector = new StateVector(positionFunction.toGlobalPosition(measurement), Vec3.ZERO, measurement.tsc());
      TrackPoint trackPoint = new TrackPoint(ping.getPingIndex(), new TargetPoint(stateVector, positionFunction));
      trackPoint.setEstimate(trackPoint.getPrediction());
      trackPoint.setMeasurement(measurement);
      trackPoint.setRangeRange(rangeRange);
      trackPoint.setSampleCount(sampleCount);
      points.add(trackPoint);
   }

   @Override
   public String toString() {
      return "{" + id + ", " + points.size() + "}";
   }

   public int getId() {
      return id;
   }

   public List<TrackPoint> getPoints() {
      return points;
   }

   public TrackPoint getLastPoint() {
      return points.getLast();
   }

   public TrackPoint getLastPointWithEstimate() {
      for (int i = points.size() - 1; i >= 0; i--) {
         TrackPoint point = points.get(i);
         if (point.getEstimate() != null) {
            return point;
         }
      }
      throw new IllegalStateException();
   }
}
