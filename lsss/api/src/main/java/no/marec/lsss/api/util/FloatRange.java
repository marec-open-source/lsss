package no.marec.lsss.api.util;

import no.marec.lsss.api.DoNotImplement;

/**
 * A half-open interval of float value.
 */
@DoNotImplement
public interface FloatRange {
   /**
    * {@return the start of this range, inclusively}
    */
   float begin();

   /**
    * {@return the end of this range, exclusively}
    */
   float end();
}
