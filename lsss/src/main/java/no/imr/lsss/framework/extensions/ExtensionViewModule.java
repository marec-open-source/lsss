package no.imr.lsss.framework.extensions;

import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.modules.ViewModule;
import no.marec.lsss.api.modules.ViewModuleAccess;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import java.util.List;
import java.util.function.Function;

final class ExtensionViewModule extends BaseViewModule {
   private final ViewModule module;
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   ExtensionViewModule(ModuleInfo<ExtensionFeaturePlugin> moduleInfo, Function<ViewModuleAccess, ? extends ViewModule> factory) {
      super(moduleInfo);

      module = factory.apply(new ViewModuleAccessImpl(this));
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return ExtensionUtils.getParameters(module);
   }

   @Override
   public boolean isConfigurable() {
      return module.getConfig() != null;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      module.onEnable(registry);
   }

   @Override
   protected void onDisable() {
      module.onDisable();
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseView {
      private final ViewModule.Gui gui;

      private View(ExtensionViewModule module) {
         super(module);

         gui = module.module.getGuiHolder().getGui();
      }

      @Override
      public JComponent getComponent() {
         return gui.getComponent();
      }

      @Override
      public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
         gui.extendFloatableModuleMenu(popupMenu);
      }
   }
}
