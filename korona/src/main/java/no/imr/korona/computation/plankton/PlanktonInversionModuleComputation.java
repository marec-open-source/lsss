package no.imr.korona.computation.plankton;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.computation.plankton.models.BackscatterModel;
import no.imr.korona.data.datagrams.Pic0Datagram;
import no.imr.korona.data.datagrams.Pid0Datagram;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingSource;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BooleanParameter;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class PlanktonInversionModuleComputation extends GeneralPingModuleComputation {
   private final PlanktonInversionModule module;
   private final Set<Integer> channelToUse = new HashSet<>();
   private final PlanktonFile planktonFile;
   private final SingleModelInverter singleModelInverter;
   private final Set<Byte> excludedCategoryNumbers = new HashSet<>();
   private final Deque<Ping> pingOutputQueue = new ArrayDeque<>();
   private final int mainChannel;
   private float lastSampleInterval;
   private final RawFileConfiguration rawFileConfiguration;

   PlanktonInversionModuleComputation(PlanktonInversionModule module, ComputationContext computationContext, PingSource pingSource) throws IOException {
      super(module, computationContext, pingSource);

      this.module = module;

      PingConfiguration pingConfiguration = pingSource.getPingConfiguration();
      PingConfiguration newPingConfiguration = pingConfiguration.createCopy();

      rawFileConfiguration = newPingConfiguration.getRawFileConfiguration();
      int mainKHz = 38;
      int mainChannelCandidate = rawFileConfiguration.lastChannelWithKHz(mainKHz);
      if (mainChannelCandidate > 0) {
         mainChannel = mainChannelCandidate;
      } else {
         mainChannel = 1;
         List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
         if (transducers.isEmpty()) {
            Log.global.warning("No data");
         } else {
            Log.global.warning(mainKHz + " kHz is not available, instead using channel " + mainChannel
                  + " with " + transducers.getFirst().getKHz() + " kHz");
         }
      }

      module.updateParameters(rawFileConfiguration);

      updateChannelToUse();

      Path file = module.getRequiredConfigFile(PlanktonFileService.NAME);
      planktonFile = new PlanktonFile(file);

      singleModelInverter = configureInverter();

      updateExcludedCategories();

      Pic0Datagram pic0Datagram = createPic0Datagram(newPingConfiguration.getRawFileConfiguration().getNTDate());
      newPingConfiguration.getConfigurationItems().add(pic0Datagram);
      setNewPingConfiguration(newPingConfiguration);
   }

   private void updateExcludedCategories() {
      excludedCategoryNumbers.clear();

      Configurator configurator = new Configurator(getComputationContext().getModuleContainer().getConfigFileSettings(), null);

      for (BooleanParameter categoryToggleParameter : module.categoriesUsedForInversion.getParameters()) {
         Category category = configurator.getCategory(categoryToggleParameter.getPersistentName());
         if (!categoryToggleParameter.getBooleanValue() && category != null) {
            excludedCategoryNumbers.add(category.getNumber());
         }
      }
   }

   @Override
   protected void convertPing(Ping ping, Ping newPing) {
      newPing.addAll(ping.getPingItems());
   }

   private void updateChannelToUse() {
      channelToUse.clear();

      Set<Integer> activeKHz = new HashSet<>();
      for (PlanktonInversionModule.FrequencyParameter parameter : module.activeFrequencies.getParameters()) {
         if (parameter.getBooleanValue()) {
            activeKHz.add(parameter.getKHz());
         }
      }

      for (int channel = 1; channel <= rawFileConfiguration.getTransducers().size(); channel++) {
         RawFileTransducer transducer = rawFileConfiguration.getTransducers().get(channel - 1);
         if (module.useAllFrequencies.getBooleanValue() || activeKHz.contains(transducer.getKHz())) {
            channelToUse.add(channel);
         }
      }
   }

   private SingleModelInverter configureInverter() {
      SingleModelInverter.InversionParameters inversionParameters = new SingleModelInverter.InversionParameters(
            module.minResidualErrorThreshold.getDoubleValue(),
            module.maxResidualErrorThreshold.getDoubleValue(),
            module.levenbergMarquardtFactor.getDoubleValue(),
            module.maxIter.getIntValue()
      );

      float[] frequencies = new float[channelToUse.size()];
      int freqNum = 0;
      for (int channel : channelToUse) {
         frequencies[freqNum] = rawFileConfiguration.getTransducers().get(channel - 1).getFrequency();
         freqNum++;
      }
      return new SingleModelInverter(inversionParameters, frequencies, module.getSelectedScatterers());
   }

   private Pic0Datagram createPic0Datagram(long ntDate) {
      Pic0Datagram pic0 = new Pic0Datagram(ntDate);

      for (PlanktonScatterer<?> planktonScatterer : module.getSelectedScatterers()) {
         pic0.addCategory(planktonScatterer.getPlanktonCategory());
      }

      return pic0;
   }

   private void createPid0Datagrams() {
      //possibly split output queue if the sample interval changes
      List<List<Ping>> outputQueues = new ArrayList<>();
      List<Ping> nextList = new ArrayList<>();
      PowerData firstPowerData = getReferenceDatagram(pingOutputQueue.getFirst());
      float sampleInterval = firstPowerData != null ? firstPowerData.getSampleInterval() : lastSampleInterval;
      for (Ping ping : pingOutputQueue) {
         PowerData powerData = getReferenceDatagram(ping);
         if (powerData == null) {
            nextList.add(ping);
            continue;
         }
         if (powerData.getSampleInterval() == sampleInterval) {
            nextList.add(ping);
         } else {
            outputQueues.add(nextList);
            nextList = new ArrayList<>();
            sampleInterval = powerData.getSampleInterval();
            lastSampleInterval = sampleInterval;
            nextList.add(ping);
         }
      }
      outputQueues.add(nextList);
      for (List<Ping> pings : outputQueues) {
         processOutputQueue(pings);
      }
   }

   private @Nullable PowerData getReferenceDatagram(Ping ping) {
      PowerData powerData = ping.getPowerData(mainChannel);
      return powerData != null ? powerData : ping.getNonNullPowerData();
   }

   private int getDepthSamplesPerBin(PowerData referencePowerData) {
      return switch (module.verticalUnit.getValue()) {
         case SAMPLES -> {
            yield module.depthSamplesPerBin.getIntValue();
         }
         case DURATION -> {
            float sampleIntervalMilliseconds = referencePowerData.getSampleInterval() * 1000;
            yield Math.max(1, Math.round(module.depthDurationPerBin.getValue() / sampleIntervalMilliseconds));
         }
         case DISTANCE -> {
            float sampleDistance = referencePowerData.getSampleDistance();
            yield Math.max(1, Math.round(module.depthDistancePerBin.getValue() / sampleDistance));
         }
      };
   }

   private @Nullable PowerData getReferencePowerData(List<Ping> pings) {
      for (Ping ping : pings) {
         PowerData referenceDatagram = getReferenceDatagram(ping);
         if (referenceDatagram != null) {
            return referenceDatagram;
         }
      }
      return null;
   }

   private void processOutputQueue(List<Ping> pings) {
      PowerData referencePowerData = getReferencePowerData(pings);
      if (referencePowerData == null) {
         return; //nothing to be done
      }
      PingSubSampler subSampler = new PingSubSampler(pings, mainChannel, rawFileConfiguration,
            excludedCategoryNumbers, getDepthSamplesPerBin(referencePowerData), channelToUse, module.useNoiseThreshold.getBooleanValue(),
            module.noiseThreshold.getFloatValue(), module.useMinInversionDepth.getBooleanValue(), module.minInversionDepth.getFloatValue(),
            module.useMaxInversionDepth.getBooleanValue(), module.maxInversionDepth.getFloatValue());
      List<Pid0Datagram> pid0List = new ArrayList<>(pingOutputQueue.size());
      for (Ping ping : pings) {
         PowerData referenceDatagram = getReferenceDatagram(ping);
         if (referenceDatagram == null) {
            continue;
         }
         Pid0Datagram pid0 = new Pid0Datagram(referenceDatagram, ping.getNTDate());
         pid0List.add(pid0);
         ping.add(pid0);
      }

      // Update initial size map for this time
      for (PlanktonScatterer<? extends BackscatterModel> planktonScatterer : module.getSelectedScatterers()) {
         planktonScatterer.setInitialSizeHistogramMap(planktonFile.getDepthMap(planktonScatterer.getPlanktonCategory().getLegend(), subSampler.getCenterMillis()));
      }

      // todo: Optimize: The rest of this function could be parallelized.

      for (PingSubSampler.SubSampledBin subSampledBin : subSampler) {
         byte id = Pic0Datagram.getFillerCategory().getNumber();
         double[] binDividers = Utils.EMPTY_DOUBLE_ARRAY;
         double[] abundances = Utils.EMPTY_DOUBLE_ARRAY;
         double residual = module.maxResidualErrorThreshold.getDoubleValue();
         if (!module.getSelectedScatterers().isEmpty()) {
            SingleModelInverter.ScattererInversion scattererInversion = singleModelInverter.invert(subSampledBin.getSubSampledSvValues(), subSampledBin.getValidSubSampledSvValues(), subSampledBin.getDepth());
            if (scattererInversion != null) {
               id = scattererInversion.getPlanktonScatterer().getPlanktonCategory().getNumber();
               binDividers = scattererInversion.getSizeHistogram().getDividers();
               abundances = scattererInversion.getSizeHistogram().getAbundances();
               residual = scattererInversion.getResidualError();
            } else {
               id = Pic0Datagram.getNotPlanktonCategory().getNumber();
            }
         }

         Pid0Datagram.LengthDistribution lengthDistribution = new Pid0Datagram.LengthDistribution(binDividers, abundances);

         // Fill the plankton id and the corresponding length distribution into the pid0 datagrams.
         fillIntoPid0Datagrams(pid0List, subSampledBin, id, lengthDistribution, (float) residual);
      }
   }

   private static void fillIntoPid0Datagrams(List<Pid0Datagram> pid0Datagrams, PingSubSampler.SubSampledBin subSampledBin,
                                             byte id, Pid0Datagram.LengthDistribution lengthDistribution, float residual) {
      int pingNo = 0;
      for (Pid0Datagram pid0Datagram : pid0Datagrams) {
         boolean wasValid = false;
         int index = subSampledBin.getStartSample();

         while (index < subSampledBin.getEndSample()) {
            boolean isValid = subSampledBin.isValid(pingNo, index);
            if (index == subSampledBin.getStartSample() || isValid != wasValid) {
               byte currentId;
               if (!isValid) {
                  currentId = Pic0Datagram.getFillerCategory().getNumber();
               } else {
                  currentId = id;
               }
               wasValid = isValid;

               Pid0Datagram.PlanktonData planktonData = new Pid0Datagram.PlanktonData(currentId, lengthDistribution, 1.0f, residual);

               pid0Datagram.setCategorySamples(index, List.of(planktonData));
            }
            index++;
         }
         pid0Datagram.setCount(index);
         pingNo++;
      }
   }

   @Override
   protected @Nullable Ping generateOutput() throws IOException {
      if (pingOutputQueue.isEmpty()) {
         // Build up output queue until pingOutputQueue.size() == pingsPerBin. (or next datagram == null)
         // Create a sub-sampled array of sv-values and perform plankton inversion.
         // Add plankton inversion datagram to pings.
         while (pingOutputQueue.size() < module.pingsPerBin.getIntValue()) {
            if (getAsyncHandle().isCancelled()) {
               return null;
            }
            Ping ping = inputPing();
            if (ping == null) {
               break;
            }
            pingOutputQueue.addLast(ping);
         }
         if (!pingOutputQueue.isEmpty()) {
            createPid0Datagrams();
         }
      }

      return pingOutputQueue.pollFirst();
   }
}
