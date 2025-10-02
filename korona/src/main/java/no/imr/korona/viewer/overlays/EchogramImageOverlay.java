package no.imr.korona.viewer.overlays;

import no.imr.korona.data.ChannelSelector;
import no.imr.korona.data.buffer.BoundedPingBuffer;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.viewer.EchogramImage;
import no.imr.korona.viewer.coloring.PingToColor;
import no.imr.tools.listening.Listener;
import no.imr.tools.range.FloatRange;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.marec.lsss.api.util.observing.Subscription;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.Supplier;

/**
 * Displays echogram image - Sv or categories.
 */
public final class EchogramImageOverlay extends EchogramOverlay {
   private final Supplier<JComponent> settings;
   private List<Subscription> subscriptions = List.of();
   private EchogramImage echogramImage = new EchogramImage(this, new BoundedPingBuffer(0),
         ChannelSelector.channelOne(), FloatRange.EMPTY_RANGE, PingToColor.multi(List.of()));

   public EchogramImageOverlay(Supplier<JComponent> settings) {
      this.settings = settings;
   }

   @Override
   public void init() {
      super.init();

      echogramImage = new EchogramImage(this, getPingBuffer(), getEchogramDisplay().getChannelSelector(), getEchogramDisplay().getDepthRange(), getEchogramDisplay().getConverterContainer());

      subscriptions.forEach(Subscription::unsubscribe);
      subscriptions = List.of(
            getPingAnimator().getPingChangeManager().subscribe(Listener.of(this::gotoEnd))
      );
   }

   @Override
   public void close() {
      subscriptions.forEach(Subscription::unsubscribe);
      super.close();
   }

   private void gotoEnd() {
      PingIndex pingIndex = getPingAnimator().getLastPingIndex();
      if (pingIndex != null) {
         echogramImage.setEndPingIndex(pingIndex);
      }
   }

   @Override
   public void resized(GraphicsConfiguration graphicsConfiguration, int width, int height) {
      echogramImage.dispose();
      echogramImage = new EchogramImage(this, getPingBuffer(), getEchogramDisplay().getChannelSelector(), getEchogramDisplay().getDepthRange(), getEchogramDisplay().getConverterContainer(),
            graphicsConfiguration, width, height);
      gotoEnd();
   }

   @Override
   public void draw(Graphics2D g2d) {
      echogramImage.paint(g2d);
   }

   @Override
   public void channelChanged() {
      echogramImage.setChannel(getEchogramDisplay().getChannelSelector());
   }

   @Override
   public void depthRangeChanged() {
      echogramImage.setDepthRange(getEchogramDisplay().getDepthRange());
   }

   @Override
   public void svRangeChanged() {
      echogramImage.redraw();
   }

   @Override
   public void refresh() {
      echogramImage.redraw();
   }

   @Override
   public boolean overlaps(Rectangle2D rectangle2D) {
      return true;
   }

   @Override
   public JPopupMenu getPopupMenu(Point point) {
      JPopupMenu popupMenu = new JPopupMenu();
      JMenuItem menuItem = MiscIcons.SETTINGS.on(popupMenu.add("Configure echogram..."));
      menuItem.addActionListener(e -> showConfigurationDialog());
      return popupMenu;
   }

   private void showConfigurationDialog() {
      JDialog dialog = new JDialog(GuiUtils.windowForComponent(getEchogramDisplay().getComponent()), "Configure echogram", Dialog.ModalityType.DOCUMENT_MODAL);

      JButton closeButton = new JButton("Close");
      closeButton.addActionListener(e -> dialog.dispose());
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(closeButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(settings.get());
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
      dialog.getRootPane().setDefaultButton(closeButton);
      dialog.add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(getEchogramDisplay().getComponent());
      dialog.setVisible(true);
   }
}
