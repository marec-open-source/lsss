package no.imr.lsss.region.ek500;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.formats.ek500.EK500PingIndex;
import no.imr.korona.data.formats.ek500.EK500SegmentData;
import no.imr.korona.data.formats.ek500.IndexRecord;
import no.imr.korona.data.formats.ek500.InfoRecord;
import no.imr.korona.data.formats.ek500.PingFile;
import no.imr.korona.data.formats.ek500.WorkRecord;
import no.imr.korona.data.ping.ExtrapolatedPingIndex;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.geometry.EchogramPoint;
import no.imr.korona.data.util.geometry.depth.DepthTransform;
import no.imr.korona.data.util.geometry.depth.IdentityDepthTransform;
import no.imr.korona.data.util.geometry.depth.PerPingDepthTransform;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.CurveBoundary;
import no.imr.korona.region.Layer;
import no.imr.korona.region.LayerAndBoundaryPair;
import no.imr.korona.region.Region;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Converts the work files of a single {@link EK500SegmentData}.
 */
final class EK500WorkConverter {
   private final RegionManager regionManager;
   private final SpeciesConverter speciesConverter;
   private final DataFile dataFile;
   private final DataFileSet dataFileSet;
   private final EK500SegmentData ek500SegmentData;
   private final PingRange pingRange;
   private final double firstVesselDistance;

   private final float mainFrequency;
   private final int defaultSpecies;

   private final double vesselDistancePerEK500Pixel;

   EK500WorkConverter(RegionManager regionManager, Path ek500WorkDir, SpeciesConverter speciesConverter,
                      DataFileSet dataFileSet, float mainFrequency, int defaultSpecies) throws IOException {
      this.regionManager = regionManager;
      this.speciesConverter = speciesConverter;
      dataFile = dataFileSet.getDataFiles().getFirst();
      this.dataFileSet = dataFileSet;
      pingRange = dataFile.getPingRange();
      firstVesselDistance = pingRange.begin().getVesselDistance();
      ek500SegmentData = (EK500SegmentData) dataFile.getSegmentData();
      ek500SegmentData.loadWorkRecords(ek500WorkDir);
      vesselDistancePerEK500Pixel = pingRange.getVesselDistance() / (double) WorkRecord.HORIZONTAL_PIXELS;

      this.mainFrequency = mainFrequency;
      this.defaultSpecies = defaultSpecies;
   }

   void convert() {
      PingFile mainPingFile = getMainPingFile();
      if (mainPingFile != null) {
         InfoRecord infoRecord = mainPingFile.getInfoRecord();
         speciesConverter.setPlatform(infoRecord.nation, infoRecord.ship);

         convertLayers(mainPingFile);
      }

      for (PingFile pingFile : ek500SegmentData.getPingFiles()) {
         convertSchools(pingFile);
      }
   }

   private @Nullable PingFile getMainPingFile() {
      // Return ping file with work record with frequency closest to main frequency. If no such file can be found, return null.
      PingFile result = null;
      float bestFitFreqDist = Float.MAX_VALUE;
      for (PingFile pingFile : ek500SegmentData.getPingFiles()) {
         WorkRecord workRecord = pingFile.getWorkRecord();
         if (workRecord != null) {
            float freqDist = Math.abs(mainFrequency - pingFile.getInfoRecord().getFrequency());
            if (freqDist < bestFitFreqDist) {
               result = pingFile;
               bestFitFreqDist = freqDist;
            }
         }
      }
      return result;
   }

