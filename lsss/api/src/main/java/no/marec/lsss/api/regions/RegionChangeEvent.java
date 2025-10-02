package no.marec.lsss.api.regions;

import no.marec.lsss.api.DoNotImplement;
import no.marec.lsss.api.data.PingRange;

import java.util.Collection;

/**
 * An event indicating that some regions have changed.
 */
@DoNotImplement
public interface RegionChangeEvent {
   /**
    * {@return the regions that are changed}
    */
   Collection<? extends Region> regions();

   /**
    * {@return the ping range containing the changes}
    */
   PingRange pingRange();
}
