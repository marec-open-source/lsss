package no.imr.lsss.modules.echogram;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.util.geometry.depth.ChannelBottomDepthTransform;
import no.imr.korona.data.util.geometry.depth.CoordinatedBottomDepthTransform;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.InterpretationZSettings;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Bottom echogram view.
 */
public final class BottomEchogramModule extends EchogramModule {
   private final ViewHolder<View> viewHolder = new ViewHolder<>(() -> new View(this));

   public BottomEchogramModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo, moduleInfo.plugin().getLSSS().getInterpretationSettings().getBottomZSettings());

      getConfigurationManager().getSurveyMiscConf().pelagicMode.subscribe(pelagicMode -> setEnabled(!pelagicMode));
   }

   @Override
   public ViewHolder<? extends BaseEchogramView> getViewHolder() {
      return viewHolder;
   }

   private static final class View extends BaseEchogramView {
      private final BottomEchogramModule module;
      private final InterpretationZSettings.Bottom bottomZSettings;

      private View(BottomEchogramModule module) {
         super(module);

         this.module = module;
         bottomZSettings = module.getLSSS().getInterpretationSettings().getBottomZSettings();
      }

      @Override
      protected void addToPopupMenu(JPopupMenu popupMenu) {
         JMenu depthTransformMenu = new JMenu("Depth transform");
         popupMenu.add(depthTransformMenu);
         RawFileConfiguration rawFileConfiguration = module.getInterpretationSettings().getDataFileSet().getRawFileConfiguration();
         if (rawFileConfiguration.getTransducerCount() > 0) {
            for (int channel = 1; channel <= rawFileConfiguration.getTransducerCount(); channel++) {
               depthTransformMenu.add(createChannelItem(rawFileConfiguration, channel));
            }
            depthTransformMenu.addSeparator();
            depthTransformMenu.add(createCoordinatedItem());
            depthTransformMenu.add(createLayerItem());
         } else {
            depthTransformMenu.setEnabled(false);
         }
      }

      private JMenuItem createChannelItem(RawFileConfiguration rawFileConfiguration, int channel) {
         boolean selected = bottomZSettings.getDepthTransform() instanceof ChannelBottomDepthTransform channelBottomDepthTransform
               && channelBottomDepthTransform.getChannel() == channel;
         int kHz = rawFileConfiguration.getTransducers().get(channel - 1).getKHz();
         String text = kHz + " kHz";
         if (channel == module.getInterpretationSettings().getChannel()) {
            text += "  –  Current channel";
         }
         JMenuItem menuItem = MiscIcons.check(selected).on(new JMenuItem(text));
         menuItem.addActionListener(_ -> {
            bottomZSettings.setDepthTransformForCurrentChannel(
                  new ChannelBottomDepthTransform(module.getLSSS().getDataManager(), channel));
         });
         return menuItem;
      }

      private JMenuItem createCoordinatedItem() {
         boolean selected = bottomZSettings.getDepthTransform() instanceof CoordinatedBottomDepthTransform;
         JMenuItem menuItem = MiscIcons.check(selected).on(new JMenuItem("Coordinated bottom"));
         menuItem.addActionListener(_ -> {
            bottomZSettings.setDepthTransformForCurrentChannel(
                  new CoordinatedBottomDepthTransform(module.getLSSS().getDataManager()));
         });
         return menuItem;
      }

      private JMenuItem createLayerItem() {
         boolean selected = bottomZSettings.getDepthTransform() instanceof BottomBoundaryDepthTransform;
         JMenuItem menuItem = MiscIcons.check(selected).on(new JMenuItem("Bottom layer boundary"));
         menuItem.addActionListener(_ -> {
            bottomZSettings.setDepthTransformForCurrentChannel(
                  new BottomBoundaryDepthTransform(module.getRegionManager().getLayerManager(), module.getLSSS().getDataManager()));
         });
         return menuItem;
      }
   }
}
