package no.imr.deepvision.lsss.modules.image;

import no.imr.deepvision.lsss.DeepVisionPlugin;
import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.ViewHolder;

import java.util.List;

public final class DeepVisionImageViewModule extends BaseViewModule {
   private final ViewHolder<DeepVisionImageView> viewHolder = new ViewHolder<>(() -> new DeepVisionImageView(this));

   final BooleanParameter showLeftImage = new BooleanParameter(
         new Name("ShowLeftImage", "Show left image"),
         true,
         "Show the left image");

   final BooleanParameter showRightImage = new BooleanParameter(
         new Name("ShowRightImage", "Show right image"),
         true,
         "Show the right image");

   public final BooleanParameter showOnlyActiveImages = new BooleanParameter(
         new Name("ShowOnlyActiveImages", "Show only images marked as active"),
         false,
         "Show only active images");

   private final DeepVisionEngine deepVisionEngine;

   public DeepVisionImageViewModule(ModuleInfo<DeepVisionPlugin> moduleInfo) {
      super(moduleInfo);

      deepVisionEngine = moduleInfo.plugin().getDeepVisionEngine();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            showLeftImage,
            showRightImage,
            showOnlyActiveImages
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   DeepVisionEngine getDeepVisionEngine() {
      return deepVisionEngine;
   }
}
