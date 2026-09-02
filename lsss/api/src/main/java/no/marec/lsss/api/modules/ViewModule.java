package no.marec.lsss.api.modules;

import no.marec.lsss.api.util.GuiHolder;

import javax.swing.JPopupMenu;

/**
 * A view module provided by an LSSS plugin.
 * <p>
 * This interface represents the behaviour of the view module, while
 * the actual view module can be accessed via {@link ViewModuleAccess}.
 */
public interface ViewModule extends LsssModule {
   /**
    * {@return a holder for this module's GUI}
    */
   GuiHolder<? extends Gui> getGuiHolder();

   /**
    * The GUI created by this view module.
    */
   interface Gui extends GuiHolder.Gui {
      /**
       * Optionally extend the view module menu.
       * <p>
       * The view module menu is accessed via the menu button
       * located in the header above the view module content.
       *
       * @param popupMenu the menu to extend
       */
      default void extendFloatableModuleMenu(JPopupMenu popupMenu) {
      }
   }
}
