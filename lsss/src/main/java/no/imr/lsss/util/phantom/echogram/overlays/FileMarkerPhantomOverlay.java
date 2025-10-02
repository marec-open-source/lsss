package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.overlays.FileMarkerEngine;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Draws markers on echogram indicating the beginning of files.
 */
public final class FileMarkerPhantomOverlay extends BasePhantomOverlay {
   private final IntParameter markerHeight = new IntParameter(
         new Name("MarkerHeight", "Marker height"),
         30, Unit.COUNT, ValueConstraints.gt(0),
         "Height of markers in pixels");

   private final IntParameter lineThickness = new IntParameter(
         new Name("LineThickness", "Line thickness"),
         3, Unit.COUNT, ValueConstraints.gt(0),
         "Thickness of lines in pixels");

   private final FileMarkerEngine fileMarkerEngine;

   public FileMarkerPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);

      fileMarkerEngine = new FileMarkerEngine(this, getPingSettings(), getPhantomEchogramSettings().getPhantomDataManager());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            markerHeight,
            lineThickness
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getPhantomEchogramModule().getEchogramAreaChangeManager(),
            markerHeight,
            lineThickness
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      return fileMarkerEngine.update(markerHeight.getIntValue(), lineThickness.getIntValue());
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      return fileMarkerEngine.getToolTipText();
   }

   @Override
   public void onDeactivate() {
      fileMarkerEngine.onDeactivate();
   }

   @Override
   public void mouseClicked(MouseEvent mouseEvent) {
      fileMarkerEngine.mouseClicked(mouseEvent);
   }
}
