package no.imr.lsss.framework.export;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.region.ChannelInterpretation;
import no.imr.korona.region.Interpretation;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.util.LanguageUtils;
import no.imr.lsss.framework.export.pojo.ExportScrutiny;
import no.imr.tools.NoCanDoException;
import no.imr.tools.math.MathUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

public final class ExportUtils {
   private ExportUtils() {
   }

   public static ExportScrutiny makeScrutiny(LSSS lsss, Interpretation interpretation) {
      List<Integer> channels = IntStream.rangeClosed(1, interpretation.getChannelInterpretations().size())
            .boxed()
            .toList();
      return makeScrutiny(lsss, channels, interpretation);
   }

   public static ExportScrutiny makeScrutiny(LSSS lsss, List<Integer> channels, Interpretation interpretation) {
      LanguageUtils languageUtils = lsss.getConfigurationManager().getLanguageUtils();
      Map<Integer, AcousticCategory> acousticCategoryMap = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
      List<Integer> sortedAcousticCategoryIds = new ArrayList<>(interpretation.getAcousticCategoryIds());
      sortedAcousticCategoryIds.sort(null);
      ExportScrutiny scrutiny = new ExportScrutiny();
      for (int channel : channels) {
         ChannelInterpretation channelInterpretation = interpretation.getChannelInterpretations().get(channel - 1);
         Map<Integer, Float> assignments = channelInterpretation.getAssignments();
         if (assignments.isEmpty()) {
            continue;
         }
         List<ExportScrutiny.Category> categories = new ArrayList<>();
         for (Integer id : sortedAcousticCategoryIds) {
            Float assignment = assignments.get(id);
            if (assignment != null) {
               String initials = languageUtils.getAcCatInitials(acousticCategoryMap.get(id));
               categories.add(new ExportScrutiny.Category(id, initials, MathUtils.round(assignment, 10_000)));
            }
         }
         RawFileTransducer transducer = lsss.getDataManager().getDataFileSet().getRawFileConfiguration().getTransducers().get(channel - 1);
         scrutiny.channels.add(new ExportScrutiny.Channel(transducer.getChannelId(), categories));
      }
      Set<Integer> rest = interpretation.getRestSpecies();
      if (!rest.isEmpty()) {
         List<Integer> sortedRest = new ArrayList<>(rest);
         sortedRest.sort(null);
         for (Integer restSpeciesId : sortedRest) {
            String initials = languageUtils.getAcCatInitials(acousticCategoryMap.get(restSpeciesId));
            scrutiny.restCategories.add(new ExportScrutiny.RestCategory(restSpeciesId, initials));
         }
      }
      return scrutiny;
   }

   public static void applyScrutiny(LSSS lsss, List<Interpretation> interpretations, ExportScrutiny scrutiny) {
      Map<Integer, AcousticCategory> acousticCategoryMap = lsss.getConfigurationManager().getSurveyConfiguration().getAcousticCategoryConf().getAcousticCategoryMap();
      Map<String, Integer> idToChannel = new HashMap<>();
      List<RawFileTransducer> transducers = lsss.getDataManager().getDataFileSet().getRawFileConfiguration().getTransducers();
      for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
         idToChannel.put(transducers.get(channelIndex).getChannelId(), channelIndex + 1);
      }
      ImmutableSet<Integer> restSpecies = scrutiny.restCategories.stream()
            .map(category -> {
               if (!acousticCategoryMap.containsKey(category.id)) {
                  throw new NoCanDoException("Unknown category id: " + category.id);
               }
               return category.id;
            })
            .collect(ImmutableSet.toImmutableSet());
      for (Interpretation interpretation : interpretations) {
         interpretation.reset();
         for (ExportScrutiny.Channel channelScrutiny : scrutiny.channels) {
            Collection<Integer> channels;
            String channelId = channelScrutiny.channelId;
            if (channelId != null) {
               Integer channel = idToChannel.get(channelId);
               if (channel == null) {
                  throw new NoCanDoException("Unknown channel id: " + channelId);
               }
               channels = List.of(channel);
            } else {
               channels = idToChannel.values();
            }
            ImmutableMap.Builder<Integer, Float> assignmentsBuilder = ImmutableMap.builder();
            for (ExportScrutiny.Category category : channelScrutiny.categories) {
               if (!acousticCategoryMap.containsKey(category.id)) {
                  throw new NoCanDoException("Unknown category id: " + category.id);
               }
               if (category.assignment < 0) {
                  throw new NoCanDoException("Invalid assignment: " + category.assignment);
               }
               assignmentsBuilder.put(category.id, category.assignment);
            }
            ImmutableMap<Integer, Float> assignments = assignmentsBuilder.build();
            for (Integer channel : channels) {
               ChannelInterpretation channelInterpretation = interpretation.getChannelInterpretation(channel);
               channelInterpretation.setAssignments(assignments);
            }
         }
         interpretation.getChannelInterpretations().forEach(channelInterpretation -> channelInterpretation.setInitialized(true));
         interpretation.setRestSpecies(restSpecies);
         interpretation.updateRestInterpretation();
      }
      lsss.getRegionManager().getInterpretationChangeManager().notifyListeners(scrutiny);
   }
}
