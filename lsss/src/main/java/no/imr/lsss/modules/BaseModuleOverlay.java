package no.imr.lsss.modules;

import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.SeparatorParameter;
import org.jspecify.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for modules that are overlays on another module.
 * See {@link BaseLsssModule} for more information about implementing subclasses.
 */
public abstract non-sealed class BaseModuleOverlay extends BaseLsssModule {
   private boolean enabledByUser;
   private final ArgChangeManager<Boolean> enabledByUserChangeManager = new ArgChangeManager<>();
   private @Nullable OverlayDisplayData displayData;

   protected BaseModuleOverlay(ModuleInfo<?> moduleInfo, BaseOverlaidModule<?> module) {
      super(moduleInfo);

      module.getEnabledChangeManager().addListener(this::updateEnabled);
      getEnabledChangeManager().addListener(enabled -> {
         if (enabled) {
            recompute();
         } else {
            displayData = null;
         }
      });
   }

   public boolean isBackgroundOverlay() {
      return false;
   }

   public abstract BaseOverlaidModule<?> getOverlaidModule();

   public int getWidth() {
      return getOverlaidModule().getWidth();
   }

   public int getHeight() {
      return getOverlaidModule().getHeight();
   }

   public boolean isEnabledByUser() {
      return enabledByUser;
   }

   public void setEnabledByUser(boolean enabledByUser) {
      if (this.enabledByUser == enabledByUser) {
         return;
      }
      this.enabledByUser = enabledByUser;
      updateEnabled();
      enabledByUserChangeManager.notifyListeners(enabledByUser);
   }

   public ArgChangeManager<Boolean> getEnabledByUserChangeManager() {
      return enabledByUserChangeManager;
   }

   private void updateEnabled() {
      setEnabled(enabledByUser && getOverlaidModule().isEnabled());
   }

   public @Nullable OverlayDisplayData getDisplayData() {
      return displayData;
   }

   public void setDisplayData(@Nullable OverlayDisplayData displayData) {
      if (this.displayData == displayData) {
         return;
      }
      this.displayData = displayData;
      repaint();
   }

   protected Listener createRecomputeListener() {
      return newCoalescingExecListener(this::recompute);
   }

   public void recompute() {
      setDisplayData(recomputeDisplayData());
   }

   protected abstract @Nullable OverlayDisplayData recomputeDisplayData();

   /**
    * Tests if this overlay is ready to take focus.
    *
    * @return true if the overlay is in a state ready to take focus
    */
   public boolean readyToTakeFocus() {
      return true;
   }

   /**
    * Return the tooltip for this overlay for a given location.
    *
    * @param point the location in the overlaid module
    * @return the tooltip text, or {@code null} if the location is not close enough
    */
   public @Nullable String getToolTipText(Point point) {
      return null;
   }

   /**
    * Returns the popup menu for this overlay.
    *
    * @param point the location in the overlaid module
    * @return the popup menu
    */
   public @Nullable JPopupMenu getPopupMenu(Point point) {
      if (isConfigurable()) {
         return getDefaultPopupMenu(point);
      } else {
         return null;
      }
   }

   public boolean isActive() {
      return getOverlaidModule().getActiveOverlay() == this;
   }

   public void onActivate() {
   }

   public void onDeactivate() {
   }

   public void mousePressed(MouseEvent mouseEvent) {
   }

   public void mouseReleased(MouseEvent mouseEvent) {
   }

   public void mouseClicked(MouseEvent mouseEvent) {
   }

   public void mouseEntered(MouseEvent mouseEvent) {
   }

   public void mouseExited(MouseEvent mouseEvent) {
   }

   public void mouseDragged(MouseEvent mouseEvent) {
   }

   public void mouseMoved(MouseEvent mouseEvent) {
   }

   public boolean keyTyped(KeyEvent keyEvent) {
      return false;
   }

   public boolean keyPressed(KeyEvent keyEvent) {
      return false;
   }

   public boolean keyReleased(KeyEvent keyEvent) {
      return false;
   }

   public void repaint() {
      getOverlaidModule().repaint();
   }

   public void setCursor(Cursor cursor) {
      getOverlaidModule().setCursor(cursor);
   }

   public JPopupMenu getDefaultPopupMenu(Point point) {
      JPopupMenu popupMenu = new JPopupMenu();

      popupMenu.add(createPopupMenuTitle(getDisplayName()));
      popupMenu.addSeparator();
      popupMenu.add(createConfigureMenuItem());
      getOverlaidModule().getViewHolder().getView().addDisableOverlaysItems(point, popupMenu);

      return popupMenu;
   }

   private static JPanel createPopupMenuTitle(String title) {
      JPanel titlePanel = new JPanel(new BorderLayout());
      titlePanel.add(new JLabel(title, JLabel.CENTER));
      return titlePanel;
   }

   public boolean isUserVisibleForegroundOverlay() {
      return !isBackgroundOverlay();
   }

   @Override
   protected List<? extends BaseParameter<?>> getConfigurationEditorParameters() {
      List<? extends BaseParameter<?>> parameters = super.getConfigurationEditorParameters();
      if (parameters.isEmpty()) {
         return parameters;
      }
      List<BaseParameter<?>> allParameters = new ArrayList<>();
      BooleanParameter displayParameter = new BooleanParameter(
            new Name("_", "Display " + getDisplayName()),
            enabledByUser,
            "Turns on or off the display of " + getDisplayName());
      displayParameter.subscribe(this::setEnabledByUser);
      allParameters.add(displayParameter);
      allParameters.add(SeparatorParameter.line());
      allParameters.addAll(parameters);
      return allParameters;
   }
}