   private void convertLayers(PingFile pingFile) {
      WorkRecord workRecord = pingFile.getWorkRecord();
      if (workRecord == null) {
         return;
      }
      int delSjiktLengde = workRecord.getVersion().delSjiktLengde;
      int sjiktLengde = workRecord.getVersion().sjiktLengde();
      int horLengde = sjiktLengde * delSjiktLengde;

      EK500PelagicDepthTransform pelagicDepthTransform = new EK500PelagicDepthTransform(pingFile);
      EK500BottomDepthTransform bottomDepthTransform = new EK500BottomDepthTransform(pingFile);

      CurveBoundary upperBoundary = regionManager.getLayerManager().getUpperBoundary(pingRange.begin());
      CurveBoundary bottomBoundary = regionManager.getLayerManager().getBottomBoundary(pingRange.begin());
      assert upperBoundary.getCurve().getStartDepth() == 0;

      // Create all layers
      int layerCount = workRecord.layer.length;
      int lastPelagicLayerIndex;
      if (workRecord.bottomIsDefined == 1) {
         // Add layer between pelagic and bottom data
         layerCount++;
         lastPelagicLayerIndex = layerCount - 3;
      } else {
         if (layerCount > 0 && workRecord.layer[layerCount - 1].type == 1) {
            layerCount--;
         }
         lastPelagicLayerIndex = layerCount - 1;
      }
      List<CurveBoundary> curveBoundaries = new ArrayList<>();
      curveBoundaries.add(upperBoundary);
      for (int i = 1; i < layerCount; i++) {
         EchogramPoint echogramPoint = new EchogramPoint(pingRange.begin(), i / (float) layerCount);
         LayerAndBoundaryPair<CurveBoundary> layerAndBoundaryPair = regionManager.getLayerManager().addCurveBoundary(echogramPoint, IdentityDepthTransform.INSTANCE);
         if (layerAndBoundaryPair != null) {
            curveBoundaries.add(layerAndBoundaryPair.boundary());
         }
      }
      curveBoundaries.add(bottomBoundary);
      assert curveBoundaries.size() == layerCount + 1;

      // Edit both boundaries of bottom layer
      if (workRecord.bottomIsDefined == 1) {
         CurveBoundary upperBottomLayerBoundary = curveBoundaries.get(curveBoundaries.size() - 2);
         WorkRecord.LayerParameters layerParameters = workRecord.layer[workRecord.layer.length - 1];

         int j = 0;
         for (int i = 0; i < sjiktLengde; i++) {
            int iNext = i == sjiktLengde - 1 ? i : i + 1;

            for (int jDelta = 0; jDelta < delSjiktLengde; jDelta++) {
               int jNext = j == horLengde - 1 ? j : j + 1;
               EchogramPoint p1 = toEchogramPoint(bottomDepthTransform, j, layerParameters.boundary[i] - workRecord.bottomOffset[j]);
               EchogramPoint p2 = toEchogramPoint(bottomDepthTransform, j + 1, layerParameters.boundary[iNext] - workRecord.bottomOffset[jNext]);
               regionManager.getLayerManager().editBoundary(p1, p2, bottomDepthTransform, bottomBoundary);
               j++;
            }
         }

         EchogramPoint p1 = toEchogramPoint(bottomDepthTransform, 0, 0);
         EchogramPoint p2 = toEchogramPoint(bottomDepthTransform, horLengde, 0);
         regionManager.getLayerManager().editBoundary(p1, p2, bottomDepthTransform, upperBottomLayerBoundary);
      }

      // Edit the bottom boundary of the lowest pelagic layer.
      CurveBoundary lowerPelagicLayerBoundary = curveBoundaries.get(lastPelagicLayerIndex + 1);
      for (int i = 0; i < sjiktLengde; i++) {
         PingIndex pingIndex1 = ek500PixelToPingIndex(i * delSjiktLengde);
         PingIndex pingIndex2 = ek500PixelToPingIndex((i + 1) * delSjiktLengde);
         EchogramPoint p1 = new EchogramPoint(pingIndex1, getIndexRecord(pingFile, pingIndex1).getPelagicEchogramMaxDepth());
         EchogramPoint p2 = new EchogramPoint(pingIndex2, getIndexRecord(pingFile, pingIndex2).getPelagicEchogramMaxDepth());
         regionManager.getLayerManager().editBoundary(p1, p2, IdentityDepthTransform.INSTANCE, lowerPelagicLayerBoundary);
      }

      // Edit all pelagic layers
      for (int layerIndex = lastPelagicLayerIndex; layerIndex >= 0; layerIndex--) {
         CurveBoundary curveBoundary = curveBoundaries.get(layerIndex);
         WorkRecord.LayerParameters layerParameters = workRecord.layer[layerIndex];
         for (int i = 0; i < sjiktLengde; i++) {
            EchogramPoint p1 = toEchogramPoint(pelagicDepthTransform, i * delSjiktLengde, layerParameters.boundary[i]);
            EchogramPoint p2 = toEchogramPoint(pelagicDepthTransform, (i + 1) * delSjiktLengde, layerParameters.boundary[i == sjiktLengde - 1 ? i : i + 1]);
            regionManager.getLayerManager().editBoundary(p1, p2, IdentityDepthTransform.INSTANCE, curveBoundary);
         }
      }

      // Remove layer between pelagic and bottom data if degenerated
      if (workRecord.bottomIsDefined == 1) {
         CurveBoundary upperBottomLayerBoundary = curveBoundaries.get(curveBoundaries.size() - 2);
         if (Arrays.equals(lowerPelagicLayerBoundary.getCurve().getDepths(), upperBottomLayerBoundary.getCurve().getDepths())) {
            regionManager.getLayerManager().mergeLayers(lowerPelagicLayerBoundary).ifError(Log.global::warning);
         }
      }

      // Species assignments
      for (int i = 0; i < workRecord.layer.length; i++) {
         WorkRecord.LayerParameters layerParameters = workRecord.layer[i];
         if (layerParameters.type == 1 && workRecord.bottomIsDefined == 0) {
            continue;
         }
         int curveBoundaryIndex = i;
         if (i == workRecord.layer.length - 1) {
            curveBoundaryIndex = curveBoundaries.size() - 2;
         }
         Layer layer = curveBoundaries.get(curveBoundaryIndex).getLayerBelow();
         if (layer != null) {
            setSpeciesInterpretation(pingFile, layer, layerParameters.speciesDensity);
         }
      }
   }

