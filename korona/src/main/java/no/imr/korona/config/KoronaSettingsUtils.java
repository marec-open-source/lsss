package no.imr.korona.config;

import com.google.common.html.HtmlEscapers;
import no.imr.korona.Korona;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.gui.ConfigurableGUI;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ProgressView;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.WorkerDialog;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class KoronaSettingsUtils {
   private KoronaSettingsUtils() {
   }

   public static boolean showMainConfigDirDialog(Korona korona, @Nullable Component referenceComponent, Path dir) {
      KoronaSettings koronaSettings = korona.getKoronaSettings();
      if (koronaSettings.getKoronaConfigDir().getFile() == null) {
         koronaSettings.getKoronaConfigDir().setFile(dir);
      }
      JComponent gui = new ConfigurableGUI()
            .setParameterEditorAdaptor(parameterEditor -> {
               parameterEditor.getGUIConfig().setHorizontalFill(true);
            })
            .createComponent(koronaSettings.getKoronaConfigDir());
      String infoText = """
            <html>
            <body style="margin: 0.5cm; width: 500px;">
            <h3>Please select/create the default preprocessor (KORONA) configuration directory.</h3>
            <p>
               The directory is being populated by default configuration files from the LSSS distribution, but
               should later be populated by general institute-wide (e.g. IMR or NOAA/AFSC) or sea area related
               (e.g. North Sea) configuration files. This is because the features used for categorization
               (="Species identification") are trained from selected species, so that a general training
               from e.g. Norwegian waters would not work optimally in e.g. Japanese waters.
               Some examples of preprocessor configuration files:
            </p>
            <ul>
               <li>Default transducer configuration</li>
               <li>Default pulse transmission delay</li>
               <li>Dummy or real features used for species categorization</li>
               <li>Default max range for frequency (to be used in species categorization)</li>
               <li>Example of module setup for the preprocessor KORONA</li>
               <li>Default length groups for each model to be used in zooplankton size estimation</li>
            </ul>
            """;
      boolean ok = new ConfigurableGUIDialog(referenceComponent, "Default config directory", koronaSettings)
            .setTopText(infoText)
            .setGUI(gui)
            .show();
      if (ok) {
         koronaSettings.save();
      }
      return ok;
   }

   public static boolean showMainConfigDirDialogIfNecessary(Korona korona, @Nullable Component referenceComponent, Path defaultPath) {
      boolean newDir = false;

      if (korona.getKoronaSettings().getKoronaConfigDir().getFile() == null) {
         JOptionPane.showMessageDialog(referenceComponent, "Default config directory is not set.\nPlease set it now.");
         showMainConfigDirDialog(korona, referenceComponent, defaultPath);
         newDir = true;
      }

      copyNewOrMissingConfigFiles(korona, referenceComponent, true);

      return newDir;
   }

   public static void copyNewOrMissingConfigFiles(Korona korona, @Nullable Component referenceComponent, boolean interactiveMode) {
      Path destDir = korona.getKoronaSettings().getKoronaConfigDir().getFile();
      if (destDir == null) {
         return;
      }

      FilesToCopy filesToCopy = new FilesToCopy(ConfigFileCopier.getLastModified());
      new WorkerDialog(referenceComponent, "Searching for new config files...")
            .startWithoutCancel(() -> {
               for (ConfigFileService service : korona.createConfigFileSettings().getFileServices()) {
                  try {
                     service.addInstallationConfigFilesToCopy(filesToCopy, destDir);
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, "Error accessing installation config files for " + service.getName().persistentName(), e);
                  }
               }
            });

      if (filesToCopy.getFilesToCopy().isEmpty() && filesToCopy.getExistingFiles().isEmpty()) {
         // Update last modified in case som source files have newer timestamps, but are equal.
         ConfigFileCopier.updateLastModified(filesToCopy);
         return;
      }

      if (interactiveMode && !filesToCopy.getExistingFiles().isEmpty()) {
         JPanel panel = new JPanel(new BorderLayout());
         panel.add(new JLabel("<html>"
               + "Newer versions of existing config files in"
               + "<br>" + HtmlEscapers.htmlEscaper().escape(destDir.toString())
               + "<br>are available:<br><br>"), BorderLayout.NORTH);
         String fileList = filesToCopy.getExistingFiles().keySet().stream()
               .map(Path::toString)
               .sorted()
               .map(HtmlEscapers.htmlEscaper().asFunction())
               .collect(Collectors.joining("<br>", "<html>", ""));
         JComponent fileListComponent = VerticalScrollablePanel.wrap(new JLabel(fileList));
         fileListComponent.setBorder(GuiUtils.DEFAULT_MARGIN);
         JScrollPane scrollPane = new JScrollPane(fileListComponent);
         Dimension preferredSize = scrollPane.getPreferredSize();
         int maxHeight = 800;
         if (preferredSize.height > maxHeight) {
            int width = preferredSize.width + scrollPane.getVerticalScrollBar().getPreferredSize().width;
            scrollPane.setPreferredSize(new Dimension(width, maxHeight));
         }
         panel.add(scrollPane);
         panel.add(new JLabel("<html><br>Replace existing files?"), BorderLayout.SOUTH);

         int answer = JOptionPane.showConfirmDialog(referenceComponent, panel, "Overwrite existing config files?",
               JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
         if (answer == JOptionPane.YES_OPTION) {
            filesToCopy.getFilesToCopy().putAll(filesToCopy.getExistingFiles());
         }
      }

      try {
         FileUtils.createDirectories(destDir);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error creating " + destDir, e);
         return;
      }

      ProgressView progressView = new ProgressView("<html>Copying config files to<br>" + destDir, 1000)
            .mainProgressAsPercentage();
      new WorkerDialog(referenceComponent, progressView.getComponent())
            .startWithoutCancel(() -> {
               ConfigFileCopier.copy(filesToCopy, progressView.getMainProgressHandler(), new AsyncHandle());
            });
   }
}
