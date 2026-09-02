package no.imr.lsss.modules.korona.region;

import no.imr.korona.data.datagrams.RegionInfoDatagram;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.region.School;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.korona.DataFilesCache;
import no.imr.lsss.modules.korona.DataObjectLoader;
import no.imr.tools.Utils;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.logging.Log;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Manages data for regions detected by KORONA.
 */
public final class KoronaRegionModule extends BaseDataModule {
   public static final String XML_CONVERTED = "converted";
   private static final String XML_REGION_IDENTIFIER = "regionIdentifier";
   private static final String XML_START_TIME = "startTime";
   private static final String XML_NUMBER_OF_PINGS = "numberOfPings";
   private static final String XML_DEPTH = "depth";

   private final Set<KoronaRegionLSSS> koronaRegions = ConcurrentHashMap.newKeySet();
   private final NavigableSet<KoronaRegionIdentifier> convertedRegions = new ConcurrentSkipListSet<>();
   private @Nullable KoronaRegionLSSS activeKoronaRegion;
   private final DataFilesCache<KoronaRegionLSSS> koronaRegionCache = new DataFilesCache<>();
   private DataObjectLoader<KoronaRegionLSSS> koronaRegionLoader;
   private List<KoronaRegionEchogramOverlay> koronaRegionEchogramOverlays = List.of();

   public KoronaRegionModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      koronaRegionLoader = KoronaRegionLoader.make(getInterpretationSettings().getExecutorObservation(), DataFileSet.empty(), koronaRegionCache, Utils.emptyConsumer());
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      koronaRegionEchogramOverlays = getModuleManager().getModules(KoronaRegionEchogramOverlay.class).toList();

