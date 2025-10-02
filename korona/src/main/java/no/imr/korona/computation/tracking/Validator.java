package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Track;

public interface Validator {
   boolean isValid(Track track);
}
