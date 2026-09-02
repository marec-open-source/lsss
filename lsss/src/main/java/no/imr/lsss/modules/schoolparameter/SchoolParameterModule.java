package no.imr.lsss.modules.schoolparameter;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.util.DataUtils;
import no.imr.korona.region.RegionManager;
import no.imr.korona.region.School;
import no.imr.korona.region.SchoolParameters;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.schoolparameter.morphological.AreaParameterCollection;
import no.imr.lsss.modules.schoolparameter.morphological.CircumferenceParameterCollection;
import no.imr.lsss.modules.schoolparameter.morphological.DepthParameterCollection;
import no.imr.lsss.modules.schoolparameter.morphological.MorphologicalParameterCollection;
import no.imr.lsss.modules.schoolparameter.morphological.SizeParameterCollection;
import no.imr.lsss.modules.schoolparameter.perchannel.HistogramParameterCollection;
import no.imr.lsss.modules.schoolparameter.perchannel.MomentsParameterCollection;
import no.imr.lsss.modules.schoolparameter.perchannel.PerChannelComputer;
import no.imr.lsss.modules.schoolparameter.perchannel.PerChannelParameterCollection;
import no.imr.lsss.modules.schoolparameter.perchannel.SaParameterCollection;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.CoalescingExecutor;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.ObservingExecutor;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.FloatRange;
import no.imr.tools.range.FloatRangeSet;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class SchoolParameterModule extends BaseDataModule {
   private final Map<String, SchoolParameter> schoolParameters;
   private final List<MorphologicalParameterCollection> morphologicalParameterCollections;
   private final List<PerChannelParameterCollection> perChannelParameterCollections;

   private final Set<School> queue = ConcurrentHashMap.newKeySet();
   private final CoalescingExecutor coalescingExecutor;

   private final ArgChangeManager<School> changeManager = new ArgChangeManager<>();

   public SchoolParameterModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      coalescingExecutor = new CoalescingExecutor(new ObservingExecutor(Exec.CACHED_THREAD_POOL, getInterpretationSettings().getExecutorObservation()));

      morphologicalParameterCollections = List.of(
            new AreaParameterCollection(),
            new CircumferenceParameterCollection(),
            new DepthParameterCollection(),
            new SizeParameterCollection(getLSSS())
      );

      perChannelParameterCollections = List.of(
            new HistogramParameterCollection(),
            new MomentsParameterCollection(),
            new SaParameterCollection()
      );

      schoolParameters = Stream.of(morphologicalParameterCollections, perChannelParameterCollections)
            .flatMap(Collection::stream)
            .flatMap(schoolParameterCollection -> schoolParameterCollection.getParameters().stream())
            .collect(Collectors.toUnmodifiableMap(SchoolParameter::getPersistentName, Function.identity()));
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      Listener restartListener = newCoalescingExecListener(() -> {
         queue.clear();
         if (getInterpretationSettings().getWorkFilesLoading().getValue()) {
            return;
         }
         if (getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().computeSchoolParameters.getBooleanValue()) {
            getRegionManager().getSchoolManager().getSchools().stream()
                  .filter(school -> !school.getParameters().isUpToDate())
                  .forEach(queue::add);
            computeNext();
         } else {
            getRegionManager().getSchoolManager().getSchools().forEach(School::invalidateParameters);
         }
      });
      registry.add(restartListener, List.of(
            getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().computeSchoolParameters,
            getInterpretationSettings().getWorkFilesLoading()
      ));
      Listener recomputeListener = newCoalescingExecListener(() -> {
         getRegionManager().getSchoolManager().getSchools().forEach(School::invalidateParameters);
         restartListener.listen();
      });
      registry.add(recomputeListener, List.of(
            getConfigurationManager().getSurveyMiscConf().mainFrequency, // Used by corrected length and height
            getConfigurationManager().getSurveyConfiguration().getSurveyMiscConf().recomputeSchoolParameters
      ));
      registry.add(getRegionManager().getRegionDefinitionChangeManager(), regionEvent -> {
         if (getInterpretationSettings().getWorkFilesLoading().getValue()) {
            return;
         }
         executeIfEnabled(() -> {
            Utils.getAllOfType(regionEvent.regions(), School.class).forEach(school -> {
               school.invalidateParameters();
               queue.add(school);
            });
            computeNext();
         });
      });
      registry.add(getRegionManager().getRegionDeletedChangeManager(), newExecListener(regions -> {
         Utils.getAllOfType(regions, School.class).forEach(queue::remove);
      }));
   }

   public ArgChangeManager<School> getChangeManager() {
      return changeManager;
   }

   public Map<String, SchoolParameter> getSchoolParameters() {
      return schoolParameters;
   }

   public List<PerChannelParameterCollection> getPerChannelParameterCollections() {
      return perChannelParameterCollections;
   }

   private void computeNext() {
      if (!getConfigurationManager().getSurveyMiscConf().computeSchoolParameters.getBooleanValue()) {
         return;
      }
      coalescingExecutor.execute(() -> {
         queue.stream()
               .min(Comparator.comparing(school -> school.getPingRange().begin()))
               .ifPresent(school -> {
                  queue.remove(school);
                  compute(school);
                  computeNext();
               });
      });
   }

   void compute(School school) {
      RegionManager regionManager = getRegionManager();
      DataFileSet dataFileSet = getInterpretationSettings().getDataFileSet();

      Map<String, Float> values = new TreeMap<>();
      morphologicalParameterCollections.forEach(parameterCollection -> {
         values.putAll(parameterCollection.computeValues(dataFileSet, regionManager, school));
      });

      int transducerCount = dataFileSet.getRawFileConfiguration().getTransducerCount();
      Map<Integer, List<PerChannelComputer>> perChannelComputers = createPerChannelComputers(perChannelParameterCollections, transducerCount);
      PingRange pingRange = school.getPingRange();
      dataFileSet.getPingIndices(pingRange).forEach(pingIndex -> {
         Ping ping = dataFileSet.getPing(pingIndex);
         double pingWidthMeters = DataUtils.getPingWidthMeters(dataFileSet, pingIndex);
         FloatRangeSet depthRanges = regionManager.getExclusionManager().isExcluded(pingIndex)
               ? FloatRangeSet.of()
               : regionManager.getNonMaskedRegionDepthRanges(school, pingIndex);
         // Cannot shortcut if excluded or depthRanges is empty. Sa needs to accumulate distance.
         IntStream.rangeClosed(1, transducerCount).forEach(channel -> {
            FloatRangeSet maskedDepthRanges = regionManager.getMaskingManager().getMask(channel).get(pingIndex);
            List<FloatRange> perChannelDepthRanges = depthRanges.subtract(maskedDepthRanges).getFloatRanges();
            perChannelComputers.get(channel).forEach(computer -> {
               computer.accumulate(ping, pingWidthMeters, channel, perChannelDepthRanges);
            });
         });
      });
      ImmutableMap<Integer, ImmutableMap<String, Float>> perChannelValues = collectPerChannelValues(perChannelComputers);

      school.setParameters(new SchoolParameters(
            getLSSS().getDataSetManager().getSelectedDataType() == DataType.PROCESSED,
            ImmutableMap.copyOf(values),
            perChannelValues));

      changeManager.notifyListeners(school);
   }

   private static Map<Integer, List<PerChannelComputer>> createPerChannelComputers(List<PerChannelParameterCollection> perChannelParameterCollections, int transducerCount) {
      Map<Integer, List<PerChannelComputer>> computers = HashMap.newHashMap(transducerCount);
      for (int channel = 1; channel <= transducerCount; channel++) {
         List<PerChannelComputer> perChannelComputers = perChannelParameterCollections.stream()
               .map(PerChannelParameterCollection::createComputer)
               .toList();
         computers.put(channel, perChannelComputers);
      }
      return computers;
   }

   private static ImmutableMap<Integer, ImmutableMap<String, Float>> collectPerChannelValues(Map<Integer, List<PerChannelComputer>> perChannelComputers) {
      ImmutableMap.Builder<Integer, ImmutableMap<String, Float>> perChannelValues = ImmutableMap.builder();
      perChannelComputers.forEach((channel, computers) -> {
         Map<String, Float> values = new TreeMap<>();
         computers.forEach(computer -> values.putAll(computer.getValues()));
         perChannelValues.put(channel, ImmutableMap.copyOf(values));
      });
      return perChannelValues.build();
   }
}
