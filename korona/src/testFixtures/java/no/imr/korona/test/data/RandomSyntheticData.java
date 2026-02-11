package no.imr.korona.test.data;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.RandomUtils;
import no.imr.tools.Utils;
import no.imr.tools.annotations.ReflectionEntryPoint;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * As file: RandomSyntheticData@1-1000.lsss-ss.
 */
public final class RandomSyntheticData extends SyntheticData {
   private static final String SEED = "seed";

   private final Map<PingIndex, Map<Integer, RawInfo>> rawInfos = new ConcurrentHashMap<>();
   private long seed;

   private float[] frequencies = new float[0];
   private TransducerDepthType transducerDepthType = TransducerDepthType.CONSTANT_7_5;
   private CountType countType = CountType.CONSTANT_1000;
   private RawDataType rawDataType = RawDataType.NICE;
   private HasRawType hasRawType = HasRawType.ALWAYS;

   @ReflectionEntryPoint
   public RandomSyntheticData() {
      this(RandomUtils.newSeed());
   }

   public RandomSyntheticData(long seed) {
      setSeed(seed);
   }

   private void setSeed(long seed) {
      this.seed = seed;
      Random random = new Random(seed);
      List<Float> allFrequencies = IntStream.rangeClosed(1, super.getTransducerCount()).mapToObj(super::getFrequency).toList();
      List<Float> selectedFrequencies = RandomUtils.stream(random, allFrequencies, random.nextInt(allFrequencies.size() + 1)).collect(Collectors.toList());
      Collections.shuffle(selectedFrequencies, random);
      frequencies = Utils.toFloats(selectedFrequencies);
      transducerDepthType = RandomUtils.get(random, TransducerDepthType.values());
      countType = RandomUtils.get(random, CountType.values());
      rawDataType = RandomUtils.get(random, RawDataType.values());
      hasRawType = RandomUtils.get(random, HasRawType.values());
      rawInfos.clear();
   }

   @Override
   protected int getTransducerCount() {
      return frequencies.length;
   }

   @Override
   protected float getFrequency(int channel) {
      return frequencies[channel - 1];
   }

   @Override
   protected void addParameters(Map<String, String> parameters) {
      super.addParameters(parameters);
      parameters.put(SEED, Long.toString(seed));
   }

   @Override
   protected void setParameter(String name, String value) {
      if (name.equals(SEED)) {
         setSeed(Long.parseLong(value));
      } else {
         super.setParameter(name, value);
      }
   }

   @Override
   protected float getTransducerDepth(PingIndex pingIndex, int channel) {
      return getRawInfo(pingIndex, channel).transducerDepth;
   }

   @Override
   protected void defineSampleValues(PowerData powerData, PingIndex pingIndex) {
      RawInfo rawInfo = getRawInfo(pingIndex, powerData.getChannel());
      rawDataType.defineData(new Random(rawInfo.rawDataSeed), powerData, rawInfo);
   }

   @Override
   protected boolean hasPowerData(PingIndex pingIndex, int channel) {
      return getRawInfo(pingIndex, channel).hasRaw;
   }

   private RawInfo getRawInfo(PingIndex pingIndex, int channel) {
      Map<Integer, RawInfo> map = rawInfos.computeIfAbsent(pingIndex, _ -> new ConcurrentHashMap<>());

      RawInfo rawInfo = map.get(channel);
      if (rawInfo == null) {
         rawInfo = createRawInfo(pingIndex, channel);
         map.put(channel, rawInfo);
      }

      return rawInfo;
   }

   private RawInfo createRawInfo(PingIndex pingIndex, int channel) {
      Random random = new Random(seed ^ pingIndex.getPingNumber() * 10 + channel);
      return new RawInfo(
            hasRawType.hasRaw(random),
            countType.getCount(random),
            transducerDepthType.getTransducerDepth(random),
            random.nextLong()
      );
   }

   private record RawInfo(
         boolean hasRaw,
         int count,
         float transducerDepth,
         long rawDataSeed) {
   }

   private enum HasRawType {
      ALWAYS,
      NEVER,
      RANDOM;

      private boolean hasRaw(Random random) {
         return switch (this) {
            case ALWAYS -> true;
            case NEVER -> false;
            case RANDOM -> random.nextBoolean();
         };
      }
   }

   private enum CountType {
      CONSTANT_1000,
      RANDOM_10000;

      private int getCount(Random random) {
         return switch (this) {
            case CONSTANT_1000 -> 1000;
            case RANDOM_10000 -> random.nextInt(10000);
         };
      }
   }

   private enum TransducerDepthType {
      CONSTANT_7_5,
      RANDOM_25;

      private float getTransducerDepth(Random random) {
         return switch (this) {
            case CONSTANT_7_5 -> 7.5f;
            case RANDOM_25 -> random.nextFloat(25);
         };
      }
   }

   private enum RawDataType {
      NICE,
      RANDOM;

      private void defineData(Random random, PowerData powerData, RawInfo rawInfo) {
         switch (this) {
            case NICE -> {
               float[] sv = new float[rawInfo.count];
               for (int i = 0; i < sv.length; i++) {
                  sv[i] = Math.max(0, 15000 - 20 * i);
               }
               powerData.setSv(sv);
            }
            case RANDOM -> {
               float[] logSv = new float[rawInfo.count];
               for (int i = 0; i < logSv.length; i++) {
                  logSv[i] = random.nextFloat(-200, 10);
               }
               powerData.setLogSv(logSv);
            }
         }
      }
   }
}
