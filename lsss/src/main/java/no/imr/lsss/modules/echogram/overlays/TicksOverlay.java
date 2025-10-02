package no.imr.lsss.modules.echogram.overlays;

import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.range.ArrayRangeSet;
import no.imr.tools.range.RangeSet;
import no.imr.tools.swing.GuiUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;
import java.util.List;

/**
 * Draws equidistant ticks on echogram.
 */
public final class TicksOverlay extends BaseEchogramOverlay {
   private static final float TICK_HEIGHT = 8;

   private final ObjectParameter<PingMapping> unit = new ObjectParameter<>(
         new Name("Unit"),
         PingMapping.TIME, PingMapping.values(),
         "Tick unit");

   private final FloatParameter timeTick = new FloatParameter(
         new Name("TickInterval", "Tick interval"),
         10, Unit.MINUTES, ValueConstraints.gt(0f),
         "Tick interval");

   private final FloatParameter distanceTick = new FloatParameter(
         new Name("DistanceTick", "Distance tick"),
         1, Unit.NAUTICAL_MILES, ValueConstraints.gt(0f),
         "Distance tick interval");

   private final IntParameter pingTick = new IntParameter(
         new Name("PingTick", "Ping tick"),
         100, Unit.COUNT, ValueConstraints.gte(1),
         "Ping number tick interval");

   public TicksOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      unit.addListenerAndNotify(pingMapping -> {
         timeTick.setVisible(pingMapping == PingMapping.TIME);
         distanceTick.setVisible(pingMapping == PingMapping.DISTANCE);
         pingTick.setVisible(pingMapping == PingMapping.NUMBER);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            unit,
            timeTick,
            distanceTick,
            pingTick
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener recomputeListener = createRecomputeListener();
      registry.add(getParameters(), recomputeListener);
      registry.add(getEchogramModule().echogramArea(), recomputeListener);
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      if (pingRange.isEmpty()) {
         return null;
      }

      PingMapping pingMapping = unit.getValue();
      double tickInterval = switch (pingMapping) {
         case TIME -> timeTick.getFloatValue() * 60;
         case DISTANCE -> distanceTick.getFloatValue();
         case NUMBER -> pingTick.getIntValue();
      };

      long iMin = (long) Math.ceil(pingMapping.valueOf(pingRange.begin()) / tickInterval);
      long iMax = (long) Math.floor(pingMapping.valueOf(pingRange.end()) / tickInterval);

      RangeSet<Float> xSet = new ArrayRangeSet<>();
      for (long i = iMin; i <= iMax; i++) {
         double value = i * tickInterval;
         float x = getPingSettings().valueToX(value, pingMapping);
         xSet.add(x, x + 1);
      }
      if (xSet.isEmpty()) {
         return null;
      }
      int pathCapacity = 5 * xSet.size();
      Path2D.Float path = new Path2D.Float(Path2D.WIND_NON_ZERO, pathCapacity);
      xSet.forEach((x0, x1) -> {
         GuiUtils.appendRectangle(path, x0, 0, x1, TICK_HEIGHT);
      });
      return new DisplayData(path);
   }

   private static final class DisplayData extends OverlayDisplayData {
      private final Path2D.Float path;

      private DisplayData(Path2D.Float path) {
         this.path = path;
      }

      @Override
      public void draw(Graphics2D g2d) {
         g2d.setColor(Color.YELLOW);
         g2d.fill(path);
      }
   }
}
