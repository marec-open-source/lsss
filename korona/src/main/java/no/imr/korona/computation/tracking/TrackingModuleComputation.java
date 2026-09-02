package no.imr.korona.computation.tracking;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.offset.TransducerParameterManager;
import no.imr.korona.computation.offset.TransducerParameters;
import no.imr.korona.computation.offset.TransducerRangesFileService;
import no.imr.korona.computation.tracking.data.Track;
import no.imr.korona.computation.tracking.data.TrackPoint;
import no.imr.korona.computation.tracking.impl.AggregationAssociator;
import no.imr.korona.computation.tracking.impl.AggregationCandidateExtractor;
import no.imr.korona.computation.tracking.impl.AggregationCompositor;
import no.imr.korona.computation.tracking.impl.AggregationInitiator;
import no.imr.korona.computation.tracking.impl.FilteringTargetCandidateExtractor;
import no.imr.korona.computation.tracking.impl.FloatingPositionFunction;
import no.imr.korona.computation.tracking.impl.MovingPositionFunction;
import no.imr.korona.computation.tracking.impl.SimplePredictor;
import no.imr.korona.computation.tracking.impl.SimpleTerminator;
import no.imr.korona.computation.tracking.impl.SimpleValidator;
import no.imr.korona.computation.tracking.impl.StationaryPositionFunction;
import no.imr.korona.computation.tracking.impl.TsDetectorAssociator;
import no.imr.korona.computation.tracking.impl.TsDetectorCandidateExtractor;
import no.imr.korona.computation.tracking.impl.TsDetectorCompositor;
import no.imr.korona.computation.tracking.impl.TsDetectorInitiator;
import no.imr.korona.data.datagrams.Dep0Datagram;
import no.imr.korona.data.datagrams.TBR0Datagram;
import no.imr.korona.data.datagrams.TNF0Datagram;
import no.imr.korona.data.datagrams.TTC0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.util.ts.DatagramTSDetector;
import no.imr.korona.util.ts.PeakTSDetector;
import no.imr.korona.util.ts.SedTSDetector;
import no.imr.korona.util.ts.TSDetector;
import no.imr.tools.Utils;
import no.imr.tools.range.FloatRange;
import no.imr.tools.xml.XmlUtils;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;
import java.util.TreeSet;

final class TrackingModuleComputation extends GeneralPingModuleComputation implements TrackingListener {
   private final TrackingModule module;

   private final TargetTracker targetTracker;
   private final List<Integer> validIds = new ArrayList<>();
   private final NavigableSet<Instant> instants = new TreeSet<>();
   private final float blindZone;
   private final int channel;
   private @Nullable Ping ping;
   private @Nullable PowerData powerData;

   TrackingModuleComputation(TrackingModule module, ComputationContext computationContext, PingSource pingSource, int channel) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;

