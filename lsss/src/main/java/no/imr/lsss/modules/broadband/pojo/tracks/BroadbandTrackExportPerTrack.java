package no.imr.lsss.modules.broadband.pojo.tracks;

import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;

/**
 * Export of a track.
 */
public final class BroadbandTrackExportPerTrack {
   public ImmutableSet<String> labels = ImmutableSet.of();
   public List<Long> pingNumber = new ArrayList<>();
   public List<String> time = new ArrayList<>();
   public List<Double> longitude = new ArrayList<>();
   public List<Double> latitude = new ArrayList<>();
   public List<Float> heading = new ArrayList<>();
   public List<Float> heave = new ArrayList<>();
   public List<Float> pitch = new ArrayList<>();
   public List<Float> roll = new ArrayList<>();
   public List<Float> minDepth = new ArrayList<>();
   public List<Float> maxDepth = new ArrayList<>();
   public List<Float> peakDepth = new ArrayList<>();

   public List<BroadbandTrackExportPerChannel> channels = new ArrayList<>();

   public BroadbandTrackExportPerTrack() {
   }
}
