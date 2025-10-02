package no.imr.lsss.test;

import no.imr.korona.data.datamanager.DataManager;
import no.imr.korona.data.datamanager.DataManagerTestUtils;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.BaseSystemFeatureService;
import no.imr.lsss.framework.LsssConfig;
import no.imr.lsss.framework.ServiceCollection;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.framework.config.survey.data.DataSetLoader;
import no.imr.lsss.framework.config.survey.data.DataSetManager;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.BaseLsssModule;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleCollection;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.lsss.modules.OnStartup;
import no.imr.lsss.modules.Where;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.plugins.FeatureService;
import no.imr.tools.misc.test.UniqueTmpDir;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GuiUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public final class LsssTestUtils {
   private LsssTestUtils() {
   }

   public static LSSS start(List<Class<? extends BaseDataModule>> dataModuleClasses, List<Class<? extends BaseViewModule>> viewModuleClasses) {
      FeatureService lsssService = new BaseSystemFeatureService() {
         @Override
         public FeaturePlugin createPlugin(LSSS lsss) {
            return new BaseSystemFeaturePlugin(this, lsss) {
               @Override
               public ModuleCollection<?> getModules() {
                  ModuleCollection<BaseSystemFeaturePlugin> moduleCollection = new ModuleCollection<>(this);
                  dataModuleClasses.forEach(c -> addDataModule(moduleCollection, c));
                  viewModuleClasses.forEach(c -> addViewModule(moduleCollection, c));
                  return moduleCollection;
               }
            };
         }
      };
      return start(List.of(lsssService));
   }

   public static LSSS start(List<FeatureService> featureServices) {
      return start(new ServiceCollection(featureServices));
   }

   public static LSSS start() {
      return start(new ServiceCollection());
   }

   public static LSSS start(ServiceCollection serviceCollection) {
      LSSS lsss = new LSSS(new LsssConfig(serviceCollection).invisible().skipLoadSetting());
      Path workDir = UniqueTmpDir.newSubDir(DataConfLSSS.WORK_SUB_DIR.defaultRelativePath());
      lsss.getConfigurationManager().getDataConf().getDir(DataConfLSSS.WORK_SUB_DIR).setFile(workDir);
      lsss.getInterpretationSettings().setSampledPingCount(1000);
      return lsss;
   }

   public static void open(LSSS lsss, SegmentHandle... segmentHandles) {
      GuiUtils.invokeNowOrWait(() -> {
         lsss.getInterpretationSettings().dataFilesAboutToChange();
         open(lsss.getConfigurationManager().getDataConf().getDataSetManager(), segmentHandles);
         lsss.getInterpretationSettings().dataFilesChanged();
         lsss.getInterpretationSettings().resetNavigation();
      });
   }

   public static void open(DataManager dataManager, SegmentHandle... segmentHandles) {
      DataManagerTestUtils.open(dataManager, segmentHandles);
   }

   public static void open(DataSetManager dataSetManager, SegmentHandle... segmentHandles) {
      DataSetLoader dataSetLoader = new DataSetLoader(dataSetManager.getDataManager().getDataConfiguration());
      dataSetLoader.asyncOpenFiles(DataType.RAW, DataManagerTestUtils.testFileOpenRequest(segmentHandles));
      dataSetLoader.waitUntilFinished();
      dataSetManager.installNewDataFiles(dataSetLoader, DataType.RAW);
   }

   public static <P extends FeaturePlugin> ModuleCollection<P> addModules(P plugin, List<? extends Class<? extends BaseLsssModule>> classes) {
      ModuleCollection<P> moduleCollection = new ModuleCollection<>(plugin);
      classes.forEach(c -> {
         if (BaseDataModule.class.isAssignableFrom(c)) {
            addDataModule(moduleCollection, c.asSubclass(BaseDataModule.class));
         } else if (BaseViewModule.class.isAssignableFrom(c)) {
            addViewModule(moduleCollection, c.asSubclass(BaseViewModule.class));
         } else {
            fail(c.toString());
         }
      });
      return moduleCollection;
   }

   private static <P extends FeaturePlugin> void addDataModule(ModuleCollection<P> moduleCollection, Class<? extends BaseDataModule> c) {
      moduleCollection.dataModules()
            .add(new Name(c.getSimpleName()), c.getName(), classToFactory(c));
   }

   private static <P extends FeaturePlugin> void addViewModule(ModuleCollection<P> moduleCollection, Class<? extends BaseViewModule> c) {
      moduleCollection.viewModules(Where.UNSPECIFIED)
            .add(new Name(c.getSimpleName()), c.getName(), OnStartup.ENABLED, classToFactory(c));
   }

   private static <P extends FeaturePlugin, M extends BaseLsssModule> Function<ModuleInfo<P>, M> classToFactory(Class<? extends M> moduleClass) {
      return moduleInfo -> {
         try {
            return moduleClass.getConstructor(ModuleInfo.class).newInstance(moduleInfo);
         } catch (ReflectiveOperationException e) {
            throw new IllegalArgumentException(moduleClass.getName(), e);
         }
      };
   }
}
