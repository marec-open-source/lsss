package no.imr.korona.computation.tracking;

import no.imr.korona.computation.tracking.data.Track;

interface TrackingListener {
   void onNewTrackPoint(Track track);

   void onTrackTermination(Track track, boolean valid);
}
