package no.imr.lsss.modules;

import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

public final class ModuleCollection<P extends FeaturePlugin> {
   private final P plugin;
   private final DataModuleInfoCollection dataModuleInfoCollection = new DataModuleInfoCollection();
   private final Map<Where, ViewModuleInfoCollection> viewModuleInfoCollections = new EnumMap<>(Where.class);
   private final Map<Class<? extends BaseOverlaidModule<?>>, OverlayInfoCollection<?, ?>> overlayInfoCollections = new HashMap<>();

   public ModuleCollection(P plugin) {
      this.plugin = plugin;
      for (Where where : Where.values()) {
         viewModuleInfoCollections.put(where, new ViewModuleInfoCollection());
      }
   }

   public DataModuleInfoCollection dataModules() {
      return dataModuleInfoCollection;
   }

   public ViewModuleInfoCollection viewModules(Where where) {
      return viewModuleInfoCollections.get(where);
   }

   Collection<OverlayInfoCollection<?, ?>> overlays() {
      return overlayInfoCollections.values();
   }

   @SuppressWarnings("unchecked")
   public <M extends BaseOverlaidModule<O>, O extends BaseModuleOverlay> OverlayInfoCollection<M, O> overlays(Class<M> overlaidModuleClass) {
      return (OverlayInfoCollection<M, O>) overlayInfoCollections.computeIfAbsent(overlaidModuleClass, OverlayInfoCollection::new);
   }

   @SuppressWarnings("unchecked")
   <M extends BaseOverlaidModule<O>, O extends BaseModuleOverlay> @Nullable OverlayInfoCollection<M, O> overlaysOrNull(Class<M> overlaidModuleClass) {
      return (OverlayInfoCollection<M, O>) overlayInfoCollections.get(overlaidModuleClass);
   }

   public final class DataModuleInfoCollection {
      final List<ModuleInfo.DataModuleInfo<P>> dataModuleInfos = new ArrayList<>();

      private DataModuleInfoCollection() {
      }

      public DataModuleInfoCollection add(Name name, String description, Function<ModuleInfo<P>, ? extends BaseDataModule> factory) {
         dataModuleInfos.add(new ModuleInfo.DataModuleInfo<>(new ModuleInfo<>(plugin, name, description, OnStartup.ENABLED), factory));
         return this;
      }
   }

   public final class ViewModuleInfoCollection {
      final List<ModuleInfo.ViewModuleInfo<P>> viewModuleInfos = new ArrayList<>();

      private ViewModuleInfoCollection() {
      }

      public ViewModuleInfoCollection add(Name name, String description, OnStartup onStartup,
                                          Function<ModuleInfo<P>, ? extends BaseViewModule> factory) {
         return add(name, description, onStartup, factory, null);
      }

      public ViewModuleInfoCollection add(Name name, String description, OnStartup onStartup,
                                          Function<ModuleInfo<P>, ? extends BaseViewModule> factory,
                                          Position position, String referenceName) {
         return add(name, description, onStartup, factory, new ModuleInfo.RelativePosition(position, referenceName));
      }

      public ViewModuleInfoCollection add(Name name, String description, OnStartup onStartup,
                                          Function<ModuleInfo<P>, ? extends BaseViewModule> factory,
                                          ModuleInfo.@Nullable RelativePosition relativePosition) {
         viewModuleInfos.add(new ModuleInfo.ViewModuleInfo<>(new ModuleInfo<>(plugin, name, description, onStartup), factory, relativePosition));
         return this;
      }
   }

   public final class OverlayInfoCollection<M extends BaseOverlaidModule<O>, O extends BaseModuleOverlay> {
      final Class<M> overlaidModuleClass;
      final List<ModuleInfo.OverlayInfo<P, M, O>> overlayInfos = new ArrayList<>();

      private OverlayInfoCollection(Class<M> overlaidModuleClass) {
         this.overlaidModuleClass = overlaidModuleClass;
      }

      public OverlayInfoCollection<M, O> add(Name name, String description, OnStartup onStartup,
                                             BiFunction<ModuleInfo<P>, M, O> factory) {
         return add(name, description, onStartup, factory, null);
      }

      public OverlayInfoCollection<M, O> add(Name name, String description, OnStartup onStartup,
                                             BiFunction<ModuleInfo<P>, M, O> factory,
                                             Position position, String referenceName) {
         return add(name, description, onStartup, factory, new ModuleInfo.RelativePosition(position, referenceName));
      }

      public OverlayInfoCollection<M, O> add(Name name, String description, OnStartup onStartup,
                                             BiFunction<ModuleInfo<P>, M, O> factory,
                                             ModuleInfo.@Nullable RelativePosition relativePosition) {
         overlayInfos.add(new ModuleInfo.OverlayInfo<>(new ModuleInfo<>(plugin, name, description, onStartup), factory, relativePosition));
         return this;
      }
   }
}
