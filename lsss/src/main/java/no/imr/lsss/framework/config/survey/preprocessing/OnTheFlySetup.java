package no.imr.lsss.framework.config.survey.preprocessing;

import no.imr.korona.computation.ConcurrentPingModule;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.util.CfsManager;
import no.imr.lsss.LSSS;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import org.jspecify.annotations.Nullable;

import javax.swing.JFileChooser;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

public final class OnTheFlySetup extends Configurable implements ParameterContainer {
   public final BooleanParameter useOnTheFlyProcessing = new BooleanParameter(
         new Name("UseOnTheFlyProcessing", "Use on the fly processing"),
         false);

   public final CfsParameter cfsFile;

   public final BooleanParameter evenIfProcessed = new BooleanParameter(
         new Name("EvenIfProcessed", "Apply even if already processed"),
         false);

   private final CfsManager cfsManager;

   private final ChangeManager editOkChangeManager = new ChangeManager();

   OnTheFlySetup(PreprocessingConf preprocessingConf) {
      super(new Name("OnTheFly"));

      LSSS lsss = preprocessingConf.getLSSS();
      cfsFile = new CfsParameter(lsss);
      cfsManager = new CfsManager(lsss.getKorona(), cfsFile, preprocessingConf.getContext(), ContextVisibility.HIDE);
      cfsManager.setModulePredicate(moduleInfo -> ConcurrentPingModule.class.isAssignableFrom(moduleInfo.moduleClass()));

      useOnTheFlyProcessing.addListenerAndNotify(use -> {
         cfsFile.setEnabled(use);
         evenIfProcessed.setEnabled(use);
      });
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(useOnTheFlyProcessing, cfsFile, evenIfProcessed);
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return getParameters();
   }

   public @Nullable ModuleContainer createModuleContainer() {
      if (!useOnTheFlyProcessing.getBooleanValue()) {
         return null;
      }
      Path cfsFile = this.cfsFile.getFile();
      if (cfsFile == null) {
         return null;
      }
      try {
         return cfsManager.loadModuleContainer();
      } catch (IOException e) {
         if (Files.exists(cfsFile)) {
            Log.global.log(Level.WARNING, "Error loading on the fly setup " + cfsFile, e);
         }
         return null;
      }
   }

   public void showEditor(@Nullable Component referenceComponent, boolean onlyModuleSetup) {
      if (onlyModuleSetup) {
         try {
            FileParameter.Editor editor = cfsManager.loadConfigFileSettings().getModuleConfigurationFileParameter().getEditor();
            assert editor != null;
            boolean ok = editor.edit(referenceComponent, true);
            if (ok) {
               editOkChangeManager.notifyListeners();
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, e.toString(), e);
         }
      } else {
         cfsFile.getEditor().edit(referenceComponent, true);
      }
   }

   public ChangeManager getEditOkChangeManager() {
      return editOkChangeManager;
   }

   public final class CfsParameter extends ConfigFileSettingsParameter {
      private CfsParameter(LSSS lsss) {
         super(lsss, new Name("OnTheFlyDataDir", "On the fly processing of DataDir"));
      }

      @Override
      public void customizeFileChooser(JFileChooser fileChooser) {
         fileChooser.setDialogTitle("Select config file settings");
      }

      @Override
      public Editor getEditor() {
         return new Editor() {
            @Override
            public boolean edit(@Nullable Component referenceComponent, boolean editable) {
               boolean ok = cfsManager.createCfsEditor().edit(referenceComponent, editable);
               if (ok) {
                  editOkChangeManager.notifyListeners();
               }
               return ok;
            }

            @Override
            public boolean createNew(@Nullable Component referenceComponent) {
               boolean ok = cfsManager.createCfsEditor().createNew(referenceComponent);
               if (ok) {
                  editOkChangeManager.notifyListeners();
               }
               return ok;
            }
         };
      }

      @Override
      public Copier getCopier() {
         return cfsManager.createCfsCopier();
      }
   }
}
