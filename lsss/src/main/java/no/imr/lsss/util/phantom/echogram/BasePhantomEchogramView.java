package no.imr.lsss.util.phantom.echogram;

import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.lsss.modules.BaseOverlaidModule;
import no.imr.lsss.modules.echogram.EchogramZoomMouseWheelListener;
import no.imr.tools.swing.icons.MiscIcons;

import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.KeyListener;
import java.util.List;

public abstract class BasePhantomEchogramView extends BaseOverlaidModule.BaseOverlaidView {
   private final BasePhantomEchogramModule module;
   private final JPanel panel = new JPanel(new BorderLayout());

   protected BasePhantomEchogramView(BasePhantomEchogramModule module) {
      super(module);

      this.module = module;

      JComponent component = super.getComponent();
      component.addMouseWheelListener(new EchogramZoomMouseWheelListener(module.getPingSettings(), module.getZSettings(), module.getLSSS().getInterpretationSettings().getNavigationHistory()));

      panel.add(component);
      panel.add(module.getVerticalEchogramScrollBar().getComponent(), BorderLayout.EAST);
   }

   @Override
   public JComponent getComponent() {
      return panel;
   }

   @Override
   public JComponent getApiComponent() {
      return super.getComponent();
   }

   @Override
   protected KeyListener getKeyListener() {
      return new PhantomEchogramKeyListener(module);
   }

   protected JMenu createChannelMenu() {
      JMenu menu = new JMenu("Channel");
      List<RawFileTransducer> transducers = module.getPhantomEchogramSettings().getPhantomDataFileSet().getRawFileConfiguration().getTransducers();
      if (transducers.isEmpty()) {
         menu.setEnabled(false);
      } else {
         for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
            int channel = channelIndex + 1;
            RawFileTransducer transducer = transducers.get(channelIndex);
            boolean selected = module.getPhantomEchogramSettings().getChannel() == channel;
            JMenuItem item = MiscIcons.check(selected).on(menu.add(channel + "  –  " + transducer.getChannelId()));
            item.addActionListener(e -> module.getPhantomEchogramSettings().setChannel(channel));
         }
      }
      return menu;
   }
}
