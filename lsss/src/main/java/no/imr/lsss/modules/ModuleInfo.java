package no.imr.lsss.modules;

import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Info about a {@link BaseLsssModule}.
 */
public record ModuleInfo<P extends FeaturePlugin>(
      P plugin,
      Name name,
      String description,
      OnStartup onStartup
) {
   record DataModuleInfo<P extends FeaturePlugin>(
         ModuleInfo<P> moduleInfo,
         Function<ModuleInfo<P>, ? extends BaseDataModule> factory
   ) {
      BaseDataModule create() {
         return factory.apply(moduleInfo);
      }
   }

   public record RelativePosition(Position position, String referenceName) {
   }

   sealed interface PositionedModuleInfo<P extends FeaturePlugin> {
      ModuleInfo<P> moduleInfo();

      @Nullable RelativePosition relativePosition();
   }

   record ViewModuleInfo<P extends FeaturePlugin>(
         ModuleInfo<P> moduleInfo,
         Function<ModuleInfo<P>, ? extends BaseViewModule> factory,
         @Nullable RelativePosition relativePosition
   ) implements PositionedModuleInfo<P> {

      BaseViewModule create() {
         return factory.apply(moduleInfo);
      }
   }

   record OverlayInfo<P extends FeaturePlugin, M extends BaseOverlaidModule<O>, O extends BaseModuleOverlay>(
         ModuleInfo<P> moduleInfo,
         BiFunction<ModuleInfo<P>, M, ? extends O> factory,
         @Nullable RelativePosition relativePosition
   ) implements PositionedModuleInfo<P> {

      O create(M module) {
         return factory.apply(moduleInfo, module);
      }
   }
}
