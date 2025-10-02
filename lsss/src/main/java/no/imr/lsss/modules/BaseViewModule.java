package no.imr.lsss.modules;

import no.imr.tools.help.ContextSensitiveHelp;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.Component;

/**
 * Base class for modules with a separate display component.
 */
public abstract non-sealed class BaseViewModule extends BaseLsssModule {
   private static final Object MODULE_KEY = new Object();

   private @Nullable FloatableInfo floatableInfo;

   protected BaseViewModule(ModuleInfo<?> moduleInfo) {
      super(moduleInfo);
   }

   public abstract ViewHolder<? extends BaseView> getViewHolder();

   public JComponent getComponent() {
      return getViewHolder().getComponent();
   }

   public @Nullable FloatableInfo getFloatableInfo() {
      return floatableInfo;
   }

   public void setFloatableInfo(@Nullable FloatableInfo floatableInfo) {
      this.floatableInfo = floatableInfo;
   }

   public static @Nullable BaseViewModule moduleForComponent(Component component) {
      return (BaseViewModule) GuiUtils.getAncestorProperty(component, MODULE_KEY);
   }

   public abstract static class BaseView implements ViewHolder.View {
      protected BaseView(BaseViewModule module) {
         SwingUtilities.invokeLater(() -> {
            JComponent component = getComponent();
            ContextSensitiveHelp.setHelpID(component, module.getHelpID());
            component.putClientProperty(MODULE_KEY, module);
            GuiUtils.putInfoProperty(component, module.getPersistentName());
         });
      }

      public void addToFloatableModuleMenu(JPopupMenu popupMenu) {
      }

      public JComponent getApiComponent() {
         return getComponent();
      }
   }

   public interface FloatableInfo {
      boolean isFloating();

      void setFloating(boolean floating);
   }
}
