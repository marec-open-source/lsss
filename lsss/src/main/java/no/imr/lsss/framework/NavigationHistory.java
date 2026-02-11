package no.imr.lsss.framework;

import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.framework.packages.LsssAction;
import no.imr.lsss.framework.packages.LsssPackage;
import no.imr.lsss.framework.packages.TaskLsssAction;
import no.imr.tools.Pair;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.UndoManager;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a list of navigation states, between which the user can go back and forward.
 */
public final class NavigationHistory {
   private final InterpretationSettings interpretationSettings;
   private final UndoManager undoManager = new UndoManager();
   private final List<EchogramZSettings> zSettingsList = new CopyOnWriteArrayList<>();
   private final AtomicInteger checkPointLockCounter = new AtomicInteger(1);
   private NavigationState currentState;
   private @Nullable NavigationStateContainer recentlyAddedStateContainer;
   private @Nullable Object coalescingIdentifier;
   private Future<?> coalesceEndFuture = new CompletableFuture<>();

   public final LsssAction backAction = new TaskLsssAction("goBack", "Go back to previous location",
         _ -> doWithNoAddCheckPoint(undoManager::undo))
         .setIcon(MiscIcons.ARROW_LEFT);

   public final LsssAction forwardAction = new TaskLsssAction("goForward", "Go forward to next location",
         _ -> doWithNoAddCheckPoint(undoManager::redo))
         .setIcon(MiscIcons.ARROW_RIGHT);

   NavigationHistory(InterpretationSettings interpretationSettings, LsssPackage lsssPackage) {
      this.interpretationSettings = interpretationSettings;
      currentState = new NavigationState();
      updateUndoableNavigationActions();
      lsssPackage.addAction(backAction);
      lsssPackage.addAction(forwardAction);
   }

   public void addZSettings(EchogramZSettings zSettings) {
      zSettingsList.add(zSettings);
   }

   public void doWithNoAddCheckPoint(Runnable runnable) {
      checkPointLockCounter.incrementAndGet();
      try {
         runnable.run();
      } finally {
         checkPointLockCounter.decrementAndGet();
      }
   }

   /**
    * Adds the current navigation state to the list of navigation states
    * which the user can go back and forward to.
    */
   public void addCheckPoint() {
      if (checkPointLockCounter.get() != 0) {
         return;
      }

      coalescingIdentifier = null;
      recentlyAddedStateContainer = null;
      NavigationState newState = new NavigationState();
      if (currentState.equals(newState)) {
         return;
      }

      recentlyAddedStateContainer = new NavigationStateContainer(newState);
      undoManager.addEdit(new NavigationEdit(new NavigationStateContainer(currentState), recentlyAddedStateContainer));
      currentState = newState;
      updateUndoableNavigationActions();
   }

   public void coalesceCheckPoint(Object identifier, Runnable runnable) {
      if (checkPointLockCounter.get() != 0) {
         return;
      }

      doWithNoAddCheckPoint(runnable);

      coalesceEndFuture.cancel(false);
      coalesceEndFuture = Exec.schedule(() -> coalescingIdentifier = null, 1, TimeUnit.SECONDS);

      if (coalescingIdentifier == identifier && recentlyAddedStateContainer != null) {
         currentState = new NavigationState();
         recentlyAddedStateContainer.state = currentState;
      } else {
         addCheckPoint();
         coalescingIdentifier = identifier;
      }
   }

   public void reset() {
      undoManager.discardAllEdits();
      updateUndoableNavigationActions();
      checkPointLockCounter.set(0);
      currentState = new NavigationState();
      recentlyAddedStateContainer = null;
      coalescingIdentifier = null;
   }

   public NavigationState getCurrentState() {
      return currentState;
   }

   private void updateUndoableNavigationActions() {
      backAction.setEnabled(undoManager.canUndo());
      forwardAction.setEnabled(undoManager.canRedo());
   }

   public void zoomOut() {
      doWithNoAddCheckPoint(() -> {
         interpretationSettings.getPingSettings().zoomOut();
         zSettingsList.forEach(EchogramZSettings::zoomOut);
      });
      addCheckPoint();
   }

   /**
    * Immutable object initialized with the current navigation state.
    */
   public final class NavigationState {
      private final DoubleRange valueRange = interpretationSettings.getValueRange();
      private final List<Pair<EchogramZSettings, FloatRange>> z;

      private NavigationState() {
         z = zSettingsList.stream()
               .map(zSettingsContainer -> new Pair<>(zSettingsContainer, zSettingsContainer.getZoomedZRange()))
               .toList();
      }

      public void apply() {
         interpretationSettings.setValueRange(valueRange);
         z.forEach(entry -> entry.first().setZ(entry.second()));
      }

      @Override
      public boolean equals(@Nullable Object obj) {
         if (this == obj) {
            return true;
         }
         return obj instanceof NavigationState that
               && valueRange.equals(that.valueRange)
               && z.equals(that.z);
      }

      @Override
      public int hashCode() {
         int result = valueRange.hashCode();
         result = 31 * result + z.hashCode();
         return result;
      }
   }

   private static final class NavigationStateContainer {
      private NavigationState state;

      private NavigationStateContainer(NavigationState state) {
         this.state = state;
      }
   }

   /**
    * Represents the navigation between two check points.
    */
   private final class NavigationEdit extends AbstractUndoableEdit {
      private final NavigationStateContainer prev;
      private final NavigationStateContainer next;

      private NavigationEdit(NavigationStateContainer prev, NavigationStateContainer next) {
         this.prev = prev;
         this.next = next;
      }

      @Override
      public void undo() {
         super.undo();
         apply(prev);
      }

      @Override
      public void redo() {
         super.redo();
         apply(next);
      }

      private void apply(NavigationStateContainer stateContainer) {
         stateContainer.state.apply();
         currentState = stateContainer.state;
         recentlyAddedStateContainer = null;
         coalescingIdentifier = null;
         updateUndoableNavigationActions();
      }
   }
}
