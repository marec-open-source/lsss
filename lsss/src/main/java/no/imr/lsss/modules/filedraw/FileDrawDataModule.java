package no.imr.lsss.modules.filedraw;

import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.lsss.modules.BaseDataModule;
import no.imr.lsss.modules.ModuleInfo;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.listening.ListenerRegistry;
import no.imr.tools.swing.WorkerDialog;
import no.marec.lsss.api.util.observing.ObservableValue;

import java.util.List;

public final class FileDrawDataModule extends BaseDataModule {
   private final ListenableProperty<FileDrawData> fileDrawData = new ListenableProperty<>(new FileDrawData(List.of()));

   public FileDrawDataModule(ModuleInfo<BaseSystemFeaturePlugin> moduleInfo) {
      super(moduleInfo);
   }

   ObservableValue<FileDrawData> fileDrawData() {
      return fileDrawData;
   }

   @Override
   protected void onEnable(ListenerRegistry registry) {
      registry.add(newCoalescingExecListener(this::updateDataDir), List.of(
            getConfigurationManager().getDataConf().getDir(DataConfLSSS.FILE_DRAW_SUB_DIR),
            getInterpretationSettings().getReloadChangeManager()
      ));
   }

   private void updateDataDir() {
      List<FileDrawLine> lines = getConfigurationManager().getDataConf().getDir(DataConfLSSS.FILE_DRAW_SUB_DIR).getValue()
            .map(dir -> {
               return new WorkerDialog(getLSSS()::getReferenceComponent, "Loading file draw data from\n" + dir)
                     .setModalDialog(false)
                     .startMakeValue(asyncHandle -> FileDrawDataLoader.loadFiles(dir, asyncHandle));
            })
            .orElse(List.of());
      fileDrawData.setValue(new FileDrawData(lines));
   }
}
