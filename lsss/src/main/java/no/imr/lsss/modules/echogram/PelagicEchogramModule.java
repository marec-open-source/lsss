package no.imr.lsss.modules.echogram;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JPopupMenu;

/**
 * Pelagic echogram view.
 */
public final class PelagicEchogramModule extends EchogramModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   public PelagicEchogramModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo, moduleInfo.plugin().getLSSS().getInterpretationSettings().getPelagicZSettings());
   }

   @Override
   public ViewHolder<? extends BaseEchogramView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseEchogramView {
      private View(PelagicEchogramModule module) {
         super(module);
      }

      @Override
      void addToPopupMenu(JPopupMenu popupMenu) {
      }
   }
}