   private void setSpeciesInterpretation(PingFile pingFile, Region region, float[] speciesDensity) {
      WorkRecord workRecord = pingFile.getWorkRecord();
      if (workRecord == null) {
         return;
      }
      if (workRecord.isScrutinized == 0) {
         // reset the interpretation for the region
         region.getInterpretation().resetInterpretation(pingFile.getChannel());
      } else {
         ChannelInterpretation channelInterpretation = region.getChannelInterpretation(pingFile.getChannel());
         int speciesNumber = 0;
         for (WorkRecord.SpeciesParameters speciesParameters : workRecord.speciesSubset) {
            int beiSpecies = speciesParameters.speciesCode;
            int lsssSpecies = speciesConverter.beiToLSSS(beiSpecies);
            if (lsssSpecies == -1) {
               Log.global.warning("Could not find species mapping for BEI species number " + beiSpecies + "\n" +
                     "Mapping to default species.");
               lsssSpecies = defaultSpecies;
            }
            channelInterpretation.setAssignment(lsssSpecies, speciesDensity[speciesNumber]);
            speciesNumber++;
         }
      }
   }

   private void convertSchools(PingFile pingFile) {
      WorkRecord workRecord = pingFile.getWorkRecord();
      if (workRecord == null) {
         return;
      }
      EK500PelagicDepthTransform pelagicDepthTransform = new EK500PelagicDepthTransform(pingFile);

      for (WorkRecord.SchoolParameters schoolParameters : workRecord.school) {
         EchogramPoint p1 = toEchogramPoint(pelagicDepthTransform, schoolParameters.xOrigo, schoolParameters.yOrigo);
         EchogramPoint p2 = toEchogramPoint(pelagicDepthTransform, schoolParameters.xOrigo + schoolParameters.xLength, schoolParameters.yOrigo + schoolParameters.yLength);
         //Schools only exist in pelagic window
         School school = regionManager.addSchool(p1, p2, IdentityDepthTransform.INSTANCE);
         if (school != null) {
            // Number is not the same as object number.
            //school.setObjectNumber(schoolParameters.Number);
            setSpeciesInterpretation(pingFile, school, schoolParameters.speciesDensity);
         }
      }
   }

