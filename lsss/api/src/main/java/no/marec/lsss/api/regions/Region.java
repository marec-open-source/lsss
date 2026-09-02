package no.marec.lsss.api.regions;

import no.marec.lsss.api.DoNotImplement;

import java.util.Set;

/**
 * A region is a part of the echogram interpretation.
 * <p>
 * A region can be either a layer or a school.
 */
@DoNotImplement
public interface Region {
   /**
    * {@return the ID of this region}
    */
   int getId();

   /**
    * {@return the labels assigned to this region}
    */
   Set<String> getLabels();

   /**
    * Sets the labels assigned to this region.
    *
    * @param labels a set of labels
    */
   void setLabels(Set<String> labels);

   /**
    * {@return the interpretation of this region}
    */
   Interpretation getInterpretation();
}
