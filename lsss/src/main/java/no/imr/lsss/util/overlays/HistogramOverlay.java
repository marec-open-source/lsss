package no.imr.lsss.util.overlays;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.viewer.SvColorPanel;
import no.imr.korona.viewer.TransferFunction;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.korona.viewer.variables.ContinuousVariableResult;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.lsss.LSSS;
import no.imr.tools.Max;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import no.imr.tools.math.Histogram1D;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.imr.tools.swing.overlay.Overlay;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.geom.Path2D;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An overlay to a {@link SvColorPanel} showing a {@link Histogram1D}.
 */
public final class HistogramOverlay extends Overlay {
   private final TransferFunction transferFunction;
   private final Listener recomputeListener;
   private DisplayData displayData = computeDisplayData(null);

   public HistogramOverlay(LSSS lsss, TransferFunction transferFunction,
                           ArgChangeManager<Optional<Ping>> pingChangeManager, Supplier<@Nullable Ping> pingSupplier) {
      this.transferFunction = transferFunction;

      recomputeListener = Listeners.coalescingInExecutor(lsss.getInterpretationSettings().getObservingExecutor(), () -> {
         displayData = computeDisplayData(pingSupplier.get());
         repaint();
      });
      pingChangeManager.addListener(recomputeListener);
   }

   @Override
   public void resized(GraphicsConfiguration graphicsConfiguration, int width, int height) {
      recomputeListener.listen();
   }

   @Override
   public void draw(Graphics2D g2d) {
      g2d.setColor(Color.BLUE);
      g2d.draw(displayData.curve);
   }

   public int logSvToCount(float logSv) {
      return displayData.histogram.valueToCount(logSv);
   }

   private DisplayData computeDisplayData(@Nullable Ping ping) {
      Path2D.Float curve = new Path2D.Float();
      Histogram1D histogram;
      if (ping == null) {
         histogram = Histogram1D.fromBinCount(FloatRange.of(0, 1), 1);
      } else {
         histogram = createHistogram(ping);
         int[] counts = histogram.getCounts();
         int maxCount = Max.of(counts);
         float xScale = maxCount == 0 ? 0 : (float) (getWidth() - 1) / maxCount;
         float yScale = (float) (getHeight() - 1) / (counts.length - 1);

         LineStripBuilder curveBuilder = LineStripBuilders.coalescing(curve);
         for (int i = 0; i < counts.length; i++) {
            float x = xScale * counts[counts.length - 1 - i];
            curveBuilder.addPoint(x, yScale * i);
            curveBuilder.addPoint(x, yScale * (i + 1));
         }
         curveBuilder.endLineStrip();
      }
      return new DisplayData(curve, histogram);
   }

   private Histogram1D createHistogram(Ping ping) {
      ContinuousVariable variable = transferFunction.getColorConverterContainer().getSV();
      ContinuousVariableSettings settings = variable.getSettings();
      Histogram1D histogram = Histogram1D.fromBinCount(settings.getMaxRange(), Math.round(settings.getMaxRange().getSize() / settings.getDelta()));

      int transducerCount = ping.getRawFileConfiguration().getTransducerCount();
      for (int channel = 1; channel <= transducerCount; channel++) {
         ContinuousVariableResult continuousVariableResult = variable.evaluate(channel, ping);
         if (continuousVariableResult != null) {
            histogram.addValues(continuousVariableResult.floatData);
         }
      }
      return histogram;
   }


   private record DisplayData(Path2D.Float curve, Histogram1D histogram) {
   }
}