      Path file = module.getRequiredConfigFile(TransducerRangesFileService.NAME);
      TransducerParameterManager transducerParameterManager = new TransducerParameterManager(TransducerParameters.ParameterType.VERTICAL, XmlUtils.readDocument(file));
      blindZone = transducerParameterManager.getBlindZone(module.kHz.getIntValue()).orElse(0f);
      this.channel = channel;
      targetTracker = createTargetTracker();
   }

   private TargetTracker createTargetTracker() throws IOException {
      return switch (module.targetTrackerType.getValue()) {
         case AGGREGATION -> {
            yield createTargetTracker(
                  new AggregationCandidateExtractor(channel, tscRange(), 2 * module.maxGainCompensation.getFloatValue()),
                  new AggregationAssociator(module.maxMissingSamples.getIntValue()),
                  new AggregationInitiator(module.initiationGateFunction.create(), module.initiationMinLength.getIntValue()),
                  new AggregationCompositor());
         }
         case SED -> {
            TSDetector tsDetector = new SedTSDetector(module.minTS.getFloatValue(), module.maxGainCompensation.getFloatValue(),
                  module.pulseLengthDeterminationLevel.getFloatValue(), module.minEchoLength.getFloatValue(), module.maxEchoLength.getFloatValue(),
                  module.doPhaseDeviationCheck.getBooleanValue(), module.maxPhaseDevPhaseSteps.getFloatValue(),
                  module.maxDepth.getValue().orElse(Float.POSITIVE_INFINITY));
            yield createTargetTracker(tsDetector);
         }
         case PEAK -> {
            TSDetector tsDetector = new PeakTSDetector(module.minTS.getFloatValue(),
                  module.maxGainCompensation.getFloatValue(), module.pulseLengthDeterminationLevel.getFloatValue(),
                  module.minEchoLength.getFloatValue(), module.maxEchoLength.getFloatValue(),
                  module.doPhaseDeviationCheck.getBooleanValue(), module.maxPhaseDevPhaseSteps.getFloatValue(),
                  module.maxDepth.getValue().orElse(Float.POSITIVE_INFINITY));
            yield createTargetTracker(tsDetector);
         }
         case TS_MODULE -> {
            TSDetector tsDetector = new DatagramTSDetector();
            yield createTargetTracker(tsDetector);
         }
      };
   }

   private TargetTracker createTargetTracker(TSDetector tsDetector) throws IOException {
      return createTargetTracker(
            new TsDetectorCandidateExtractor(channel, tsDetector),
            new TsDetectorAssociator(),
            new TsDetectorInitiator(),
            new TsDetectorCompositor());
   }

   private TargetTracker createTargetTracker(TargetCandidateExtractor targetCandidateExtractor, Associator associator, Initiator initiator, Compositor compositor) throws IOException {
      PositionFunction positionFunction = switch (module.platformMotionType.getValue()) {
         case STATIONARY -> new StationaryPositionFunction();
         case FLOATING -> new FloatingPositionFunction();
         case MOVING -> new MovingPositionFunction();
      };

      targetCandidateExtractor = new FilteringTargetCandidateExtractor(targetCandidateExtractor, tscRange(), module.maxDepth.getValue().orElse(Float.POSITIVE_INFINITY),
            module.maxAlongshipAngle.getFloatValue(), module.maxAthwartshipAngle.getFloatValue());

      Terminator terminator = new SimpleTerminator(module.maxMissingPings.getIntValue());
      Validator validator = new SimpleValidator(module.maxMissingPingsFraction.getFloatValue(), module.minTrackLength.getIntValue(),
            module.minSampleToLengthFraction.getFloatValue());

      TrackIdGenerator trackIdGenerator = new TrackIdGenerator(channel, peekPingSourcePing(0));
      return new TargetTracker(this, positionFunction, targetCandidateExtractor,
            new SimplePredictor(), associator,
            initiator, module.alphaBetaEstimator.create(), terminator, validator, compositor,
            module.gateFunction.create(), trackIdGenerator);
   }

   private FloatRange tscRange() {
      return FloatRange.of(module.minTS.getFloatValue(), module.maxTS.getFloatValue());
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      ping = inputPing();
      if (ping == null) {
         return null;
      }

      powerData = ping.getPowerData(channel);
      targetTracker.track(ping, getDepthRange());

      if (peekPingSourcePing(0) == null) {
         targetTracker.end();
         ping.add(new TTC0Datagram(ping.getInstant(), Utils.toInts(validIds), List.copyOf(instants)));
      }

      return ping;
   }

   private FloatRange getDepthRange() {
      if (ping == null || powerData == null) {
         return FloatRange.EMPTY_RANGE;
      }

      float min = Math.min(powerData.rangeToDepth(blindZone), powerData.getMaxDepth());
      float max;
      Dep0Datagram dep0Datagram = ping.getDep0Datagram();
      if (dep0Datagram != null) {
         max = dep0Datagram.getMinimumDepth();
      } else {
         double depth = ping.getBot0Datagram().getChannelDepths()[channel - 1];
         if (depth != 0) {
            max = (float) depth;
         } else {
            max = powerData.getMaxDepth();
         }
      }
      return FloatRange.of(min, Math.clamp(max, min, powerData.getMaxDepth()));
   }

   @Override
   public void onNewTrackPoint(Track track) {
      if (ping == null || powerData == null) {
         return;
      }
      TrackPoint trackPoint = track.getLastPoint();
      int id = track.getId();
      float minDepth = powerData.rangeToDepth(trackPoint.getRangeRange().min());
      float maxDepth = powerData.rangeToDepth(trackPoint.getRangeRange().max());
      float peakDepth = powerData.rangeToDepth(trackPoint.getMeasurement().range());
      ping.add(new TBR0Datagram(ping.getInstant(), id, channel, FloatRange.of(minDepth, maxDepth), peakDepth));
   }

   @Override
   public void onTrackTermination(Track track, boolean valid) {
      if (ping == null || powerData == null) {
         return;
      }
      int pingsSinceFirst = (int) (ping.getPingNumber() - track.getPoints().getFirst().getPingIndex().getPingNumber());
      int pingsSinceLast = (int) (ping.getPingNumber() - track.getLastPointWithEstimate().getPingIndex().getPingNumber());
      ping.add(new TNF0Datagram(ping.getInstant(), track.getId(), channel, valid, pingsSinceFirst, pingsSinceLast));
      if (valid) {
         validIds.add(track.getId());
         instants.add(ping.getInstant());
      }
   }
}
