package no.marec.lsss.api.util;

import no.marec.lsss.api.DoNotImplement;

/**
 * A handle given to an async task.
 */
@DoNotImplement
public interface AsyncHandle {
   /**
    * {@return <code>true</code> if cancelled}
    */
   boolean isCancelled();
}
