package no.marec.lsss.api.modules;

import no.marec.lsss.api.DoNotImplement;

import java.util.function.Consumer;

/**
 * Access to the actual module.
 */
@DoNotImplement
public interface LsssModuleAccess {
   /**
    * Wraps an observer in a new observer that delegates to this module's thread.
    *
    * @param observer an observer
    * @param <T>      the observer value type
    * @return a new observer that will delegate to this module's thread
    */
   <T> Consumer<T> inModuleThread(Consumer<T> observer);

   /**
    * Wraps an observer in a new observer that delegates to this module's thread.
    * <p>
    * Multiple notifications to the observer might be coalesced into a single notification.
    *
    * @param observer an observer
    * @param <T>      the observer value type
    * @return a new observer that will delegate to this module's thread
    */
   <T> Consumer<T> coalescingInModuleThread(Consumer<T> observer);
}
