package no.imr.lsss.modules.echogram;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.util.echogram.EchogramImageSettings;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.korona.viewer.coloring.PingToColor;
import no.imr.lsss.framework.InterpretationSettings;

public final class EchogramModuleImageSettings implements EchogramImageSettings {
   private final InterpretationSettings interpretationSettings;
   private final EchogramZSettings zSettings;

   public EchogramModuleImageSettings(InterpretationSettings interpretationSettings, EchogramZSettings zSettings) {
      this.interpretationSettings = interpretationSettings;
      this.zSettings = zSettings;
   }

   @Override
   public PingToColor getPingToColor() {
      return interpretationSettings.getColorConverterContainer().getColorConverter();
   }

   @Override
   public int getChannel(RawFileConfiguration rawFileConfiguration) {
      return interpretationSettings.getChannel();
   }

   @Override
   public EchogramPingSettings getPingSettings() {
      return interpretationSettings.getPingSettings();
   }

   @Override
   public EchogramZSettings getZSettings() {
      return zSettings;
   }
}
