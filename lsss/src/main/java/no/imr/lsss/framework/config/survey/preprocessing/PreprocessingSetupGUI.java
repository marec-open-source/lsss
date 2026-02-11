package no.imr.lsss.framework.config.survey.preprocessing;

import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.List;
import java.util.Optional;

final class PreprocessingSetupGUI {
   private final PreprocessingConf preprocessingConf;
   private final PreprocessingSetup preprocessingSetup;

   PreprocessingSetupGUI(PreprocessingSetup preprocessingSetup, PreprocessingConf preprocessingConf) {
      this.preprocessingSetup = preprocessingSetup;
      this.preprocessingConf = preprocessingConf;
   }

   JComponent getComponent() {
      GridBag gridBag = new GridBag();
      gridBag.activateHorizontalFill();
      gridBag.addWithLineBreak(createParameterEditor().getEditorComponent());
      gridBag.addWithLineBreak(createButtons());
      return gridBag.getPanel();
   }

   private ParameterEditor createParameterEditor() {
      ParameterEditor parameterEditor = new ParameterEditor(preprocessingSetup.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setParameterEnabledDecider(preprocessingConf::isParameterEnabled);
      return parameterEditor;
   }

   private JComponent createButtons() {
      Box east = Box.createHorizontalBox();
      east.add(createMoveUpButton());
      east.add(Box.createHorizontalStrut(5));
      east.add(createMoveDownButton());
      east.add(Box.createHorizontalStrut(5));
      east.add(createRemoveButton());

      JPanel panel = new JPanel(new BorderLayout());
      panel.add(createStartButton(), BorderLayout.WEST);
      panel.add(east, BorderLayout.EAST);
      return panel;
   }

   private JButton createStartButton() {
      JButton button = MiscIcons.PLAY.on(new JButton("Start preprocessing (KORONA)"));
      button.addActionListener(_ -> preprocessingSetup.start(Optional.empty()));
      return button;
   }

   private JButton createMoveUpButton() {
      JButton button = MiscIcons.ARROW_UP.on(new JButton());
      button.setToolTipText("Move up");
      button.setEnabled(preprocessingConf.getPreprocessingSetups().indexOf(preprocessingSetup) > 0);
      button.addActionListener(_ -> {
         preprocessingConf.moveProcessingSetup(preprocessingSetup, -1);
      });
      return button;
   }

   private JButton createMoveDownButton() {
      JButton button = MiscIcons.ARROW_DOWN.on(new JButton());
      button.setToolTipText("Move down");
      List<PreprocessingSetup> preprocessingSetups = preprocessingConf.getPreprocessingSetups();
      button.setEnabled(preprocessingSetups.indexOf(preprocessingSetup) + 1 < preprocessingSetups.size());
      button.addActionListener(_ -> {
         preprocessingConf.moveProcessingSetup(preprocessingSetup, 1);
      });
      return button;
   }

   private JButton createRemoveButton() {
      JButton button = MiscIcons.DELETE.on(new JButton());
      button.setToolTipText("Remove this setup");
      button.setEnabled(preprocessingConf.getPreprocessingSetups().size() > 1);
      button.addActionListener(_ -> {
         preprocessingConf.deleteProcessingSetup(preprocessingSetup);
      });
      return button;
   }
}
