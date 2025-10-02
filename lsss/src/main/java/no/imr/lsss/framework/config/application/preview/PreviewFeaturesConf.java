package no.imr.lsss.framework.config.application.preview;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.UserProfile;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JComponent;
import java.util.List;

public final class PreviewFeaturesConf extends ConfigurationUnit {
   public final BooleanParameter showStatusBarIndicator = new BooleanParameter(
         new Name("ShowStatusBarIndicator", "Show status bar indicator"),
         true,
         "Shows an indicator in the status bar if preview features are activated");

   private final ViewHolder<PreviewFeaturesConfView> viewHolder = new ViewHolder<>(() -> new PreviewFeaturesConfView(this));

   private final PreviewFeatureToggles toggles = new PreviewFeatureToggles();

   public PreviewFeaturesConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("PreviewFeaturesConf", "Preview features"),
            "Selection of which preview features to activate");
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            showStatusBarIndicator
      );
   }

   @Override
   public JComponent getComponent() {
      return viewHolder.getComponent();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   @Override
   public boolean apply() {
      toggles.save();
      return true;
   }

   @Override
   public void cancelled() {
      toggles.load();
   }

   @Override
   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return parameter == showStatusBarIndicator ? UserProfile.NORMAL_USE : UserProfile.ADMINISTRATOR_MODE;
   }

   public PreviewFeatureToggles getToggles() {
      return toggles;
   }
}
