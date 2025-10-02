package no.imr.korona.viewer;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.viewer.coloring.ColorConverterContainer;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Class to contain echogram panel and colormap panel.
 */
public final class EchogramColorPanel {
   private final JPanel panel = new JPanel(new BorderLayout());
   private final EchogramPanel echogramPanel;
   private final SvColorPanel svColorPanel;

   public EchogramColorPanel(ColorConverterContainer colorConverterContainer, DepthRangeChooser depthRangeChooser, PingConfiguration pingConfiguration, int channel) {
      ColorConverterContainer privateColorConverterContainer = new ColorConverterContainer();

      echogramPanel = new EchogramPanel(colorConverterContainer, privateColorConverterContainer, this, depthRangeChooser, pingConfiguration, channel);

      svColorPanel = new SvColorPanel(privateColorConverterContainer);
      svColorPanel.setUseAdvancedDialog(true);

      panel.add(echogramPanel.getComponent(), BorderLayout.CENTER);
      panel.add(svColorPanel.getComponent(), BorderLayout.WEST);

      setColorPanelVisible(false);
   }

   public JComponent getComponent() {
      return panel;
   }

   public void setColorPanelVisible(boolean visible) {
      svColorPanel.getComponent().setVisible(visible);
   }

   public boolean isColorPanelVisible() {
      return svColorPanel.getComponent().isVisible();
   }

   public EchogramPanel getEchogramPanel() {
      return echogramPanel;
   }

   public SvColorPanel getSvColorPanel() {
      return svColorPanel;
   }

   public void setPingConfiguration(PingConfiguration pingConfiguration, ConfigFileSettings configFileSettings) {
      echogramPanel.setPingConfiguration(pingConfiguration);
      svColorPanel.setConfigurationItems(pingConfiguration.getConfigurationItems(), configFileSettings);
   }
}
