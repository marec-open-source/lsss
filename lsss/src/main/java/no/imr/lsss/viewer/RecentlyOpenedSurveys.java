package no.imr.lsss.viewer;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.survey.SurveyConfigurationXml;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.concurrent.SerialExecutor;
import no.imr.tools.io.FileUtils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.MenuAdapter;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class RecentlyOpenedSurveys {
   private static final String PREFERENCE_RECENTLY_OPENED = "recentlyOpened";
   private static final int MAX_ENTRIES = 20;

   private final SurveyManager surveyManager;
   private final JMenu menu = new JMenu("Reopen survey");
   private final List<Path> files = new ArrayList<>();
   private final SerialExecutor serialExecutor = new SerialExecutor(Exec.CACHED_THREAD_POOL);

   RecentlyOpenedSurveys(SurveyManager surveyManager) {
      this.surveyManager = surveyManager;

      surveyManager.getChangeManager().addListener(this::onSurveyManagerChange);

      menu.setEnabled(false);
      menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuSelected(MenuEvent e) {
            populate(menu);
         }

         @Override
         public void menuDeselected(MenuEvent e) {
            menu.removeAll();
            serialExecutor.discardWaitingJobs();
         }
      });

      readPreference();
      updateMenuEnabling();
   }

   JMenu getMenu() {
      return menu;
   }

   private void onSurveyManagerChange(Optional<Path> optionalFile) {
      optionalFile.ifPresent(file -> {
         files.remove(file);
         files.addFirst(file);
         while (files.size() > MAX_ENTRIES) {
            files.removeLast();
         }
         savePreference();
      });

      updateMenuEnabling();
   }

   private void updateMenuEnabling() {
      menu.setEnabled(!getApplicableFiles().isEmpty());
   }

   private List<Path> getApplicableFiles() {
      if (surveyManager.isOpen() && !files.isEmpty()) {
         return files.subList(1, files.size());
      } else {
         return files;
      }
   }

   private void populate(JMenu menu) {
      serialExecutor.discardWaitingJobs();
      menu.removeAll();
      List<Path> nonExistentFiles = new ArrayList<>();
      List<Path> applicableFiles = getApplicableFiles();
      for (int i = 0; i < applicableFiles.size(); i++) {
         Path file = applicableFiles.get(i);
         JMenuItem fileItem = menu.add(file.getFileName().toString());
         fileItem.setToolTipText(file.toString());
         int finalI = i;
         serialExecutor.execute(() -> {
            // Must be done in background since access to remote file systems may be very slow
            try {
               Document document = XmlUtils.readDocument(file);
               SurveyConfigurationXml xml = new SurveyConfigurationXml(document);
               String surveyTitle = xml.getSurveyTitle();
               String toolTipText = SurveyList.createToolTipText(file, xml);
               SwingUtilities.invokeLater(() -> {
                  if (surveyTitle != null) {
                     fileItem.setText(new HtmlStringBuilder().text(fileItem.getText()).html(" &nbsp; <span style='color:gray;'>").text(surveyTitle).html("</span>").build());
                     menu.getPopupMenu().pack();
                  }
                  fileItem.setToolTipText(toolTipText);
                  if (finalI < 9) {
                     fileItem.setMnemonic(KeyEvent.VK_0 + (finalI + 1));
                  }
               });
            } catch (IOException e) {
               if (FileUtils.notExists(e, file)) {
                  SwingUtilities.invokeLater(() -> {
                     fileItem.setEnabled(false);
                     if (nonExistentFiles.isEmpty()) {
                        menu.addSeparator();
                        JMenuItem clearNonExistentItem = MiscIcons.DELETE.on(menu.add("Clear non-existent entries"));
                        clearNonExistentItem.addActionListener(_ -> {
                           getApplicableFiles().removeAll(nonExistentFiles);
                           updateMenuEnabling();
                           savePreference();
                        });
                     }
                     nonExistentFiles.add(file);
                  });
               }
            }
         });
         fileItem.addActionListener(_ -> {
            if (surveyManager.isUnmodifiedOrUserApproved()) {
               surveyManager.open(file);
            }
         });
      }
      if (menu.getItemCount() == 0) {
         JMenuItem emptyItem = MiscIcons.EMPTY.on(menu.add("<Empty>"));
         emptyItem.setEnabled(false);
      } else {
         menu.addSeparator();
         JMenuItem clearListItem = MiscIcons.DELETE.on(menu.add("Clear list"));
         clearListItem.addActionListener(_ -> {
            getApplicableFiles().clear();
            updateMenuEnabling();
            savePreference();
         });
      }
   }

   private void savePreference() {
      String value = Joiner.on('\n').join(files);
      surveyManager.getPreferences().put(PREFERENCE_RECENTLY_OPENED, value);
   }

   private void readPreference() {
      String value = surveyManager.getPreferences().get(PREFERENCE_RECENTLY_OPENED, "");
      for (String path : Splitter.on('\n').omitEmptyStrings().trimResults().split(value)) {
         files.add(Path.of(path));
      }
   }
}
