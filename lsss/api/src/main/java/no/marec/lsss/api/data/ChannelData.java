package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;

/**
 * The sample data on one channel in one {@link Ping}.
 */
@DoNotImplement
public interface ChannelData {
   /**
    * {@return the number of samples}
    */
   int getSampleCount();

   /**
    * {@return the distance between samples}
    */
   float getSampleDistance();

   /**
    * {@return the start depth of the first sample}
    *
    * This depth is equal to <pre>{@code
    *    heave + transducer depth + offset of first sample
    * }</pre>
    */
   float getMinDepth();

   /**
    * {@return the end depth of the last sample}
    */
   float getMaxDepth();

   /**
    * Computes the index of the sample that contains a given depth.
    *
    * @param depth a depth
    * @return the index of the containing sample
    */
   int depthToContainingSampleIndex(float depth);
}
