package no.imr.lsss.framework.wizards.appsetup;

import no.imr.korona.config.KoronaSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.io.FileUtils;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class DirectoryWizardStep extends WizardStep {
   private static final String KORONA_CONFIG_DIR_NAME = "config";

   private final FileParameter mainDir;
   private final KoronaSettings koronaSettings;
   private final JLabel exampleLabel = new JLabel();

   DirectoryWizardStep(LSSS lsss) {
      super("Main directory", LsssHelp.LSSS_SETUP);

      mainDir = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf().mainDir;
      koronaSettings = lsss.getKorona().getKoronaSettings();

      WhenShowingListening.connect(exampleLabel, mainDir, this::updateExampleLabel);
   }

   private void updateExampleLabel() {
      Path dir = mainDir.getFile();
      String text;
      if (dir == null || Files.exists(dir) && !Files.isDirectory(dir)) {
         text = "<html>"
               + "<p style='color: red'>Please select a directory!";
      } else {
         Path surveyDir = dir.resolve("S2006123PTestPlatform[1234]");
         Path configDir = dir.resolve(KORONA_CONFIG_DIR_NAME);
         text = "<html>"
               + "<p style='font-size: larger; margin-top: 10px;'>Example survey directory</p>"
               + "<div style='margin: 3px 0;'><code>" + surveyDir + "</code></div>"
               + "<p>Where <code>2006123</code> is the survey number, <code>TestPlatform</code> is the platform name, and <code>1234</code> is the platform number."
               + "</p>"
               + "<br>"
               + "<p style='font-size: larger; margin-top: 10px;'>Directory for configuration files</p>"
               + "<div style='margin: 3px 0;'><code>" + configDir + "</code></div>"
               + "<p>Some of these configuration files will be copied to the survey directory when creating a new survey."
               + "</p>";
      }
      exampleLabel.setText(text);
   }

   @Override
   public JComponent getComponent() {
      GridBag gridBag = new GridBag()
            .configureVerticalBox();

      gridBag.add(new JLabel("""
            <html>
            <h1>Main directory</h1>
            <p>The main directory is used as the default location for new surveys,
            and for configuration files.</p>
            """));
      gridBag.add(Box.createVerticalStrut(30));

      ParameterEditor parameterEditor = new ParameterEditor(List.of(mainDir));
      gridBag.add(parameterEditor.getEditorComponent());

      gridBag.add(Box.createVerticalStrut(25));
      gridBag.add(exampleLabel);

      gridBag.addVerticalFiller();

      return GuiUtils.createScrollPane(gridBag.getPanel());
   }

   @Override
   public boolean onNext() {
      Path dir = mainDir.getFile();
      if (dir == null || Files.exists(dir) && !Files.isDirectory(dir)) {
         JOptionPane.showMessageDialog(getWizard().getDialog(), "Please select a directory!");
         return false;
      }
      try {
         FileUtils.createDirectories(dir);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(getWizard().getDialog(), "Error creating " + dir, e);
         return false;
      }
      koronaSettings.getKoronaConfigDir().setFile(dir.resolve(KORONA_CONFIG_DIR_NAME));
      return true;
   }

   @Override
   public void apply() {
      koronaSettings.save();
   }
}
