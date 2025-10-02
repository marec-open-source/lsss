package no.marec.lsss.api.regions;

import no.marec.lsss.api.DoNotImplement;

import java.util.Set;

/**
 * The assignment to species for a {@link Region}.
 */
@DoNotImplement
public interface Interpretation {
   /**
    * {@return interpretation on the specified channel}
    *
    * @param channel a channel number
    */
   ChannelInterpretation getChannelInterpretation(int channel);

   /**
    * {@return an immutable set of the rest species}
    */
   Set<Integer> getRestSpecies();

   /**
    * Sets the rest species.
    * <p>
    * NB: Call {@link Regions#finishedEditingInterpretations()} when done editing interpretations.
    *
    * @param restSpecies the rest species
    */
   void setRestSpecies(Set<Integer> restSpecies);
}
