package no.imr.lsss.framework.config.application.preview;

import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.HeaderParameter;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ViewHolder;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

final class PreviewFeaturesConfView implements ViewHolder.View {
   private final JComponent mainComponent;

   PreviewFeaturesConfView(PreviewFeaturesConf previewFeaturesConf) {
      JTextPane info = ConfigurationUnit.createInfoComponent("""
            <h2>Preview features</h2>
            <p>
               Preview features are intended for getting early feedback from end users,
               and are not meant for production use.
            </p>
            """);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(info, BorderLayout.NORTH);
      List<BaseParameter<?>> parameters = new ArrayList<>();
      parameters.add(previewFeaturesConf.showStatusBarIndicator);
      parameters.add(new HeaderParameter("Features"));
      List<BooleanParameter> previewParameters = previewFeaturesConf.getToggles().getParameters();
      parameters.addAll(previewParameters);
      mainPanel.add(previewFeaturesConf.createParameterEditor(parameters).getEditorComponent());
      if (previewParameters.isEmpty()) {
         JPanel newMainPanel = new JPanel(new BorderLayout());
         newMainPanel.add(mainPanel, BorderLayout.NORTH);
         newMainPanel.add(new JLabel("No preview features available at this time."));
         mainPanel = newMainPanel;
      }
      mainComponent = GuiUtils.createScrollPane(mainPanel);
   }

   @Override
   public JComponent getComponent() {
      return mainComponent;
   }
}
