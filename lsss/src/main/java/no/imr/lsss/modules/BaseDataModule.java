package no.imr.lsss.modules;

/**
 * Base class for modules that do not have a display component.
 */
public abstract non-sealed class BaseDataModule extends BaseLsssModule {
   protected BaseDataModule(ModuleInfo<?> moduleInfo) {
      super(moduleInfo);
   }
}
