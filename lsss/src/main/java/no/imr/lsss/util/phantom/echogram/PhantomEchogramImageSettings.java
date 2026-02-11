package no.imr.lsss.util.phantom.echogram;

import no.imr.korona.color.Colormap;
import no.imr.korona.color.Colormaps;
import no.imr.korona.data.datamanager.DataConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.util.echogram.EchogramImageSettings;
import no.imr.korona.util.echogram.EchogramPingSettings;
import no.imr.korona.util.echogram.EchogramZSettings;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.PingToColor;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.ContinuousVariableSettings;
import no.imr.korona.viewer.variables.raw.SvVariable;
import no.imr.lsss.LSSS;

public final class PhantomEchogramImageSettings implements EchogramImageSettings {
   private final PhantomEchogramSettings phantomEchogramSettings;
   private final SvVariable phantomSvVariable = new SvVariable();
   private SingleValueColorConverter colorConverter;

   public PhantomEchogramImageSettings(LSSS lsss, PhantomEchogramSettings phantomEchogramSettings) {
      this.phantomEchogramSettings = phantomEchogramSettings;
      ColorConverterContainer colorConverterContainer = lsss.getInterpretationSettings().getColorConverterContainer();
      colorConverterContainer.getChangeManager().addListener(_ -> {
         Colormap colormap = colorConverterContainer.getColorConverter().getColormap();
         if (colormap != null) {
            colorConverter = new SingleValueColorConverter(phantomSvVariable, colormap);
         }
      });
      ContinuousVariableSettings lsssSvSettings = colorConverterContainer.getSV().getSettings();
      lsssSvSettings.getChangeManager().addListener(_ -> {
         ContinuousVariableSettings phantomSvSettings = phantomSvVariable.getSettings();
         phantomSvSettings.setMaxRange(lsssSvSettings.getMaxRange());
         phantomSvSettings.setRange(lsssSvSettings.getRange());
         phantomSvSettings.setClipAbove(lsssSvSettings.isClipAbove());
      });
      Colormap colormap = colorConverterContainer.getColorConverter().getColormap();
      colorConverter = new SingleValueColorConverter(phantomSvVariable, colormap != null ? colormap : Colormaps.COMBINED);
   }

   @Override
   public PingToColor getPingToColor() {
      return colorConverter;
   }

   @Override
   public int getChannel(RawFileConfiguration rawFileConfiguration) {
      return phantomEchogramSettings.getChannel();
   }

   @Override
   public EchogramPingSettings getPingSettings() {
      return phantomEchogramSettings.getEchogramPingSettings();
   }

   @Override
   public EchogramZSettings getZSettings() {
      return phantomEchogramSettings.getEchogramZSettings();
   }

   @Override
   public DataConfiguration getDataConfiguration() {
      return phantomEchogramSettings.getPhantomDataFileSet().getDataConfiguration();
   }
}
