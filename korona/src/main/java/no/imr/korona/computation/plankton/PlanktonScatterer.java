package no.imr.korona.computation.plankton;

import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.tools.range.RangeMap;
import no.imr.tools.range.RangeUtils;
import org.jspecify.annotations.Nullable;

public final class PlanktonScatterer<B extends BackscatterModel> {
   private final String description;
   private final Pic0Datagram.PlanktonCategory planktonCategory;
   private final B backscatterModel;
   private RangeMap<Float, PlanktonRectangle> initialSizeHistogramMap = RangeUtils.emptyRangeMap();

   public PlanktonScatterer(String description, Pic0Datagram.PlanktonCategory planktonCategory, B backscatterModel) {
      this.description = description;
      this.planktonCategory = planktonCategory;
      this.backscatterModel = backscatterModel;
   }

   public String getDescription() {
      return description;
   }

   public Pic0Datagram.PlanktonCategory getPlanktonCategory() {
      return planktonCategory;
   }

   public B getBackscatterModel() {
      return backscatterModel;
   }

   public @Nullable SizeHistogram getInitialSizeHistogram(float depth) {
      PlanktonRectangle planktonRectangle = initialSizeHistogramMap.get(depth);
      return planktonRectangle != null ? planktonRectangle.getSizeHistogram() : null;
   }

   public void setInitialSizeHistogramMap(RangeMap<Float, PlanktonRectangle> initialSizeHistogramMap) {
      this.initialSizeHistogramMap = initialSizeHistogramMap;
   }

   @Override
   public String toString() {
      return planktonCategory.getName();
   }
}
