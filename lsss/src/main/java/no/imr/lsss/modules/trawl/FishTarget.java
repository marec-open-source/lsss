package no.imr.lsss.modules.trawl;

import no.imr.lsss.modules.trawl.biotic.pojo.BioticCatchSample;
import no.imr.lsss.modules.trawl.spd.SpdTarget;
import no.imr.lsss.modules.trawl.spd.TLine;
import no.imr.lsss.modules.trawl.spd.ULine;
import no.imr.tools.math.Histogram1D;
import no.imr.tools.math.NiceNumber;
import no.imr.tools.math.WelfordsMethod;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeBuilder;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYIntervalSeries;
import org.jfree.data.xy.XYIntervalSeriesCollection;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class FishTarget {
   final String speciesName;
   private final int speciePartNo;
   private final int catchNumber;
   private final boolean zeroGroup;
   private final int lengthSampleNo;
   private final double weight;
   private final List<FishIndividual> individuals;
   private final @Nullable SaSpecies saSpecies;

   FishTarget(SpdTarget target) {
      TLine tLine = target.tLine();
      speciesName = tLine.speciesName.trim();
      speciePartNo = tLine.speciePartNo;
      catchNumber = tLine.catchNumber;
      zeroGroup = tLine.group.trim().equals("10");
      lengthSampleNo = tLine.lengthSampleNo;
      weight = tLine.getWeight();

      individuals = new ArrayList<>();
      for (ULine uLine : target.uLines()) {
         FishSex sex = FishSex.of(uLine.sex);
         double cmPerLengthUnit = uLine.getCmPerLengthUnit();
         double minLengthGroup = uLine.minLengthGroup * cmPerLengthUnit;
         double lengthInterval = uLine.getLengthIntervalCm();

         int[] counts = uLine.getFishCounts();
         for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            if (count > 0) {
               float lengthCm = (float) (minLengthGroup + (i + 0.5) * lengthInterval);
               float lengthMeter = lengthCm / 100;
               for (int j = 0; j < count; j++) {
                  individuals.add(new FishIndividual(sex, lengthMeter, Float.NaN));
               }
            }
         }
      }

      saSpecies = SaData.getInstance().getSaSpecies(speciesName);
   }

   FishTarget(BioticCatchSample catchSample) {
      speciesName = catchSample.commonname.toUpperCase(Locale.ENGLISH);
      speciePartNo = catchSample.catchpartnumber;
      catchNumber = catchSample.catchcount;
      zeroGroup = catchSample.group.equals("10");
      lengthSampleNo = catchSample.lengthsamplecount;
      weight = catchSample.catchweight;

      individuals = catchSample.individuals.stream()
            .map(FishIndividual::new)
            .toList();

      saSpecies = SaData.getInstance().getSaSpecies(speciesName);
   }

   @Override
   public String toString() {
      return speciesName + ", " + individuals.size();
   }

   List<FishIndividual> getIndividuals() {
      return individuals;
   }

   String getCatchName(Language language) {
      String sexes = individuals.stream()
            .map(FishIndividual::sex)
            .filter(sex -> sex != FishSex.UNSPECIFIED)
            .map(sex -> String.valueOf(sex.symbol))
            .distinct()
            .sorted()
            .collect(Collectors.joining());
      String id = sexes.isEmpty()
            ? " " + "#" + speciePartNo
            : " " + sexes + " " + speciePartNo;
      String name = NameTranslation.getInstance().translate(language, speciesName);
      if (is0Group()) {
         return name + id + "(" + catchNumber + ")";
      } else {
         return name + id + ":" + catchNumber;
      }
   }

   @Nullable SaSpecies getSaSpecies() {
      return saSpecies;
   }

   double getWeight() {
      return weight;
   }

   IntervalXYDataset getFishLengthDataset(Language language) {
      XYIntervalSeriesCollection dataset = new XYIntervalSeriesCollection();
      Set<FishSex> sexes = EnumSet.noneOf(FishSex.class);
      FloatRangeBuilder lengthRangeBuilder = new FloatRangeBuilder();
      for (FishIndividual individual : individuals) {
         sexes.add(individual.sex());
         lengthRangeBuilder.expand(individual.lengthCm());
      }
      FloatRange lengthRange = lengthRangeBuilder.toFloatRange().expandToIncludeMax();

      float[] lengths = new float[individuals.size()];
      for (int i = 0; i < individuals.size(); i++) {
         lengths[i] = individuals.get(i).lengthCm();
      }
      Arrays.sort(lengths);
      float minSizeDiff = Float.POSITIVE_INFINITY;
      for (int i = 1; i < lengths.length; i++) {
         float sizeDiff = lengths[i] - lengths[i - 1];
         if (sizeDiff > 0) {
            minSizeDiff = Math.min(minSizeDiff, sizeDiff);
         }
      }
      float delta = Math.max(minSizeDiff, 0.5f);
      delta = (float) NiceNumber.niceNumber(delta, true);
      int[] totalCounts = Histogram1D.fromDelta(lengthRange, delta).getCounts();

      int k = 0;
      for (FishSex sex : sexes) {
         Histogram1D histogram = Histogram1D.fromDelta(lengthRange, delta);
         for (FishIndividual individual : individuals) {
            if (individual.sex() == sex) {
               histogram.addValue(individual.lengthCm());
            }
         }
         StringBuilder name = new StringBuilder();
         if (k == 0) {
            name.append(NameTranslation.getInstance().translate(language, speciesName));
         }
         if (sex != FishSex.UNSPECIFIED) {
            name.append(' ').append(sex.string);
         }
         if (k == sexes.size() - 1) {
            name.append(" [cm]");
         }
         k++;
         XYIntervalSeries series = new XYIntervalSeries(name.toString());
         int[] counts = histogram.getCounts();
         for (int i = 0; i < counts.length; i++) {
            int count = counts[i];
            double x = histogram.indexToValue(i);
            int totalCount = totalCounts[i];
            series.add(x + delta / 2, x, x + delta, totalCount + count, totalCount, totalCount + count);
            totalCounts[i] += count;
         }
         dataset.addSeries(series);
      }
      return dataset;
   }

   WelfordsMethod getFishLengths() {
      WelfordsMethod welfordsMethod = new WelfordsMethod();
      for (FishIndividual individual : individuals) {
         welfordsMethod.update(individual.lengthCm());
      }
      return welfordsMethod;
   }

   double getSa(double tsThreshold) {
      return switch (saSpecies) {
         case null -> {
            yield 0;
         }
         case SaSpecies.SaPlankton saPlankton -> {
            if (saPlankton.getTS() >= tsThreshold) {
               double scaleFactor = saPlankton.getWeightNumberConstant() * weight;
               yield scaleFactor * saPlankton.getSigma() * 1852.0 / saPlankton.getSweepWidth();
            } else {
               yield 0;
            }
         }
         case SaSpecies.SaFish saFish -> {
            double sigma = 0;
            for (FishIndividual individual : individuals) {
               float length = individual.lengthCm();
               if (saFish.getTS(length) >= tsThreshold) {
                  sigma += saFish.getSigma(length);
               }
            }
            double scaleFactor = (double) catchNumber / (double) lengthSampleNo;
            yield scaleFactor * sigma * 1852.0 / saFish.getSweepWidth();
         }
      };
   }

   boolean isPlankton() {
      return saSpecies instanceof SaSpecies.SaPlankton;
   }

   boolean is0Group() {
      return zeroGroup;
   }
}
