package no.imr.lsss.util.phantom.echogram.overlays;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OverlayDisplayData;
import no.imr.lsss.util.phantom.echogram.BasePhantomEchogramModule;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.GuiText;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public final class ChannelInfoPhantomOverlay extends BasePhantomOverlay {
   public ChannelInfoPhantomOverlay(ModuleInfo<?> moduleInfo, BasePhantomEchogramModule phantomEchogramModule) {
      super(moduleInfo, phantomEchogramModule);
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(createRecomputeListener(), List.of(
            getPhantomEchogramSettings().getChannelChangeManager(),
            getPhantomEchogramSettings().getPhantomDataAdministrator().getChangedManager()
      ));
   }

   @Override
   protected @Nullable OverlayDisplayData recomputeDisplayData() {
      RawFileConfiguration rawFileConfiguration = getPhantomEchogramSettings().getPhantomDataFileSet().getRawFileConfiguration();
      int channel = getPhantomEchogramSettings().getChannel();
      if (channel <= rawFileConfiguration.getTransducerCount()) {
         return new DisplayData(rawFileConfiguration.getTransducers().get(channel - 1).getChannelId());
      } else {
         return null;
      }
   }

   private final class DisplayData extends OverlayDisplayData {
      private final String channelInfo;

      private DisplayData(String channelInfo) {
         this.channelInfo = channelInfo;
      }

      @Override
      public void drawText(Graphics2D g2d) {
         GuiText.draw(g2d, channelInfo, Color.BLACK, getWidth(), 0, GuiText.HorizontalAlignment.RIGHT, GuiText.VerticalAlignment.TOP, null);
      }
   }
}
