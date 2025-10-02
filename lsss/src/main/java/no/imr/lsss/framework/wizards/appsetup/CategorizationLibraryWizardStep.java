package no.imr.lsss.framework.wizards.appsetup;

import no.imr.korona.computation.categorization.CategorizationFileService;
import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.AppPreprocessingConf;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.wizardry.WizardStep;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class CategorizationLibraryWizardStep extends WizardStep {
   private final LSSS lsss;

   CategorizationLibraryWizardStep(LSSS lsss) {
      super("Categorization library", LsssHelp.LSSS_SETUP);

      this.lsss = lsss;
   }

   @Override
   public JComponent getComponent() {
      GridBag gridBag = new GridBag()
            .configureVerticalBox();

      gridBag.add(new JLabel("""
            <html>
            <h1>Categorization library</h1>
            The categorization library is required by the KORONA categorization module.
            It is also used in LSSS to show the frequency response of KORONA categories.
            """));
      gridBag.add(Box.createVerticalStrut(30));

      AppPreprocessingConf appPreprocessingConf = lsss.getConfigurationManager().getApplicationConfiguration().getAppPreprocessingConf();
      AppPreprocessingConf.ConfigFileWrapper configFileWrapper = appPreprocessingConf.getConfigFileWrappers().stream()
            .filter(wrapper -> wrapper.getConfigFileService().getName().equals(CategorizationFileService.NAME))
            .findFirst()
            .orElseThrow();
      FileParameter fileParameter = configFileWrapper.file;
      ParameterEditor parameterEditor = new ParameterEditor(List.of(fileParameter));
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setInputFieldAlignment(GUIConfig.Alignment.LEFT);
      gridBag.add(parameterEditor.getEditorComponent());

      gridBag.add(Box.createVerticalStrut(30));
      JLabel infoLabel = new JLabel();
      gridBag.add(infoLabel);

      WhenShowingListening.connect(gridBag.getPanel(), fileParameter, () -> {
         Path file = fileParameter.getFile();
         boolean showInfo = file != null && Files.exists(file) &&
               new Configurator(file, null, null).getCategories().stream().allMatch(Category::isSpecial);
         if (showInfo) {
            infoLabel.setText("""
                  <html>
                  <strong style="color: red;">NB!</strong>
                  The chosen categorization library contains no training data.
                  You might want to use a different library with training data,
                  but that can also be changed at a later time.
                  """);
         }
         infoLabel.setVisible(showInfo);
      });

      return GuiUtils.createScrollPane(gridBag.getPanel());
   }
}
