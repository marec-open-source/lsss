package no.marec.lsss.api.data;

import no.marec.lsss.api.DoNotImplement;

/**
 * A half-open interval of {@link PingIndex}es.
 */
@DoNotImplement
public interface PingRange {
   /**
    * {@return the start of this range, inclusively}
    */
   PingIndex begin();

   /**
    * {@return the end of this range, exclusively}
    */
   PingIndex end();
}
