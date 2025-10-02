package no.marec.lsss.api.echogram;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.data.PingIndex;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * Converts between time and echogram image x-coordinate.
 */
@DoNotImplement
public interface EchogramPingTransform {
   /**
    * {@return the x-coordinate corresponding to a time}
    *
    * @param time a time
    */
   double instantToX(Instant time);

   /**
    * {@return the time corresponding to an x-coordinate}
    *
    * @param x an x-coordinate
    */
   Instant xToInstant(double x);

   /**
    * {@return the subsampled ping index containing an x-coordinate}
    * <p>
    * The returned ping index is included in {@link EchogramData#subsampledPingIndices()}.
    *
    * @param x an x-coordinate
    */
   @Nullable PingIndex xToContainingSubsampledPingIndex(double x);
}
