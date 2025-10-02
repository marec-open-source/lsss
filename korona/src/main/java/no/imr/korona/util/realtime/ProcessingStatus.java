package no.imr.korona.util.realtime;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.ChangeManager;

import java.nio.file.Path;

public final class ProcessingStatus {
   private State state = State.IDLE;
   private final ArgChangeManager<StateMessage> processStateChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<Path> fileProcessedChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<NewFileInformation> newFileChangeChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<Ping> pingProcessedChangeManager = new ArgChangeManager<>();
   private final ChangeManager noMoreFilesChangeManager = new ChangeManager();

   public ProcessingStatus() {
   }

   public void setState(State state, String message) {
      this.state = state;
      processStateChangeManager.notifyListeners(new StateMessage(state, message));
   }

   public State getState() {
      return state;
   }

   public ArgChangeManager<StateMessage> getProcessStateChangeManager() {
      return processStateChangeManager;
   }

   public ArgChangeManager<Path> getFileProcessedChangeManager() {
      return fileProcessedChangeManager;
   }

   public ArgChangeManager<NewFileInformation> getNewFileChangeChangeManager() {
      return newFileChangeChangeManager;
   }

   public ArgChangeManager<Ping> getPingProcessedChangeManager() {
      return pingProcessedChangeManager;
   }

   public ChangeManager getNoMoreFilesChangeManager() {
      return noMoreFilesChangeManager;
   }

   public enum State {
      RUNNING, IDLE, ERROR
   }

   public record StateMessage(State state, String message) {
   }

   public record NewFileInformation(Path file, PingConfiguration pingConfiguration) {
   }
}
