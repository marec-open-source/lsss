package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Track;

public interface Terminator {
   boolean shouldTerminate(Track track);
}
