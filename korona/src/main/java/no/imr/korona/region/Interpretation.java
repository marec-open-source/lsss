package no.imr.korona.region;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import no.imr.korona.data.datamanager.PingContainer;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Utils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.IntStream;

/**
 * Holds the interpretation of a region for interpreted channels.
 */
public final class Interpretation implements no.marec.lsss.api.regions.Interpretation {
   private static final String XML_REST_SPECIES = "restSpecies";
   private static final String XML_SPECIES_INTERPRETATION_ALL_FREQ = "speciesInterpretationRoot";
   private static final String XML_FREQUENCY = "frequency";
   private static final String XML_CHANNEL = "channel";
   private static final String XML_ID = "ID";

   private final ImmutableList<ChannelInterpretation> channelInterpretations;
   private ImmutableSet<Integer> restSpecies = ImmutableSet.of();

   public Interpretation(int channelCount) {
      channelInterpretations = IntStream.range(0, channelCount)
            .mapToObj(_ -> new ChannelInterpretation())
            .collect(ImmutableList.toImmutableList());
   }

   public void copyFrom(Interpretation interpretation) {
      for (int i = 0; i < channelInterpretations.size(); i++) {
         ChannelInterpretation sourceChannelInterpretation = interpretation.channelInterpretations.get(i);
         channelInterpretations.get(i).copyFrom(sourceChannelInterpretation);
      }

      restSpecies = interpretation.restSpecies; // restSpecies is immutable
   }

   public boolean isEqualTo(Interpretation interpretation) {
      return channelInterpretations.equals(interpretation.channelInterpretations)
            && restSpecies.equals(interpretation.restSpecies);
   }

   public ImmutableList<ChannelInterpretation> getChannelInterpretations() {
      return channelInterpretations;
   }

