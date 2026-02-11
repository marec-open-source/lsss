package no.imr.lsss.modules.korona.experimentation;

import no.imr.korona.computation.ModuleContainer;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.preprocessing.PreprocessingSetup;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ButtonParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.swing.ViewHolder;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

public final class KoronaExperimentationModule extends BaseViewModule {
   final IntParameter preprocessingSetup = new IntParameter(
         new Name("PreprocessingSetup", "Preprocessing setup"),
         1, Unit.NONE, ValueConstraints.gte(1),
         "Which preprocessing setup to use");

   final IntParameter pingPadding = new IntParameter(
         new Name("PingPadding", "Ping padding"),
         5, Unit.COUNT, ValueConstraints.gte(0),
         "Extra pings on each side of the selected range");

   private final ButtonParameter processParameter = new ButtonParameter(
         new Name("Process"),
         "",
         this::startProcessing);

   final OptionalIntParameter serverPort = new OptionalIntParameter(
         new Name("ServerPort", "Server port"),
         Optional.empty(), Unit.NONE, ValueConstraints.gte(0),
         "Port for the scripting server in the destination LSSS. Leave blank to disable");

   private final ViewHolder<KoronaExperimentationView> viewHolder = new ViewHolder<>(() -> new KoronaExperimentationView(this));
   private int nameSpaceCounter;
   private @Nullable KoronaExperimentationProcessor processor;
   private @Nullable ModuleContainer editingModuleContainer;

   public KoronaExperimentationModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);

      preprocessingSetup.subscribe(_ -> validateSetup());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            preprocessingSetup,
            pingPadding,
            processParameter,
            serverPort
      );
   }

   @Override
   public boolean isConfigurable() {
      return true;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(viewHolder.coalescingListener(KoronaExperimentationView::update), List.of(
            getInterpretationSettings().getDataFileChangeManager(),
            getRegionManager().selectedRegions()
      ));

      //---

      viewHolder.ifView(KoronaExperimentationView::update);
   }

   @Override
   protected void onDisable() {
      removeProcessor();
   }

   @Override
   public ViewHolder<? extends BaseView> getViewHolder() {
      return viewHolder;
   }

   void validateSetup() {
      List<PreprocessingSetup> preprocessingSetups = getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getPreprocessingSetups();
      if (preprocessingSetup.getIntValue() > preprocessingSetups.size()) {
         preprocessingSetup.setIntValue(1);
      }
      viewHolder.ifView(KoronaExperimentationView::update);
   }

   PreprocessingSetup getPreprocessingSetup() {
      validateSetup();
      List<PreprocessingSetup> preprocessingSetups = getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getPreprocessingSetups();
      return preprocessingSetups.get(preprocessingSetup.getIntValue() - 1);
   }

   @Override
   public void close() {
      removeProcessor();
   }

   int getNameSpaceCounter() {
      return nameSpaceCounter;
   }

   void setNameSpaceCounter(int nameSpaceCounter) {
      this.nameSpaceCounter = nameSpaceCounter;
   }

   void removeProcessor() {
      if (processor != null) {
         processor.close();
         processor = null;
      }
   }

   void startProcessing() {
      if (processor != null) {
         processor.stopProcessing();
      }
      executeIfEnabled(this::process);
   }

   private void process() {
      if (processor == null) {
         processor = new KoronaExperimentationProcessor(this);
      }
      processor.process();
   }

   void setProgress(double progress, boolean showText) {
      viewHolder.ifViewDelayed(this, view -> view.setProgress(progress, showText));
   }

   void setEditingModuleContainer(@Nullable ModuleContainer editingModuleContainer) {
      this.editingModuleContainer = editingModuleContainer;
   }

   ModuleContainer createModuleContainerForProcessing() throws IOException {
      if (editingModuleContainer != null) {
         ModuleContainer moduleContainer = new ModuleContainer(editingModuleContainer.getKorona(), editingModuleContainer.getConfigFileSettings());
         moduleContainer.fromXml(editingModuleContainer.toXml());
         return moduleContainer;
      } else {
         return getPreprocessingSetup().createModuleContainer();
      }
   }
}
