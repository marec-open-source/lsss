package no.marec.lsss.api.util.observing;

import no.marec.lsss.api.DoNotImplement;

/**
 * A subscription returned from {@link Observable#subscribe(java.util.function.Consumer)}.
 */
@DoNotImplement
public interface Subscription {
   /**
    * Cancels this subscription.
    */
   void unsubscribe();
}
