package no.imr.deepvision.lsss;

import no.imr.deepvision.lsss.engine.DeepVisionEngine;
import no.imr.deepvision.lsss.engine.data.DeepVisionFileInfo;
import no.imr.deepvision.lsss.engine.data.Direction;
import no.imr.deepvision.lsss.engine.data.pojo.DeepVisionFrame;
import no.imr.deepvision.lsss.modules.echogram.DeepVisionPathEchogramOverlay;
import no.imr.deepvision.lsss.modules.echogramplot.AthwartDistancePingFunction;
import no.imr.deepvision.lsss.modules.echogramplot.ImageDiffPingFunction;
import no.imr.deepvision.lsss.modules.echogramplot.SelectedFrameMarker;
import no.imr.deepvision.lsss.modules.image.DeepVisionImageViewModule;
import no.imr.deepvision.lsss.resources.DeepVisionHelp;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.backup.BackupExclusionOption;
import no.imr.lsss.framework.config.LsssConfiguration;
import no.imr.lsss.framework.config.application.SubDir;
import no.imr.lsss.framework.config.survey.SurveyConfiguration;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.lsss.modules.OnStartup;
import no.imr.lsss.modules.Position;
import no.imr.lsss.modules.Where;
import no.imr.lsss.modules.echogram.EchogramModule;
import no.imr.lsss.modules.echogramplot.EchogramPlotModule;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.help.HelpSystemHelpSet;
import no.imr.tools.parameter.Name;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class DeepVisionPlugin extends FeaturePlugin {
   public static final SubDir DEEP_VISION_DIR = new SubDir(new Name("MainDeepVisionDir"), new Name("DeepVisionDataDir"), "DeepVision");

   private final DeepVisionEngine deepVisionEngine;

   DeepVisionPlugin(DeepVisionService service, LSSS lsss) {
      super(service, lsss);

      deepVisionEngine = new DeepVisionEngine(this);
   }

   @Override
   public void setup() {
      deepVisionEngine.setup();

      SelectedFrameMarker selectedFrameMarker = new SelectedFrameMarker(deepVisionEngine);
      EchogramPlotModule echogramPlotModule = getLSSS().getModuleManager().getModule(EchogramPlotModule.class);
      echogramPlotModule.add(new AthwartDistancePingFunction(deepVisionEngine, selectedFrameMarker));
      echogramPlotModule.add(new ImageDiffPingFunction(deepVisionEngine, selectedFrameMarker));
   }

   @Override
   public HelpSystemHelpSet getHelpSet() {
      return DeepVisionHelp.HELP_SET;
   }

   @Override
   public ModuleCollection<?> getModules() {
      ModuleCollection<DeepVisionPlugin> moduleCollection = new ModuleCollection<>(this);

      moduleCollection.viewModules(Where.BOTTOM)
            .add(new Name("DeepVisionImageViewModule", "Deep Vision Image"),
                  "Displays the left and right Deep Vision images",
                  OnStartup.DISABLED, DeepVisionImageViewModule::new,
                  Position.BEFORE, "FrequencyResponseModule");

      moduleCollection.overlays(EchogramModule.class)
            .add(new Name("DeepVisionPathEchogramOverlay", "Deep Vision Path"),
                  "Displays the path of the Deep Vision vessel",
                  OnStartup.ENABLED, DeepVisionPathEchogramOverlay::new,
                  Position.AFTER, "PingMarkerOverlay");

      return moduleCollection;
   }

   @Override
   public void addConfiguration(LsssConfiguration lsssConfiguration) {
      SurveyConfiguration surveyConfiguration = lsssConfiguration.getSurveyConfiguration();
      surveyConfiguration.getDataConf().addSubConfigurationUnit(deepVisionEngine.getDeepVisionDataConf());
   }

   public DeepVisionEngine getDeepVisionEngine() {
      return deepVisionEngine;
   }

   @Override
   public List<SubDir> getSubDirs() {
      return List.of(
            DEEP_VISION_DIR
      );
   }

   @Override
   public List<BackupExclusionOption> getBackupExclusionOptions() {
      return List.of(
            new BackupExclusionOption(
                  new Name("DeepVisionOnlyActiveImages", "Deep Vision: Only active images"),
                  this::onlyActiveImagesExclusionPredicate)
      );
   }

   private Predicate<Path> onlyActiveImagesExclusionPredicate() {
      Path deepVisionDir = deepVisionEngine.getDeepVisionDataConf().baseDir.getFile();
      if (deepVisionDir == null) {
         // Directory not configured => Exclude noting.
         return file -> false;
      }

      Set<Path> activeImageFiles = new HashSet<>();
      Set<Path> imageDirectories = new HashSet<>();
      Set<String> imageFileSuffixes = new HashSet<>();
      for (DeepVisionFileInfo fileInfo : deepVisionEngine.getDataAdministrator().getAllFiles()) {
         imageDirectories.add(fileInfo.getImageDirectory(Direction.LEFT));
         imageDirectories.add(fileInfo.getImageDirectory(Direction.RIGHT));
         imageFileSuffixes.add(fileInfo.isZipped() ? ".zip" : fileInfo.getImageFileSuffix());
         for (DeepVisionFrame frame : fileInfo.getDeepVisionFile().frames.frames) {
            if (frame.active) {
               activeImageFiles.add(fileInfo.getFileContainer(frame, Direction.LEFT));
               activeImageFiles.add(fileInfo.getFileContainer(frame, Direction.RIGHT));
            }
         }
      }
      return file -> {
         String filePath = file.toString();
         if (!filePath.startsWith(deepVisionDir.toString())) {
            // File is not in the Deep Vision directory => Do no exclude.
            return false;
         }
         for (String imageSuffix : imageFileSuffixes) {
            if (filePath.endsWith(imageSuffix)) {
               // Image file => Exclude if it is in an image directory, and it is not an active image.
               return imageDirectories.contains(file.getParent()) && !activeImageFiles.contains(file);
            }
         }
         // Not image file => Do no exclude.
         return false;
      };
   }
}
