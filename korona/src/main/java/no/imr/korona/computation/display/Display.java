package no.imr.korona.computation.display;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.viewer.DepthRangeChooser;
import no.imr.korona.viewer.DepthRangePanel;
import no.imr.korona.viewer.EchogramColorPanel;
import no.imr.korona.viewer.KoronaPlaybox;
import no.imr.tools.swing.MultiSplitPane;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class Display {
   private final JPanel panel = new JPanel(new BorderLayout());
   private final DepthRangeChooser depthRangeChooser = new DepthRangeChooser();
   private final List<EchogramColorPanel> echogramColorPanels = new ArrayList<>();
   private final MultiSplitPane multiSplitPane = new MultiSplitPane(JSplitPane.HORIZONTAL_SPLIT);
   private @Nullable KoronaPlaybox koronaPlaybox;
   private @Nullable PingConfiguration pingConfiguration;

   Display() {
      multiSplitPane.getPanel().setMinimumSize(new Dimension(100, 100));

      panel.add(multiSplitPane.getPanel());
      panel.add(new DepthRangePanel(depthRangeChooser).getComponent(), BorderLayout.EAST);
   }

   JComponent getComponent() {
      return panel;
   }

   void setKoronaPlaybox(KoronaPlaybox koronaPlaybox) {
      this.koronaPlaybox = koronaPlaybox;
      init();
   }

   void setPingConfiguration(PingConfiguration pingConfiguration) {
      this.pingConfiguration = pingConfiguration;
      init();
   }

   private void init() {
      if (koronaPlaybox == null || pingConfiguration == null) {
         return;
      }

      RawFileConfiguration rawFileConfiguration = pingConfiguration.getRawFileConfiguration();
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();

      if (transducers.size() != echogramColorPanels.size()) {
         echogramColorPanels.clear();
         Consumer<JPopupMenu> popupMenuExtender = this::extendPopupMenu;
         List<JComponent> components = new ArrayList<>();
         for (int channel = 1; channel <= transducers.size(); channel++) {
            EchogramColorPanel echogramColorPanel = new EchogramColorPanel(koronaPlaybox.getColorConverterContainer(), depthRangeChooser, pingConfiguration, channel);
            echogramColorPanel.getEchogramPanel().installDefaultListener(koronaPlaybox);
            echogramColorPanel.getEchogramPanel().addPopupMenuExtender(popupMenuExtender);
            echogramColorPanels.add(echogramColorPanel);
            components.add(echogramColorPanel.getComponent());
         }

         multiSplitPane.clear();
         multiSplitPane.addAll(components);
      }

      depthRangeChooser.reset(transducers.size());

      for (EchogramColorPanel echogramColorPanel : echogramColorPanels) {
         echogramColorPanel.setPingConfiguration(pingConfiguration, koronaPlaybox.getConfigFileSettings());
      }

      panel.repaint();
   }

   void addPing(Ping ping) {
      if (koronaPlaybox == null) {
         return;
      }

      for (ChannelData channelData : ping.getChannelDatas()) {
         if (channelData != null) {
            depthRangeChooser.addDepthRange(channelData.getChannel() - 1, channelData.getDepthRange());
         }
      }
      depthRangeChooser.computeViewRange();

      for (EchogramColorPanel echogramColorPanel : echogramColorPanels) {
         echogramColorPanel.getEchogramPanel().addPing(ping);
      }

      panel.repaint();
   }

   private void extendPopupMenu(JPopupMenu popupMenu) {
      popupMenu.addSeparator();

      JMenuItem changeDirectionItem = popupMenu.add("Change split direction");
      changeDirectionItem.setEnabled(echogramColorPanels.size() > 1);
      changeDirectionItem.addActionListener(_ -> multiSplitPane.changeSplitDirection());

      JMenuItem resetSplittersItem = popupMenu.add("Reset splitters");
      resetSplittersItem.setEnabled(echogramColorPanels.size() > 1);
      resetSplittersItem.addActionListener(_ -> multiSplitPane.distributeEvenly());
   }
}