      registry.add(getInterpretationSettings().getDataFileChangeManager(), newCoalescingExecListener(this::readAllRegionInfoDatagrams));
      registry.add(getInterpretationSettings().getPingRangeChangeManager(), newCoalescingExecListener(this::setPriorityPingRange));
      registry.add(getInterpretationSettings().getCancelChangeManager(), newCoalescingExecListener(() -> koronaRegionLoader.cancel()));
   }

   private void setPriorityPingRange(PingRange pingRange) {
      koronaRegionLoader.setPriorityPingRange(pingRange);
   }

   private void repaintOverlays() {
      for (KoronaRegionEchogramOverlay koronaRegionEchogramOverlay : koronaRegionEchogramOverlays) {
         koronaRegionEchogramOverlay.repaintRegions();
      }
   }

   public Collection<KoronaRegionLSSS> getKoronaRegions() {
      return koronaRegions;
   }

   @Nullable KoronaRegionLSSS getActiveKoronaRegion() {
      return activeKoronaRegion;
   }

   void setActiveKoronaRegion(@Nullable KoronaRegionLSSS koronaRegion) {
      if (activeKoronaRegion != koronaRegion) {
         activeKoronaRegion = koronaRegion;
         repaintOverlays();
      }
   }

   private void readAllRegionInfoDatagrams() {
      koronaRegionLoader.cancel();

      // Delete all KoronaRegionIdentifiers not within the ping range of the current data files.
      convertedRegions.removeIf(koronaRegionIdentifier -> {
         return !getInterpretationSettings().getDataFileSet().getTotalRange().contains(koronaRegionIdentifier.pingRange);
      });

      koronaRegions.clear();

      koronaRegionLoader = KoronaRegionLoader.make(getInterpretationSettings().getExecutorObservation(), getInterpretationSettings().getDataFileSet(), koronaRegionCache, koronaRegion -> {
         koronaRegions.add(koronaRegion);
         if (convertedRegions.contains(new KoronaRegionIdentifier(koronaRegion))) {
            koronaRegion.setIgnored(true);
         }
         for (KoronaRegionEchogramOverlay koronaRegionEchogramOverlay : koronaRegionEchogramOverlays) {
            koronaRegionEchogramOverlay.newRegionLoaded(koronaRegion);
         }
      });
      koronaRegionLoader.setPriorityPingRange(getInterpretationSettings().getPingRange());
   }

   @Nullable School convertToSchool(KoronaRegionLSSS koronaRegion) {
      koronaRegion.setIgnored(true);
      convertedRegions.add(new KoronaRegionIdentifier(koronaRegion)); //remember which regions are converted
      School school = getRegionManager().addSchool(koronaRegion.getMask());
      repaintOverlays();
      return school;
   }

   public @Nullable Element convertedRegionsToXml(PingRange pingRange) {
      Element interpretation = DocumentHelper.createElement(XML_CONVERTED);
      for (KoronaRegionIdentifier koronaRegionIdentifier : convertedRegions) {
         if (pingRange.contains(koronaRegionIdentifier.pingRange)) {
            interpretation.addElement(XML_REGION_IDENTIFIER)
                  .addAttribute(XML_START_TIME, Double.toString(koronaRegionIdentifier.startTime))
                  .addAttribute(XML_NUMBER_OF_PINGS, Long.toString(koronaRegionIdentifier.pingCount))
                  .addAttribute(XML_DEPTH, Float.toString(koronaRegionIdentifier.firstDepth));
         }
      }

      return !interpretation.elements().isEmpty() ? interpretation : null;
   }

   public void convertedRegionsFromXml(Path workFile, Element convertedElement) {
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();
      for (Element element : convertedElement.elements()) {
         String startTimeAttribute = element.attributeValue(XML_START_TIME);
         try {
            double startTime = Double.parseDouble(startTimeAttribute);
            long pingCount = Long.parseLong(element.attributeValue(XML_NUMBER_OF_PINGS));
            float firstDepth = Float.parseFloat(element.attributeValue(XML_DEPTH));
            convertedRegions.add(new KoronaRegionIdentifier(dataFileSet, startTime, pingCount, firstDepth));
         } catch (Exception e) {
            Log.global.warning("Error parsing KORONA region conversion in " + workFile.getFileName() + " (startTime=" + startTimeAttribute + "): " + e);
         }
      }
      for (KoronaRegionLSSS koronaRegion : getKoronaRegions()) {
         if (convertedRegions.contains(new KoronaRegionIdentifier(koronaRegion))) {
            koronaRegion.setIgnored(true);
         }
      }
   }

   /**
    * Identifies a KORONA region.
    */
   private static final class KoronaRegionIdentifier implements Comparable<KoronaRegionIdentifier> {
      private final PingRange pingRange;
      private final double startTime;
      private final long pingCount;
      private final float firstDepth;

      private KoronaRegionIdentifier(KoronaRegionLSSS koronaRegion) {
         pingRange = koronaRegion.getPingRange();
         startTime = PingMapping.TIME.valueOf(pingRange.begin());
         pingCount = pingRange.getPingCount();
         firstDepth = findFirstDepth(koronaRegion.getRegionInfoDatagram());
      }

      private KoronaRegionIdentifier(DataFileSet dataFileSet, double startTime, long pingCount, float firstDepth) {
         this.startTime = startTime;
         this.pingCount = pingCount;
         this.firstDepth = firstDepth;

         PingIndex startPing = dataFileSet.getClosestPingIndex(startTime, PingMapping.TIME);
         PingIndex endPing = dataFileSet.getPingIndex(startPing.getPingNumber() + pingCount);
         pingRange = PingRange.of(startPing, endPing);
      }

      private static float findFirstDepth(RegionInfoDatagram regionInfoDatagram) {
         List<RegionInfoDatagram.MaskInterval> maskIntervals = regionInfoDatagram.getMaskIntervals();
         if (maskIntervals != null) {
            return maskIntervals.getFirst().minDepth();
         }
         List<RegionInfoDatagram.PerimeterPoint> perimeterPoints = regionInfoDatagram.getPerimeterPoints();
         if (perimeterPoints != null) {
            return perimeterPoints.getFirst().depth();
         }
         return Float.NaN;
      }

      @Override
      public boolean equals(@Nullable Object obj) {
         if (this == obj) {
            return true;
         }
         return obj instanceof KoronaRegionIdentifier that
               && Double.doubleToLongBits(startTime) == Double.doubleToLongBits(that.startTime)
               && pingCount == that.pingCount
               && Float.floatToIntBits(firstDepth) == Float.floatToIntBits(that.firstDepth);
      }

      @Override
      public int hashCode() {
         int result = Double.hashCode(startTime);
         result = 31 * result + Long.hashCode(pingCount);
         result = 31 * result + Float.floatToIntBits(firstDepth);
         return result;
      }

      @Override
      public int compareTo(KoronaRegionIdentifier other) {
         int timeCompare = Double.compare(startTime, other.startTime);
         if (timeCompare != 0) {
            return timeCompare;
         }
         int depthCompare = Float.compare(firstDepth, other.firstDepth);
         if (depthCompare != 0) {
            return depthCompare;
         }
         return Long.compare(pingCount, other.pingCount);
      }
   }
}
