package no.imr.tools.concurrent;

import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.listening.Listeners;
import no.marec.lsss.api.util.observing.Subscription;

import java.util.List;
import java.util.function.Consumer;

/**
 * An object for concurrent contexts.
 * Subclasses should make sure that:
 * <ul>
 * <li>incoming calls are executed using this objects executor.</li>
 * <li>exposed data can safely be used concurrently.</li>
 * </ul>
 */
public abstract class ConcurrentObject {
   private final SerialExecutor executor;
   private boolean enabled;
   private final ArgChangeManager<Boolean> enabledChangeManager = new ArgChangeManager<>();
   private List<Subscription> whenEnabledListenerConnections = List.of();

   protected ConcurrentObject() {
      this(new SerialExecutor(Exec.FORK_JOIN_POOL));
   }

   protected ConcurrentObject(SerialExecutor executor) {
      this.executor = executor;
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      executeAlways(() -> {
         if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
               ListenerRegistry listenerRegistry = new ListenerRegistry();
               onEnable(listenerRegistry);
               whenEnabledListenerConnections = listenerRegistry.build();
            } else {
               whenEnabledListenerConnections.forEach(Subscription::unsubscribe);
               whenEnabledListenerConnections = List.of();
               onDisable();
            }
            enabledChangeManager.notifyListeners(enabled);
         }
      });
   }

   public ArgChangeManager<Boolean> getEnabledChangeManager() {
      return enabledChangeManager;
   }

   public boolean isIdle() {
      return executor.isIdle();
   }

   protected void onEnable(ListenerRegistry registry) {
   }

   protected void onDisable() {
   }

   protected void executeIfEnabled(Runnable runnable) {
      executor.execute(ifEnabled(runnable));
   }

   public void executeAlways(Runnable runnable) {
      executor.execute(runnable);
   }

   public Listener newCoalescingExecListener(Runnable listener) {
      return Listeners.coalescingInExecutor(executor, ifEnabled(listener));
   }

   public <T> Consumer<T> newCoalescingExecListener(Consumer<T> listener) {
      return Listeners.coalescingInExecutor(executor, ifEnabled(listener));
   }

   public Listener newExecListener(Runnable listener) {
      return Listeners.inExecutor(executor, ifEnabled(listener));
   }

   public <T> Consumer<T> newExecListener(Consumer<T> listener) {
      return Listeners.inExecutor(executor, ifEnabled(listener));
   }

   private Runnable ifEnabled(Runnable runnable) {
      return () -> {
         if (enabled) {
            runnable.run();
         }
      };
   }

   private <T> Consumer<T> ifEnabled(Consumer<T> listener) {
      return argument -> {
         if (enabled) {
            listener.accept(argument);
         }
      };
   }
}
