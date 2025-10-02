package no.imr.korona.computation;

import no.imr.korona.data.ping.PingBuffering;
import no.imr.korona.data.ping.PingReader;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public final class ComputationContext {
   private final ModuleContainer moduleContainer;
   private final PingReader pingReader;
   private AsyncHandle asyncHandle;
   private final @Nullable Path associatedKoronaDirectory;
   private final PingBuffering pingBuffering = new PingBuffering();
   private boolean usingKoronaPlaybox;
   private final AtomicInteger schoolIdCounter = new AtomicInteger();

   public ComputationContext(ModuleContainer moduleContainer, PingReader pingReader, AsyncHandle asyncHandle) {
      this.moduleContainer = moduleContainer;
      this.pingReader = pingReader;
      this.asyncHandle = asyncHandle;
      associatedKoronaDirectory = moduleContainer.deriveAssociatedKoronaDirectory();
   }

   public ModuleContainer getModuleContainer() {
      return moduleContainer;
   }

   public PingReader getPingReader() {
      return pingReader;
   }

   public @Nullable Path getAssociatedKoronaDirectory() {
      return associatedKoronaDirectory;
   }

   public PingBuffering getPingBuffering() {
      return pingBuffering;
   }

   public AsyncHandle getAsyncHandle() {
      return asyncHandle;
   }

   public void setAsyncHandle(AsyncHandle asyncHandle) {
      this.asyncHandle = asyncHandle;
   }

   public boolean isUsingKoronaPlaybox() {
      return usingKoronaPlaybox;
   }

   public ComputationContext setUsingKoronaPlaybox(boolean usingKoronaPlaybox) {
      this.usingKoronaPlaybox = usingKoronaPlaybox;
      return this;
   }

   public int nextSchoolId() {
      return schoolIdCounter.incrementAndGet();
   }
}
