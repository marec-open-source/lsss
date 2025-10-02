package no.imr.lsss.modules.korona.region;

import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.mask.MaskOutlineTracer;
import no.imr.korona.region.School;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogram.overlays.BaseEchogramOverlay;
import no.imr.tools.Utils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.TableToolTipBuilder;
import no.imr.tools.swing.linestrip.LineStripBuilders;
import no.marec.lsss.api.util.LineStripBuilder;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Displays regions detected by KORONA in LSSS.
 */
public class KoronaRegionEchogramOverlay extends BaseEchogramOverlay {
   private final Supplier<KoronaRegionModule> koronaRegionModule = moduleSupplier(KoronaRegionModule.class);
   final Map<KoronaRegionLSSS, RegionPath> regionPaths = new ConcurrentHashMap<>();

   public KoronaRegionEchogramOverlay(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo, EchogramModule echogramModule) {
      super(moduleInfo, echogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::reset), List.of(
            getEchogramModule().echogramArea(),
            getInterpretationSettings().getDataFileChangeManager()
      ));

      //---

      reset();
   }

   @Override
   protected void onDisable() {
      regionPaths.clear();
   }

   private void reset() {
      regionPaths.clear();
      for (KoronaRegionLSSS koronaRegion : koronaRegionModule.get().getKoronaRegions()) {
         createPath(koronaRegion);
      }
      recompute();
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      if (regionPaths.isEmpty()) {
         return null;
      }
      return new DisplayData();
   }

   void repaintRegions() {
      repaint();
   }

   void newRegionLoaded(KoronaRegionLSSS koronaRegion) {
      executeIfEnabled(() -> {
         createPath(koronaRegion);
         recompute();
      });
   }

   private void createPath(KoronaRegionLSSS koronaRegion) {
      if (!getInterpretationSettings().getPingRange().intersects(koronaRegion.getPingRange())
            || regionPaths.containsKey(koronaRegion)) {
         return;
      }

      Path2D path = new Path2D.Float();
      LineStripBuilder pathBuilder = LineStripBuilders.piecewiseHorizontal(path);
      Set<? extends List<EchogramPoint>> boundary = MaskOutlineTracer.createBoundary(koronaRegion.getMask(), getInterpretationSettings().getDataFileSet());
      for (List<EchogramPoint> boundaryPointList : boundary) {
         for (EchogramPoint echogramPoint : boundaryPointList) {
            PingIndex pingIndex = echogramPoint.pingIndex();
            float x = getPingSettings().pingIndexToX(pingIndex);
            float y = getZSettings().depthToY(echogramPoint.depth(), pingIndex);
            pathBuilder.addPoint(x, y);
         }
         pathBuilder.endLineStrip();
         path.closePath();
      }
      regionPaths.put(koronaRegion, new RegionPath(path, path.getBounds2D()));
   }

   @Override
   public boolean readyToTakeFocus() {
      KoronaRegionLSSS activeKoronaRegion = koronaRegionModule.get().getActiveKoronaRegion();
      return activeKoronaRegion != null && !activeKoronaRegion.isIgnored();
   }

   @Override
   public @Nullable String getToolTipText(Point point) {
      KoronaRegionLSSS activeKoronaRegion = koronaRegionModule.get().getActiveKoronaRegion();
      if (activeKoronaRegion == null) {
         return null;
      }
      RegionInfoDatagram info = activeKoronaRegion.getRegionInfoDatagram();
      RegionInfoDatagram.Values values = info.getValues();
      return new TableToolTipBuilder()
            .addLine("KORONA region:")
            .addRow("Area [m<sup>2</sup>]", Utils.format("%.1f", values.area))
            .addRow("Length [m]", Utils.format("%.1f", values.length))
            .addRow("Max height [m]", Utils.format("%.1f", values.maxHeight))
            .addRow("Perimeter [m]", Utils.format("%.1f", values.perimeter))
            .addRow("Compactness [-]", Utils.format("%.3f", values.getCircleCompactness()))
            .build();
      // Don't show sv in tooltip as it is calculated on a combination channel and thus not relevant.
   }

   @Override
   public JPopupMenu getPopupMenu(Point point) {
      JPopupMenu popupMenu = new JPopupMenu();

      KoronaRegionLSSS activeKoronaRegion = koronaRegionModule.get().getActiveKoronaRegion();

      JMenuItem convertThisRegionItem = popupMenu.add("Convert this KORONA region to LSSS school");
      if (activeKoronaRegion == null) {
         convertThisRegionItem.setEnabled(false);
      } else {
         convertThisRegionItem.addActionListener(e -> {
            School school = koronaRegionModule.get().convertToSchool(activeKoronaRegion);
            if (school != null) {
               getRegionManager().replaceSelectedRegions(school);
            }
         });
      }

      JMenuItem convertUnconvertedRegionsItem = popupMenu.add("Convert all unconverted visible KORONA regions");
      convertUnconvertedRegionsItem.addActionListener(e -> convertKoronaRegions(true));

      JMenuItem convertAllRegionsItem = popupMenu.add("Convert all visible KORONA regions");
      convertAllRegionsItem.addActionListener(e -> convertKoronaRegions(false));

      popupMenu.addSeparator();

      getEchogramModule().getViewHolder().getView().addDisableOverlaysItems(point, popupMenu);

      return popupMenu;
   }

   private void convertKoronaRegions(boolean skipIgnored) {
      PingRange pingRange = getInterpretationSettings().getPingRange();
      Predicate<KoronaRegionLSSS> koronaRegionFilter = koronaRegion -> pingRange.intersects(koronaRegion.getPingRange());
      if (skipIgnored) {
         koronaRegionFilter = koronaRegionFilter.and(Predicate.not(KoronaRegionLSSS::isIgnored));
      }
      List<School> convertedSchools = koronaRegionModule.get().getKoronaRegions().stream()
            .filter(koronaRegionFilter)
            .map(koronaRegionModule.get()::convertToSchool)
            .filter(Objects::nonNull)
            .toList();
      getRegionManager().replaceSelectedRegions(convertedSchools);
   }

   record RegionPath(Path2D path, Rectangle2D bounds) {
   }

   private final class DisplayData extends OverlayDisplayData {
      private DisplayData() {
      }

      @Override
      public void draw(Graphics2D g2d) {
         for (Map.Entry<KoronaRegionLSSS, RegionPath> entry : regionPaths.entrySet()) {
            KoronaRegionLSSS koronaRegion = entry.getKey();
            Color color;
            if (!koronaRegion.isIgnored() && koronaRegion == koronaRegionModule.get().getActiveKoronaRegion()) {
               color = Color.GREEN;
            } else if (koronaRegion.isIgnored()) {
               color = Color.GRAY;
            } else {
               color = ColorUtils.MAGENTA;
            }
            g2d.setColor(color);
            g2d.draw(entry.getValue().path);
         }
      }

      @Override
      public boolean intersects(Rectangle2D rectangle) {
         for (Map.Entry<KoronaRegionLSSS, RegionPath> entry : regionPaths.entrySet()) {
            if (entry.getValue().bounds.intersects(rectangle)
                  && GuiUtils.intersects(entry.getValue().path, rectangle)) {
               koronaRegionModule.get().setActiveKoronaRegion(entry.getKey());
               return true;
            }
         }
         koronaRegionModule.get().setActiveKoronaRegion(null);
         return false;
      }
   }
}
