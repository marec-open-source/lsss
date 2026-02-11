package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.SurveyManager;
import no.imr.tools.io.FileUtils;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import org.jspecify.annotations.Nullable;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SurveyFileDialog {
   private static final String PREFERENCE_OPEN_DIALOG_SIZE = "openDialogSize";

   private final LSSS lsss;
   private final SurveyManager surveyManager;
   private boolean showList;

   public SurveyFileDialog(LSSS lsss, SurveyManager surveyManager) {
      this.lsss = lsss;
      this.surveyManager = surveyManager;
   }

   public @Nullable Path chooseFile(int dialogType) {
      JFileChooser fileChooser;
      if (dialogType == JFileChooser.OPEN_DIALOG) {
         fileChooser = new OpenSurveyFileChooser();
         fileChooser.setDialogTitle("Open survey");
      } else {
         fileChooser = new JFileChooser();
         fileChooser.setDialogTitle("Save survey as");
      }
      adaptFileChooser(fileChooser);

      Path lastSurveyFile = surveyManager.getLastSurveyFile();
      if (lastSurveyFile != null) {
         if (Files.exists(lastSurveyFile)) {
            fileChooser.setSelectedFile(lastSurveyFile.toFile());
         } else {
            fileChooser.setCurrentDirectory(FileUtils.toFile(FileUtils.getExistingParent(lastSurveyFile)));
         }
      }

      int returnState;
      if (dialogType == JFileChooser.OPEN_DIALOG) {
         returnState = fileChooser.showOpenDialog(lsss.getFrame());
      } else {
         returnState = fileChooser.showSaveDialog(lsss.getFrame());
      }

      if (returnState == JFileChooser.APPROVE_OPTION) {
         return addSurveyFileSuffix(fileChooser.getSelectedFile().toPath());
      } else {
         return null;
      }
   }

   private static Path addSurveyFileSuffix(Path file) {
      return FileUtils.ensureSuffix(file, SurveyManager.SURVEY_FILE_SUFFIX);
   }

   private static void adaptFileChooser(JFileChooser fileChooser) {
      fileChooser.setFileFilter(new SuffixFileFilter("LSSS survey configuration", SurveyManager.SURVEY_FILE_SUFFIX));
   }

   private final class OpenSurveyFileChooser extends JFileChooser {
      private OpenSurveyFileChooser() {
      }

      @Override
      protected JDialog createDialog(Component parent) {
         JDialog dialog = super.createDialog(parent);
         dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

         SurveyList surveyList = new SurveyList(lsss, surveyManager.getLastSurveyFile(),
               file -> {
                  setSelectedFile(file.toFile());
                  approveSelection();
               },
               this::cancelSelection);

         JTabbedPane tabbedPane = new JTabbedPane();
         tabbedPane.add("Browse", dialog.getContentPane());
         tabbedPane.add("List", surveyList.getComponent());
         tabbedPane.setMnemonicAt(0, KeyEvent.VK_B);
         tabbedPane.setMnemonicAt(1, KeyEvent.VK_L);
         dialog.setContentPane(tabbedPane);
         dialog.getRootPane().setDefaultButton(getUI().getDefaultButton(this));

         tabbedPane.addChangeListener(_ -> {
            if (tabbedPane.getSelectedIndex() == -1) {
               return;
            }
            showList = tabbedPane.getSelectedIndex() == 1;
            if (showList) {
               surveyList.execute();
            }
         });

         if (showList) {
            tabbedPane.setSelectedIndex(1);
         }

         dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
               surveyList.cancel();
            }
         });

         dialog.pack();
         GuiUtils.syncSize(dialog, surveyManager.getPreferences(), PREFERENCE_OPEN_DIALOG_SIZE);
         dialog.setLocationRelativeTo(parent);
         GuiUtils.clampToScreen(dialog);

         return dialog;
      }
   }
}
