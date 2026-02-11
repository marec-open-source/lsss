package no.imr.lsss.framework.extensions;

import com.google.common.base.Strings;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.LsssConfiguration;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.survey.SurveyConfiguration;
import no.imr.lsss.framework.config.survey.SurveyDirectoryParameter;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OnStartup;
import no.imr.lsss.modules.Position;
import no.imr.lsss.modules.Where;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.map.MapModule;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;
import no.marec.lsss.api.LsssPlugin;
import no.marec.lsss.api.LsssPluginLoader;
import no.marec.lsss.api.config.DataConfig;
import no.marec.lsss.api.echogram.EchogramDepthTransform;
import no.marec.lsss.api.echogram.EchogramPingTransform;
import no.marec.lsss.api.modules.EchogramOverlay;
import no.marec.lsss.api.modules.EchogramOverlayAccess;
import no.marec.lsss.api.modules.MapOverlay;
import no.marec.lsss.api.modules.MapOverlayAccess;
import no.marec.lsss.api.modules.ModuleRegistry;
import no.marec.lsss.api.modules.ViewModule;
import no.marec.lsss.api.modules.ViewModuleAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

final class ExtensionFeaturePlugin extends FeaturePlugin {
   private final LsssPlugin lsssPlugin;
   private final List<DataConfig> dataConfigs;
   private final List<SubDir> subDirs;

   private final EchogramPingTransform echogramPingTransform;
   private final EchogramDepthTransform pelagicEchogramDepthTransform;
   private final EchogramDepthTransform bottomEchogramDepthTransform;

   private final HelpSystemHelpSet helpSet;

   ExtensionFeaturePlugin(ExtensionFeatureService service, LSSS lsss, LsssPluginLoader lsssPluginLoader) {
      super(service, lsss);

      lsssPlugin = lsssPluginLoader.createPlugin(lsss.getLsssAccess());

      dataConfigs = lsssPlugin.getDataConfigs();

      subDirs = dataConfigs.stream()
            .flatMap(dataConfig -> Utils.getAllOfType(dataConfig.getParameters(), SurveyDirectoryParameter.class))
            .map(SurveyDirectoryParameter::getSubDir)
            .toList();

      echogramPingTransform = new EchogramPingTransformImpl(lsss.getInterpretationSettings());
      pelagicEchogramDepthTransform = new EchogramDepthTransformImpl(lsss, lsss.getInterpretationSettings().getPelagicZSettings());
      bottomEchogramDepthTransform = new EchogramDepthTransformImpl(lsss, lsss.getInterpretationSettings().getBottomZSettings());

      String helpResourceDir = lsssPlugin.getHelpResourceDir();
      helpSet = helpResourceDir != null
            ? new HelpSystemHelpSet(helpResourceDir, getPersistentName(), lsssPluginLoader.getClass().getClassLoader())
            : HelpSystemHelpSet.EMPTY;
   }

   @Override
   public HelpSystemHelpSet getHelpSet() {
      return helpSet;
   }

   @Override
   public void addConfiguration(LsssConfiguration lsssConfiguration) {
      SurveyConfiguration surveyConfiguration = lsssConfiguration.getSurveyConfiguration();
      for (DataConfig dataConfig : dataConfigs) {
         surveyConfiguration.getDataConf().addSubConfigurationUnit(new ExtensionSurveyDirectoryConf(this, dataConfig));
      }
   }

   @Override
   public void setup() {
      lsssPlugin.setup();
   }

   @Override
   public ModuleCollection<?> getModules() {
      ModuleCollection<ExtensionFeaturePlugin> moduleCollection = new ModuleCollection<>(this);

      lsssPlugin.addModules(new ModuleRegistry() {
         @Override
         public void addViewModule(ViewModuleLocation location, RelativePosition relativePosition,
                                   String id, String label, String description,
                                   Function<ViewModuleAccess, ViewModule> factory) {
            Where where = switch (location) {
               case BOTTOM -> Where.BOTTOM;
               case RIGHT -> Where.RIGHT;
               case BELOW_ECHOGRAM -> Where.BELOW_ECHOGRAM;
            };
            moduleCollection.viewModules(where)
                  .add(toName(id, label),
                        Strings.nullToEmpty(description),
                        OnStartup.DISABLED, info -> new ExtensionViewModule(info, factory),
                        toRelativePosition(relativePosition));
         }

         @Override
         public void addEchogramOverlay(RelativePosition relativePosition,
                                        String id, String label, String description,
                                        Function<EchogramOverlayAccess, EchogramOverlay> factory) {
            moduleCollection.overlays(EchogramModule.class)
                  .add(toName(id, label),
                        Strings.nullToEmpty(description),
                        OnStartup.DISABLED, (info, module) -> new ExtensionEchogramOverlay(info, module, factory),
                        toRelativePosition(relativePosition));
         }

         @Override
         public void addMapOverlay(RelativePosition relativePosition,
                                   String id, String label, String description,
                                   Function<MapOverlayAccess, MapOverlay> factory) {
            moduleCollection.overlays(MapModule.class)
                  .add(toName(id, label),
                        Strings.nullToEmpty(description),
                        OnStartup.DISABLED, (info, module) -> new ExtensionMapOverlay(info, module, factory),
                        toRelativePosition(relativePosition));
         }

         private static ModuleInfo.@Nullable RelativePosition toRelativePosition(RelativePosition relativePosition) {
            return switch (relativePosition) {
               case RelativePosition.Default _ -> null;
               case RelativePosition.Before b -> new ModuleInfo.RelativePosition(Position.BEFORE, b.refId());
               case RelativePosition.After a -> new ModuleInfo.RelativePosition(Position.AFTER, a.refId());
            };
         }
      });

      return moduleCollection;
   }

   @Override
   public List<SubDir> getSubDirs() {
      return subDirs;
   }

   private static Name toName(String id, @Nullable String label) {
      return new Name(id, Objects.requireNonNullElse(label, id));
   }

   EchogramPingTransform getEchogramPingTransform() {
      return echogramPingTransform;
   }

   EchogramDepthTransform getPelagicEchogramDepthTransform() {
      return pelagicEchogramDepthTransform;
   }

   EchogramDepthTransform getBottomEchogramDepthTransform() {
      return bottomEchogramDepthTransform;
   }
}
