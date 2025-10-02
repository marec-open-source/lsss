package no.imr.lsss.modules.echogram;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.lsss.LSSS;
import no.imr.tools.concurrent.ConcurrentObject;
import no.imr.tools.listening.Listener;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WhenShowingListening;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import java.util.List;

public final class VerticalEchogramScrollBar extends ConcurrentObject {
   private final ViewHolder<View> viewHolder;
   private final ConcurrentObject owner;
   private final LSSS lsss;

   public VerticalEchogramScrollBar(ConcurrentObject owner, LSSS lsss, EchogramZSettings zSettings, DataManager dataManager) {
      super(lsss.getInterpretationSettings().createObservingSerialExecutor());

      this.owner = owner;
      this.lsss = lsss;

      viewHolder = new ViewHolder<>(() -> new View(lsss, zSettings, dataManager));

      getEnabledChangeManager().addListener(viewHolder.coalescingListener(view -> view.setVisible(isEnabled())));

      Listener.of(this::updateEnabled).addToAndNotify(
            owner.getEnabledChangeManager(),
            lsss.getConfigurationManager().getAppMiscConf().verticalScrollBar
      );
   }

   private void updateEnabled() {
      setEnabled(owner.isEnabled() && lsss.getConfigurationManager().getAppMiscConf().verticalScrollBar.getBooleanValue());
   }

   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   private static final class View implements ViewHolder.View {
      private final LSSS lsss;
      private final EchogramZSettings zSettings;
      private final DataManager dataManager;
      private final JScrollBar scrollBar = new JScrollBar(JScrollBar.VERTICAL, 0, Integer.MAX_VALUE / 4, 0, Integer.MAX_VALUE / 4);
      private boolean isUpdatingScrollBar;

      private View(LSSS lsss, EchogramZSettings zSettings, DataManager dataManager) {
         this.lsss = lsss;
         this.zSettings = zSettings;
         this.dataManager = dataManager;
         scrollBar.addAdjustmentListener(e -> updateZRange());
         scrollBar.addMouseWheelListener(e -> scrollBar.setValue(scrollBar.getValue() + e.getWheelRotation() * scrollBar.getBlockIncrement() / 10));

         WhenShowingListening.connect(scrollBar, List.of(
                     zSettings.minZ,
                     zSettings.maxZ,
                     zSettings.minZoomedZ,
                     zSettings.maxZoomedZ,
                     dataManager.getDataFileSetChangeManager()
               ),
               GuiListeners.coalescingLater(this::updateScrollBar));

         updateScrollBar();
         setVisible(lsss.getConfigurationManager().getAppMiscConf().verticalScrollBar.getBooleanValue());
      }

      @Override
      public JComponent getComponent() {
         return scrollBar;
      }

      private void updateZRange() {
         if (isUpdatingScrollBar) {
            return;
         }
         FloatRange maxZRange = zSettings.getMaxZRange();
         float minZ = maxZRange.fractionToValue((float) scrollBar.getValue() / scrollBar.getMaximum());
         float maxZ = minZ + zSettings.getZoomedZRange().getSize();
         lsss.getInterpretationSettings().getNavigationHistory().coalesceCheckPoint(this, () -> zSettings.setZ(minZ, maxZ));
      }

      private void updateScrollBar() {
         isUpdatingScrollBar = true;
         try {
            int value;
            int extent;
            boolean hasData = !dataManager.getDataFileSet().isEmpty();
            if (hasData) {
               FloatRange maxZRange = zSettings.getMaxZRange();
               value = Math.round(maxZRange.valueToFraction(zSettings.getMinZoomedZ()) * scrollBar.getMaximum());
               extent = Math.round(zSettings.getZoomedZRange().getSize() / maxZRange.getSize() * scrollBar.getMaximum());
            } else {
               value = 0;
               extent = scrollBar.getMaximum();
            }
            scrollBar.setEnabled(hasData);
            scrollBar.setValues(value, extent, 0, scrollBar.getMaximum());
            scrollBar.setBlockIncrement(extent);
            scrollBar.setUnitIncrement(extent);
         } finally {
            isUpdatingScrollBar = false;
         }
      }

      private void setVisible(boolean visible) {
         scrollBar.setVisible(visible);
         if (visible) {
            updateScrollBar();
         }
      }
   }
}
