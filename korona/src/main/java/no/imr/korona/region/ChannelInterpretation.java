package no.imr.korona.region;

import com.google.common.collect.ImmutableMap;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.Utils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

/**
 * Interpretation of a channel.
 * todo: changes species to acoustic category
 */
public final class ChannelInterpretation implements no.marec.lsss.api.regions.ChannelInterpretation {
   private static final String XML_SPECIES_INTERPRETATION = "speciesInterpretationRep";
   private static final String XML_SPECIES = "species";
   private static final String XML_ID = "ID";
   private static final String XML_FRACTION = "fraction";

   // Using sorted map to preserve ordering when saving to XML.
   private ImmutableMap<Integer, Float> assignments = ImmutableMap.of();
   private boolean initialized;

   public ChannelInterpretation() {
   }

   @Override
   public String toString() {
      return assignments.toString();
   }

   public void reset() {
      assignments = ImmutableMap.of();
      initialized = false;
   }

   void fromXml(Element element) {
      ImmutableMap.Builder<Integer, Float> builder = ImmutableMap.builder();
      for (Element speciesElement : element.elements(XML_SPECIES)) {
         int id = Integer.parseInt(speciesElement.attributeValue(XML_ID));
         float assignment = Float.parseFloat(speciesElement.attributeValue(XML_FRACTION));
         builder.put(id, assignment);
      }
      setAssignments(builder.build());
   }

   @Nullable Element toXml() {
      if (!initialized) {
         return null;
      }

      Element rep = DocumentHelper.createElement(XML_SPECIES_INTERPRETATION);

      Map<Integer, Float> sortedAssignments; // Consistent ordering in XML
      if (assignments.size() <= 1) {
         sortedAssignments = assignments;
      } else {
         sortedAssignments = new TreeMap<>(assignments);
      }
      sortedAssignments.forEach((id, assignment) -> {
         rep.addElement(XML_SPECIES)
               .addAttribute(XML_ID, Integer.toString(id))
               .addAttribute(XML_FRACTION, Utils.toString(assignment));
      });

      return rep;
   }

   public void setAssignment(int acousticCategoryId, float assignment) {
      ImmutableMap<Integer, Float> newAssignments;
      if (assignment == 0) {
         newAssignments = ImmutableUtils.remove(assignments, acousticCategoryId);
      } else {
         newAssignments = ImmutableUtils.put(assignments, acousticCategoryId, assignment);
      }
      setAssignments(newAssignments);
   }

   public float getAssignment(int acousticCategoryId) {
      Float assignment = assignments.get(acousticCategoryId);
      return assignment != null ? assignment : 0;
   }

   public float getTotalAssignment() {
      double totalAssignment = 0;
      for (float assignment : assignments.values()) {
         totalAssignment += assignment;
      }
      return (float) totalAssignment;
   }

   @Override
   public Map<Integer, Float> getAssignments() {
      return assignments;
   }

   @Override
   public void setAssignments(Map<Integer, Float> assignments) {
      setAssignments(ImmutableMap.copyOf(assignments));
   }

   public void setAssignments(ImmutableMap<Integer, Float> assignments) {
      this.assignments = assignments;
      initialized = true;
   }

   public void setInitialized(boolean initialized) {
      this.initialized = initialized;
   }

   public boolean isInitialized() {
      return initialized;
   }

   void copyFrom(ChannelInterpretation channelInterpretation) {
      assignments = channelInterpretation.assignments; // assignments is immutable
      initialized = channelInterpretation.initialized;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof ChannelInterpretation that
            && assignments.equals(that.assignments);
   }

   @Override
   public int hashCode() {
      return assignments.hashCode();
   }
}
