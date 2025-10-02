package no.imr.lsss.framework.backup;

import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

final class RepeatedCopyGui {
   private final @Nullable Duration repeatInterval;
   private final Supplier<CopyGui> copyGuiSupplier;
   private final JDialog dialog;
   private final JPanel contentPanel = new JPanel(new BorderLayout());
   private final JButton stopCurrentCopyButton = new JButton("Stop current copy");
   private final JButton startNextCopyButton = new JButton("Start next copy");
   private final Timer waitTimer = new Timer(1000, e -> waitTimerTick());
   private final JLabel remainingWaitTimeLabel = new JLabel();
   private Instant nextCopyTime = Instant.now();
   private AsyncHandle asyncHandle = new AsyncHandle();

   RepeatedCopyGui(@Nullable Component referenceComponent, String title, JLabel info,
                   @Nullable Duration repeatInterval, Supplier<CopyGui> copyGuiSupplier) {
      this.repeatInterval = repeatInterval;
      this.copyGuiSupplier = copyGuiSupplier;

      dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.MODELESS);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            close();
         }
      });

      JButton cancelButton = new JButton("Cancel");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE);
      cancelButton.addActionListener(e -> close());

      stopCurrentCopyButton.addActionListener(e -> stopCopy());
      startNextCopyButton.addActionListener(e -> startCopy());

      JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      if (repeatInterval != null) {
         buttonsPanel.add(startNextCopyButton);
         buttonsPanel.add(stopCurrentCopyButton);
      }
      buttonsPanel.add(cancelButton);

      info.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
      contentPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.GRAY),
            BorderFactory.createEmptyBorder(10, 0, 5, 0)));
      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), GuiUtils.DEFAULT_MARGIN));
      topPanel.add(info, BorderLayout.NORTH);
      topPanel.add(contentPanel);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(topPanel);
      mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

      startCopy();

      dialog.add(mainPanel);
      dialog.pack();
      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
   }

   private void stopCopy() {
      asyncHandle.cancel();
      asyncHandle = new AsyncHandle();
   }

   private void close() {
      asyncHandle.cancel();
      waitTimer.stop();
      dialog.dispose();
   }

   private void startCopy() {
      stopCopy();
      waitTimer.stop();
      stopCurrentCopyButton.setVisible(true);
      startNextCopyButton.setVisible(false);
      CopyGui copyGui = copyGuiSupplier.get();
      GuiUtils.replaceContent(contentPanel, copyGui.getComponent());
      Exec.LOW_PRIORITY_CACHED_THREAD_POOL.execute(() -> {
         copyGui.run(asyncHandle);
         SwingUtilities.invokeLater(this::afterCopy);
      });
   }

   private void afterCopy() {
      if (repeatInterval == null || asyncHandle.isCancelled()) {
         close();
         return;
      }
      nextCopyTime = Instant.now().plus(repeatInterval);
      stopCurrentCopyButton.setVisible(false);
      startNextCopyButton.setVisible(true);
      GridBag gridBag = new GridBag();
      gridBag.addWithLineBreak(new JLabel("Time until next copy:"));
      gridBag.addWithLineBreak(Box.createVerticalStrut(5));
      gridBag.addWithLineBreak(remainingWaitTimeLabel);
      GuiUtils.replaceContent(contentPanel, gridBag.getPanel());
      waitTimer.start();
      waitTimerTick();
   }

   private void waitTimerTick() {
      long waitMillis = Instant.now().until(nextCopyTime, ChronoUnit.MILLIS);
      if (waitMillis <= 0) {
         startCopy();
      } else {
         remainingWaitTimeLabel.setText(Utils.getDurationString(waitMillis));
      }
   }
}
