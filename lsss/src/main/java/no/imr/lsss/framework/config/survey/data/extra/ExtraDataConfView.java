package no.imr.lsss.framework.config.survey.data.extra;

import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.resources.LsssHelp;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ScrollablePanel;
import no.imr.tools.swing.ViewHolder;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;

final class ExtraDataConfView implements ViewHolder.View {
   private final ExtraDataConf extraDataConf;

   private final JComponent mainComponent;
   private final JPanel inputPanel = new JPanel(new GridLayout(0, 1));
   private final Listener updateListener = this::possiblyUpdate;

   ExtraDataConfView(ExtraDataConf extraDataConf) {
      this.extraDataConf = extraDataConf;

      JTextPane info = ConfigurationUnit.createInfoComponent("""
            <h2>Extra data directories</h2>
            <p>
               Extra data directories used when
               <a href="backupSurvey">backing up survey data</a>.
            </p>
            <p>
               The <em>Relative path</em> refers to the destination directory for the copying.
               If left empty, it will be
               <a href="determinedAutomatically">determined automatically</a>.
            </p>
            """);
      GuiUtils.addHrefListener(info, href -> {
         switch (href) {
            case "backupSurvey" -> LsssHelp.BACKUP_SURVEY.show();
            case "determinedAutomatically" -> extraDataConf.getHelpID().show();
            default -> {
            }
         }
      });

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(info, BorderLayout.NORTH);
      mainPanel.add(inputPanel);

      mainComponent = GuiUtils.createScrollPane(mainPanel);

      update();
   }

   @Override
   public JComponent getComponent() {
      return mainComponent;
   }

   private void possiblyUpdate() {
      if (extraDataConf.normalize()) {
         update();
      }
   }

   private void update() {
      List<ExtraDataDir> extraDataDirs = extraDataConf.getExtraDataDirs();
      for (int i = 0; i < extraDataDirs.size(); i++) {
         ExtraDataDir extraDataDir = extraDataDirs.get(i);
         while (i < inputPanel.getComponentCount() && ((JComponent) inputPanel.getComponent(i)).getClientProperty(extraDataDir) == null) {
            inputPanel.remove(i);
         }
         if (i == inputPanel.getComponentCount()) {
            inputPanel.add(createParameterEditor(extraDataDir));
         }
      }
      while (inputPanel.getComponentCount() > extraDataDirs.size()) {
         inputPanel.remove(inputPanel.getComponentCount() - 1);
      }
      inputPanel.revalidate();
      inputPanel.repaint();
   }

   private JComponent createParameterEditor(ExtraDataDir extraDataDir) {
      ParameterEditor parameterEditor = new ParameterEditor(extraDataDir.getParameters(), new GUIConfig()
            .setTextInputColumns(30)
            .setTextAlignment(GUIConfig.Alignment.LEFT)
      );
      parameterEditor.getParameterChangeManager().addListener(updateListener);
      ScrollablePanel component = parameterEditor.getEditorComponent();
      component.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));
      component.putClientProperty(extraDataDir, extraDataDir);
      return component;
   }
}
