package no.imr.korona.data.buffer;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingIndex;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.time.Stopwatch;
import org.jspecify.annotations.Nullable;

import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Animates the last displayed ping.
 */
public final class PingAnimator {
   private final ArgChangeManager<State> stateChangeManager = new ArgChangeManager<>();
   private final ArgChangeManager<PingIndex> pingChangeManager = new ArgChangeManager<>();
   private final PingBuffer pingBuffer;
   private @Nullable PingIndex lastPingIndex;

   private State state = State.Home;
   private float pingsPerSecond = 1;

   private final Stopwatch stopwatch = Stopwatch.createStarted();
   private float consumedTime;

   private final Timer timer = new Timer(10, new TimerAction());

   public PingAnimator(PingBuffer pingBuffer) {
      this.pingBuffer = pingBuffer;
      pingBuffer.getChangeManager().addListener(GuiListeners.coalescingLater(() -> {
         if (state == State.Home) {
            gotoEnd();
         }
      }));
   }

   private void gotoEnd() {
      Ping ping = pingBuffer.getLastPing();
      if (ping != null) {
         setLastPingIndex(ping.getPingIndex());
      }
   }

   public void gotoBeginning() {
      setLastPingIndex(pingBuffer.getFirstPingIndex());
   }

   public ArgChangeManager<State> getStateChangeManager() {
      return stateChangeManager;
   }

   public ArgChangeManager<PingIndex> getPingChangeManager() {
      return pingChangeManager;
   }

   public PingBuffer getPingBuffer() {
      return pingBuffer;
   }

   public State getState() {
      return state;
   }

   public void setState(State state) {
      if (this.state == state) {
         return;
      }
      this.state = state;

      switch (state) {
         case Home -> {
            timer.stop();
            gotoEnd();
         }
         case Forward, Backward -> {
            stopwatch.restart();
            consumedTime = 0;
            timer.start();
         }
         case Pause -> {
            timer.stop();
         }
      }

      stateChangeManager.notifyListeners(state);

      maybeChangeState();
   }

   public float getPingsPerSecond() {
      return pingsPerSecond;
   }

   public void setPingsPerSecond(float pingsPerSecond) {
      this.pingsPerSecond = Math.abs(pingsPerSecond);
      if (pingsPerSecond > 0) {
         setState(State.Forward);
      } else if (pingsPerSecond < 0) {
         setState(State.Backward);
      } else {
         setState(State.Pause);
      }
   }

   public @Nullable PingIndex getLastPingIndex() {
      return lastPingIndex;
   }

   private void setLastPingIndex(PingIndex pingIndex) {
      if (lastPingIndex == pingIndex) {
         return;
      }
      lastPingIndex = pingIndex;
      pingChangeManager.notifyListeners(pingIndex);

      maybeChangeState();
   }

   private void maybeChangeState() {
      if (lastPingIndex == null) {
         return;
      }
      if (state == State.Forward && lastPingIndex.equals(pingBuffer.getLastPingIndex())) {
         setState(State.Home);
      } else if (state == State.Backward && lastPingIndex.equals(pingBuffer.getFirstPingIndex())) {
         setState(State.Pause);
      }
   }

   public enum State {
      Home, Forward, Backward, Pause
   }

   private final class TimerAction implements ActionListener {
      private TimerAction() {
      }

      @Override
      public void actionPerformed(ActionEvent e) {
         double t = stopwatch.seconds();
         double dt = t - consumedTime;
         double nn = pingsPerSecond * dt;
         if (nn > 1) {
            int n = (int) Math.floor(nn);
            step(state == State.Forward ? n : -n);
            consumedTime += n / pingsPerSecond;
         }
      }

      private void step(int n) {
         if (lastPingIndex == null) {
            return;
         }
         PingIndex pingIndex = pingBuffer.getPingIndex(lastPingIndex, n);
         if (pingIndex != null) {
            if (pingBuffer.isReady(pingIndex)) {
               setLastPingIndex(pingIndex);
            } else {
               //System.out.println("Not ready for " + pingIndex); // todo: remove
            }
         } else {
            if (state == State.Backward) {
               setLastPingIndex(pingBuffer.getFirstPingIndex());
            } else {
               gotoEnd();
            }
         }
      }
   }
}
