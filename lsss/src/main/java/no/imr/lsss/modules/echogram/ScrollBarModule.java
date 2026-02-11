package no.imr.lsss.modules.echogram;

import no.imr.korona.data.ping.PingRange;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.help.HelpID;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.range.DoubleRange;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JComponent;
import javax.swing.JScrollBar;
import java.util.List;

/**
 * A scroll bar representing the current ping range relative to the total ping range.
 */
public final class ScrollBarModule extends BaseViewModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   public ScrollBarModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(viewHolder.coalescingListener(View::updateScrollBar), List.of(
            getInterpretationSettings().getPingMappingChangeManager(),
            getInterpretationSettings().getPingRangeChangeManager()
      ));

      //---

      viewHolder.ifView(View::updateScrollBar);
   }

   @Override
   public HelpID getHelpID() {
      return getModuleManager().getModule(PelagicEchogramModule.class).getHelpID();
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseView {
      private final InterpretationSettings interpretationSettings;
      private final JScrollBar scrollBar = new JScrollBar(JScrollBar.HORIZONTAL, 0, Integer.MAX_VALUE / 4, 0, Integer.MAX_VALUE / 4);
      private int previousValue;
      private boolean isUpdatingScrollBar;

      private View(ScrollBarModule module) {
         super(module);

         interpretationSettings = module.getInterpretationSettings();
         scrollBar.setEnabled(false);
         scrollBar.addAdjustmentListener(_ -> updatePingRange());
         scrollBar.addMouseWheelListener(e -> scrollBar.setValue(scrollBar.getValue() + e.getWheelRotation() * scrollBar.getBlockIncrement() / 10));
         updateScrollBar();
      }

      @Override
      public JComponent getComponent() {
         return scrollBar;
      }

      private void updateScrollBar() {
         isUpdatingScrollBar = true;
         try {
            int value;
            int extent;
            int increment;
            if (interpretationSettings.getDataFileSet().isEmpty()) {
               value = 0;
               extent = scrollBar.getMaximum();
               increment = extent;
            } else {
               DoubleRange valueRange = interpretationSettings.getValueRange();
               PingRange totalRange = interpretationSettings.getDataFileSet().getTotalRange();
               DoubleRange totalValueRange = interpretationSettings.getPingMapping().toValueRange(totalRange);
               increment = (int) Math.round(scrollBar.getMaximum() * valueRange.getSize() / totalValueRange.getSize());
               DoubleRange clampedValueRange = totalValueRange.clamp(valueRange);
               value = (int) Math.round(scrollBar.getMaximum() * totalValueRange.valueToFraction(clampedValueRange.begin()));
               extent = (int) Math.round(scrollBar.getMaximum() * clampedValueRange.getSize() / totalValueRange.getSize());
            }

            scrollBar.setEnabled(!interpretationSettings.getDataFileSet().isEmpty());
            scrollBar.setValues(value, extent, 0, scrollBar.getMaximum());
            scrollBar.setBlockIncrement(increment);
            scrollBar.setUnitIncrement(increment);

            previousValue = value;
         } finally {
            isUpdatingScrollBar = false;
         }
      }

      private void updatePingRange() {
         if (isUpdatingScrollBar) {
            return;
         }

         int delta = scrollBar.getValue() - previousValue;
         previousValue = scrollBar.getValue();

         if (delta == 0) {
            // Nothing has changed, so do nothing.
         } else if (delta == scrollBar.getBlockIncrement()) {
            // The user has (probably) clicked to the right of the scroll handle.
            interpretationSettings.gotoNextPingRange();
         } else if (delta == -scrollBar.getBlockIncrement()) {
            // The user has (probably) clicked to the left of the scroll handle.
            interpretationSettings.gotoPreviousPingRange();
         } else {
            // The user is (probably) dragging the scroll handle.
            PingRange totalRange = interpretationSettings.getDataFileSet().getTotalRange();
            DoubleRange totalValueRange = interpretationSettings.getPingMapping().toValueRange(totalRange);
            double begin;
            double end;
            if (scrollBar.getValue() + scrollBar.getModel().getExtent() == scrollBar.getMaximum()) {
               end = totalValueRange.end();
               begin = end - interpretationSettings.getValueRange().getSize();
            } else {
               begin = totalValueRange.fractionToValue((double) scrollBar.getValue() / scrollBar.getMaximum());
               end = begin + interpretationSettings.getValueRange().getSize();
            }
            DoubleRange valueRange = DoubleRange.of(begin, end);
            interpretationSettings.getNavigationHistory().coalesceCheckPoint(this, () -> interpretationSettings.setValueRange(valueRange));
         }
      }
   }
}