   private EchogramPoint toEchogramPoint(DepthTransform depthTransform, int x, int y) {
      PingIndex pingIndex = ek500PixelToPingIndex(x);
      return new EchogramPoint(pingIndex, depthTransform.zToDepth(y, pingIndex));
   }

   private IndexRecord getIndexRecord(PingFile pingFile, PingIndex pingIndex) {
      if (pingIndex instanceof ExtrapolatedPingIndex) {
         pingIndex = dataFileSet.previousOrSame(pingIndex);
      }

      IndexRecord indexRecord = ((EK500PingIndex) pingIndex).getIndexRecords()[pingFile.getChannel() - 1];
      if (indexRecord != null) {
         return indexRecord;
      }

      // Search for an existing index record in neighbouring pings
      for (long offset = 1; offset < dataFileSet.getTotalRange().getPingCount(); offset++) {
         for (long sign = -1; sign <= 1; sign += 2) {
            PingIndex neighbouringPingIndex = dataFileSet.getPingIndexOrNull(pingIndex.getPingNumber() - sign * offset);
            if (neighbouringPingIndex instanceof EK500PingIndex ek500PingIndex) {
               indexRecord = ek500PingIndex.getIndexRecords()[pingFile.getChannel() - 1];
               if (indexRecord != null) {
                  return indexRecord;
               }
            }
         }
      }

      throw new IllegalArgumentException(pingFile + ", " + pingIndex);
   }

   private PingIndex ek500PixelToPingIndex(int ek500Pixel) {
      if (ek500Pixel == 0) {
         return dataFile.getPingRange().begin();
      } else if (ek500Pixel == WorkRecord.HORIZONTAL_PIXELS) {
         return dataFile.getPingRange().end();
      } else {
         return dataFile.getClosestPingIndex(firstVesselDistance + ek500Pixel * vesselDistancePerEK500Pixel, PingMapping.DISTANCE);
      }
   }

   /**
    * Base class for pelagic and bottom depth transforms.
    */
   private abstract class EK500DepthTransform implements DepthTransform {
      private final PingFile pingFile;

      private EK500DepthTransform(PingFile pingFile) {
         this.pingFile = pingFile;
      }

      @Override
      public PerPingDepthTransform forPing(PingIndex pingIndex) {
         IndexRecord indexRecord = getIndexRecord(pingIndex);
         return new PerPingDepthTransform() {
            @Override
            public float depthToZ(float depth) {
               return (depth - getMinDepth(indexRecord)) / getSampleDistance(indexRecord);
            }

            @Override
            public float zToDepth(float z) {
               return getMinDepth(indexRecord) + z * getSampleDistance(indexRecord);
            }
         };
      }

      private IndexRecord getIndexRecord(PingIndex pingIndex) {
         return EK500WorkConverter.this.getIndexRecord(pingFile, pingIndex);
      }

      abstract float getSampleDistance(IndexRecord indexRecord);

      abstract float getMinDepth(IndexRecord indexRecord);
   }

   /**
    * Pelagic depth transform.
    */
   private final class EK500PelagicDepthTransform extends EK500DepthTransform {
      private EK500PelagicDepthTransform(PingFile pingFile) {
         super(pingFile);
      }

      @Override
      float getSampleDistance(IndexRecord indexRecord) {
         return indexRecord.getPelagicEchogramSampleDistance();
      }

      @Override
      float getMinDepth(IndexRecord indexRecord) {
         return 0;
      }
   }

   /**
    * Bottom depth transform.
    */
   private final class EK500BottomDepthTransform extends EK500DepthTransform {
      private EK500BottomDepthTransform(PingFile pingFile) {
         super(pingFile);
      }

      @Override
      float getSampleDistance(IndexRecord indexRecord) {
         return indexRecord.getBottomEchogramSampleDistance();
      }

      @Override
      float getMinDepth(IndexRecord indexRecord) {
         return indexRecord.getBottomEchogramMinDepth();
      }
   }
}
