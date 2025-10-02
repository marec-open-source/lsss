package no.imr.lsss.modules.broadband.pojo.tracks;

import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

import java.util.ArrayList;
import java.util.List;

/**
 * Export of a track on a ping for a channel.
 */
public final class BroadbandTrackExportPerChannel {
   public String id;
   public float nominalFrequency;
   public List<Float> minFrequency = new ArrayList<>();
   public List<Float> maxFrequency = new ArrayList<>();
   public List<Integer> numFrequencies = new ArrayList<>();
   public List<Float> peakRange = new ArrayList<>();
   public List<Float> fftDistanceBefore = new ArrayList<>();
   public List<Float> fftDistanceAfter = new ArrayList<>();
   public List<Float> alongshipAngle = new ArrayList<>();
   public List<Float> athwartshipAngle = new ArrayList<>();
   public List<Float> x = new ArrayList<>();
   public List<Float> y = new ArrayList<>();
   public List<Float> z = new ArrayList<>();
   public List<float[]> tsc = new ArrayList<>();

   public BroadbandTrackExportPerChannel(RawFileTransducer transducer) {
      id = transducer.getChannelId();
      nominalFrequency = transducer.getFrequency();
   }
}
