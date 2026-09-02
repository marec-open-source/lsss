package no.imr.lsss.modules.korona.tracking;

import com.google.common.base.Suppliers;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.Cat0Datagram;
import no.imr.korona.viewer.variables.categorization.CategoryVariable;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.lsss.modules.korona.region.SchoolCategoryOverlay;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.PaintFactory;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Path2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public final class TrackCategoryEchogramOverlay extends BaseEchogramOverlay {
   private final Supplier<TrackInfoModule> trackInfoModule = moduleSupplier(TrackInfoModule.class);
   private final Supplier<EchogramTrackData> echogramTrackData = Suppliers.memoize(() -> trackInfoModule.get().getEchogramTrackData(getEchogramModule()));

   public TrackCategoryEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);

      Listener enabledListener = () -> setEnabledByUser(getConfigurationManager().getSurveyMiscConf().useTrackCategorization.getBooleanValue()
            && getInterpretationSettings().getDataFileSet().getConfigurationItem(Cac0Datagram.class) != null
            && SchoolCategoryOverlay.getCategoryVariable(getInterpretationSettings().getColorConverterContainer().getColorConverter()) != null);
      enabledListener.addTo(
            getConfigurationManager().getSurveyMiscConf().useTrackCategorization,
            getInterpretationSettings().getColorConverterContainer().getChangeManager(),
            getInterpretationSettings().getDataFileChangeManager()
      );
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            trackInfoModule.get().getValidIdsChangeManager(),
            echogramTrackData.get().getTrackDataChangeManager(),
            getInterpretationSettings().getColorConverterContainer().getChangeManager(),
            trackInfoModule.get().getTrackInfoChangeManager()
      ));
   }

   @Override
   public boolean isUserVisibleForegroundOverlay() {
      return false;
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      Cac0Datagram cac0Datagram = getInterpretationSettings().getDataFileSet().getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram == null) {
         return null;
      }

      CategoryVariable categoryVariable = SchoolCategoryOverlay.getCategoryVariable(getInterpretationSettings().getColorConverterContainer().getColorConverter());
      if (categoryVariable == null) {
         return null;
      }

      List<ColoredTrack> tracks = trackInfoModule.get().getTrackInfos().values().stream()
            .map(this::toTrack)
            .filter(Objects::nonNull)
            .toList();
      return transformed(new DisplayData(tracks));
   }

   private TrackCategoryEchogramOverlay.@Nullable ColoredTrack toTrack(TrackInfo trackInfo) {
      Cat0Datagram cat0Datagram = trackInfo.cat0Datagram();
      if (cat0Datagram == null) {
         return null;
      }
      if (!trackInfo.pingRange().intersects(getInterpretationSettings().getPingRange())) {
         return null;
      }
      EchogramTrackData.TrackData trackData = echogramTrackData.get().getTrackData().get(trackInfo.trackId());
      if (trackData == null) {
         return null;
      }
      CategoryVariable categoryVariable = SchoolCategoryOverlay.getCategoryVariable(getInterpretationSettings().getColorConverterContainer().getColorConverter());
      if (categoryVariable == null) {
         return null;
      }
      byte categoryNumber = cat0Datagram.getBestCategory(categoryVariable, (byte) -1);
      if (categoryNumber < 0) {
         return null;
      }
      Cac0Datagram cac0Datagram = getInterpretationSettings().getDataFileSet().getConfigurationItem(Cac0Datagram.class);
      if (cac0Datagram == null) {
         return null;
      }
      Color color = cac0Datagram.numberToCategory(categoryNumber).getColor();
      return new ColoredTrack(PaintFactory.createCrossedPaint(color, 9, 3, 0), trackData.extent());
   }

   private record ColoredTrack(Paint paint, Path2D.Float path) {
   }

   private record DisplayData(List<ColoredTrack> tracks) implements OverlayDisplayData {
      @Override
      public void draw(Graphics2D g2d) {
         for (ColoredTrack track : tracks) {
            g2d.setPaint(track.paint);
            g2d.fill(track.path);
         }
      }
   }
}