   public void removeAcousticCategory(int acousticCategoryId) {
      removeRestSpecies(acousticCategoryId);
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         channelInterpretation.setAssignment(acousticCategoryId, 0);
      }
   }

   @Override
   public ChannelInterpretation getChannelInterpretation(int channel) {
      return channelInterpretations.get(channel - 1);
   }

   public void addRestSpecies(int acousticCategoryId) {
      restSpecies = ImmutableUtils.add(restSpecies, acousticCategoryId);
   }

   public void removeRestSpecies(int acousticCategoryId) {
      restSpecies = ImmutableUtils.remove(restSpecies, acousticCategoryId);
   }

   @Override
   public Set<Integer> getRestSpecies() {
      return restSpecies;
   }

   @Override
   public void setRestSpecies(Set<Integer> restSpecies) {
      setRestSpecies(ImmutableSet.copyOf(restSpecies));
   }

   public void setRestSpecies(ImmutableSet<Integer> restSpecies) {
      this.restSpecies = restSpecies;
   }

   public void updateRestInterpretation() {
      ImmutableSet<Integer> restSpecies = this.restSpecies; // Local variable in case of concurrent modifications
      if (restSpecies.isEmpty()) {
         return;
      }

      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         if (!channelInterpretation.isInitialized()) {
            continue;
         }

         float assignment = 0;
         for (Map.Entry<Integer, Float> entry : channelInterpretation.getAssignments().entrySet()) {
            if (!restSpecies.contains(entry.getKey())) {
               assignment += entry.getValue();
            }
         }

         float restAssignment = Math.max(0, 1 - assignment) / restSpecies.size();
         for (Integer acousticCategory : restSpecies) {
            channelInterpretation.setAssignment(acousticCategory, restAssignment);
         }
      }
   }

   public boolean hasAtLeastOneInitializedChannelInterpretation() {
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         if (channelInterpretation.isInitialized()) {
            return true;
         }
      }
      return false;
   }

   public boolean isCompletelyAssigned(Collection<Integer> channels) {
      for (int channel : channels) {
         ChannelInterpretation channelInterpretation = getChannelInterpretation(channel);
         if (!isCompletelyAssigned(channelInterpretation.getTotalAssignment())) {
            return false;
         }
      }
      return true;
   }

   public static boolean isCompletelyAssigned(float totalAssignment) {
      return Math.abs(totalAssignment - 1) <= 1e-5;
   }

   public static String assignmentToFullPresicionPercentString(float assignment) {
      // Round to avoid float precision issues. Math.ulp(100f) is 7.6293945E-6. So % can safely use 4 decimal places.
      return Utils.removeTrailingZeros(Utils.format("%.4f", assignment * 100));
   }

   public boolean isEmpty() {
      if (!restSpecies.isEmpty()) {
         return false;
      }
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         if (!channelInterpretation.getAssignments().isEmpty()) {
            return false;
         }
      }
      return true;
   }

   public boolean containsAcousticCategory(Integer acousticCategoryId) {
      if (restSpecies.contains(acousticCategoryId)) {
         return true;
      }
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         if (channelInterpretation.getAssignments().containsKey(acousticCategoryId)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Initializes the interpretation for a channel.
    * <p>
    * The source channel for interpretation copy is selected from the following prioritized list:
    * <ol>
    * <li>destinationChannel if the interpretation is initialized on that frequency.</li>
    * <li>The nearest lower channel for which the interpretation is initialized.</li>
    * <li>The nearest higher channel for which the interpretation is initialized.</li>
    * <li>If none of the above, then the interpretation is not initialized on any channel and is left uninitialized.</li>
    * </ol>
    *
    * @param sourceChannel      the preferred source for initialization
    * @param destinationChannel the channel to initialize
    */
   public void inheritInterpretation(int sourceChannel, int destinationChannel) {
      ChannelInterpretation destinationChannelInterpretation = getChannelInterpretation(destinationChannel);
      if (destinationChannelInterpretation.isInitialized()) {
         return;
      }

      ChannelInterpretation sourceChannelInterpretation = getChannelInterpretation(sourceChannel);

      if (!sourceChannelInterpretation.isInitialized()) {
         // If source not initialized find nearest lower frequency with initialized interpretation.
         for (int i = destinationChannel - 1; i >= 1 && !sourceChannelInterpretation.isInitialized(); i--) {
            sourceChannelInterpretation = getChannelInterpretation(i);
         }

         // If source still not initialized find nearest higher frequency with initialized interpretation.
         for (int i = destinationChannel + 1; i <= channelInterpretations.size() && !sourceChannelInterpretation.isInitialized(); i++) {
            sourceChannelInterpretation = getChannelInterpretation(i);
         }
      }

      if (sourceChannelInterpretation.isInitialized()) {
         destinationChannelInterpretation.copyFrom(sourceChannelInterpretation);
      }
   }

   public Set<Integer> getAcousticCategoryIds() {
      Set<Integer> ids = new HashSet<>();
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         ids.addAll(channelInterpretation.getAssignments().keySet());
      }
      return ids;
   }

   public void toXml(Element element, PingContainer pingContainer) {
      toXml(element, pingContainer, true);
   }

   public void toXml(Element element, PingContainer pingContainer, boolean useFrequencyAttribute) {
      restSpeciesToXml(element);
      element.add(interpretationToXml(pingContainer, useFrequencyAttribute));
   }

   public void fromXml(Element element, PingContainer pingContainer) {
      restSpeciesFromXml(element.elements(XML_REST_SPECIES));
      interpretationFromXml(element, pingContainer);
   }

   private void restSpeciesToXml(Element element) {
      Set<Integer> sortedRestSpecies; // Consistent ordering in XML
      if (restSpecies.size() <= 1) {
         sortedRestSpecies = restSpecies;
      } else {
         sortedRestSpecies = new TreeSet<>(restSpecies);
      }
      for (Integer acousticCategory : sortedRestSpecies) {
         element.addElement(XML_REST_SPECIES)
               .addAttribute(XML_ID, acousticCategory.toString());
      }
   }

   private void restSpeciesFromXml(List<Element> elements) {
      ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
      for (Element element : elements) {
         String idAttribute = element.attributeValue(XML_ID);
         if (idAttribute != null) {
            int id = Integer.parseInt(idAttribute);
            builder.add(id);
         }
      }
      restSpecies = builder.build();
   }

   private Element interpretationToXml(PingContainer pingContainer, boolean useFrequencyAttribute) {
      Element rep = DocumentHelper.createElement(XML_SPECIES_INTERPRETATION_ALL_FREQ);
      RawFileConfiguration rawFileConfiguration = pingContainer.getRawFileConfiguration();
      for (int channelIndex = 0; channelIndex < channelInterpretations.size(); channelIndex++) {
         ChannelInterpretation channelInterpretation = channelInterpretations.get(channelIndex);
         Element interpretationElement = channelInterpretation.toXml();
         if (interpretationElement != null) {
            if (useFrequencyAttribute) {
               int kHz = rawFileConfiguration.getTransducers().get(channelIndex).getKHz();
               interpretationElement.addAttribute(XML_FREQUENCY, Integer.toString(kHz));
            } else {
               int channel = channelIndex + 1;
               interpretationElement.addAttribute(XML_CHANNEL, Integer.toString(channel));
            }
            rep.add(interpretationElement);
         }
      }
      return rep;
   }

   private void interpretationFromXml(Element element, PingContainer pingContainer) {
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         channelInterpretation.reset();
      }
      Element rep = element.element(XML_SPECIES_INTERPRETATION_ALL_FREQ);
      if (rep == null) {
         return;
      }
      RawFileConfiguration rawFileConfiguration = pingContainer.getRawFileConfiguration();
      for (Element interpretationElement : rep.elements()) {
         int channel;
         String frequencyAttribute = interpretationElement.attributeValue(XML_FREQUENCY);
         if (frequencyAttribute != null) {
            int kHz = Integer.parseInt(frequencyAttribute);
            channel = rawFileConfiguration.lastChannelWithKHz(kHz);
            if (channel <= 0) {
               continue;
            }
         } else {
            channel = Integer.parseInt(interpretationElement.attributeValue(XML_CHANNEL));
         }
         getChannelInterpretation(channel).fromXml(interpretationElement);
      }
   }

   public void resetInterpretation(int channel) {
      getChannelInterpretation(channel).reset();
   }

   public void reset() {
      for (ChannelInterpretation channelInterpretation : channelInterpretations) {
         channelInterpretation.reset();
      }
      restSpecies = ImmutableSet.of();
   }
}
