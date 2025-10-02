package no.marec.lsss.api.regions;

import no.marec.lsss.api.DoNotImplement;

import java.util.Map;

/**
 * The species assignments on a single channel.
 */
@DoNotImplement
public interface ChannelInterpretation {
   /**
    * {@return an immutable map from species id to assignment}
    */
   Map<Integer, Float> getAssignments();

   /**
    * Sets the species assignments.
    * <p>
    * NB: Call {@link Regions#finishedEditingInterpretations()} when done editing interpretations.
    *
    * @param assignments a map from species id to assignment
    */
   void setAssignments(Map<Integer, Float> assignments);
}
