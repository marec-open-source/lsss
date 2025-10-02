package no.imr.korona.test.data;

import no.imr.korona.data.formats.synthetic.SyntheticData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.tools.RandomUtils;
import no.imr.tools.Utils;
import no.imr.tools.annotations.ReflectionEntryPoint;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
      setFirstAndLastPingNumber(1, random.nextInt(1, 11));
      List<Double> allFrequencies = Arrays.stream(Utils.toDoubles(super.getFrequencies())).boxed().toList();
      List<Double> selectedFrequencies = RandomUtils.stream(random, allFrequencies, random.nextInt(allFrequencies.size() + 1)).collect(Collectors.toList());
      Collections.shuffle(selectedFrequencies, random);
      frequencies = Utils.toFloats(selectedFrequencies);
      transducerDepthType = RandomUtils.get(random, TransducerDepthType.values());
      countType = RandomUtils.get(random, CountType.values());
      rawDataType = RandomUtils.get(random, RawDataType.values());
      hasRawType = RandomUtils.get(random, HasRawType.values());
      rawInfos.clear();
   }

   @Override
   protected float[] getFrequencies() {
      return frequencies;
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
      rawDataType.defineData(new Random(rawInfo.rawDataSeed), powerData, pingIndex, rawInfo);
   }

   @Override
   protected boolean hasPowerData(PingIndex pingIndex, int channel) {
      return getRawInfo(pingIndex, channel).hasRaw;
   }

   private RawInfo getRawInfo(PingIndex pingIndex, int channel) {
      Map<Integer, RawInfo> map = rawInfos.computeIfAbsent(pingIndex, k -> new ConcurrentHashMap<>());

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
            hasRawType.hasRaw(random, pingIndex, channel),
            countType.getCount(random, pingIndex, channel),
            transducerDepthType.getTransducerDepth(random, pingIndex, channel),
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
      ALWAYS {
         @Override
         boolean hasRaw(Random random, PingIndex pingIndex, int channel) {
            return true;
         }
      },

      NEVER {
         @Override
         boolean hasRaw(Random random, PingIndex pingIndex, int channel) {
            return false;
         }
      },

      RANDOM {
         @Override
         boolean hasRaw(Random random, PingIndex pingIndex, int channel) {
            return random.nextBoolean();
         }
      };

      abstract boolean hasRaw(Random random, PingIndex pingIndex, int channel);
   }

   private enum CountType {
      CONSTANT_1000 {
         @Override
         int getCount(Random random, PingIndex pingIndex, int channel) {
            return 1000;
         }
      },

      RANDOM_10000 {
         @Override
         int getCount(Random random, PingIndex pingIndex, int channel) {
            return random.nextInt(10000);
         }
      };

      abstract int getCount(Random random, PingIndex pingIndex, int channel);
   }

   private enum TransducerDepthType {
      CONSTANT_7_5 {
         @Override
         float getTransducerDepth(Random random, PingIndex pingIndex, int channel) {
            return 7.5f;
         }
      },

      RANDOM_25 {
         @Override
         float getTransducerDepth(Random random, PingIndex pingIndex, int channel) {
            return random.nextFloat(25);
         }
      };

      abstract float getTransducerDepth(Random random, PingIndex pingIndex, int channel);
   }

   private enum RawDataType {
      NICE {
         @Override
         void defineData(Random random, PowerData powerData, PingIndex pingIndex, RawInfo rawInfo) {
            float[] sv = new float[rawInfo.count];
            for (int i = 0; i < sv.length; i++) {
               sv[i] = Math.max(0, 15000 - 20 * i);
            }
            powerData.setSv(sv);
         }
      },

      RANDOM {
         @Override
         void defineData(Random random, PowerData powerData, PingIndex pingIndex, RawInfo rawInfo) {
            float[] logSv = new float[rawInfo.count];
            for (int i = 0; i < logSv.length; i++) {
               logSv[i] = random.nextFloat(-200, 10);
            }
            powerData.setLogSv(logSv);
         }
      };

      abstract void defineData(Random random, PowerData powerData, PingIndex pingIndex, RawInfo rawInfo);
   }
}
