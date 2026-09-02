package no.imr.tools.swing;

import com.google.common.util.concurrent.AtomicDouble;
import no.imr.tools.ProgressHandler;
import no.imr.tools.time.RemainingTimeEstimator;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.util.concurrent.atomic.AtomicInteger;

public final class ProgressView {
   private final GridBag panel = new GridBag();
   private final JPanel mainTextPanel = new JPanel(new BorderLayout());
   private final JProgressBar mainProgressBar = new JProgressBar();
   private final JLabel secondaryLabel = new JLabel(" ");
   private final JProgressBar secondaryProgressBar = new JProgressBar();
   private final JLabel timeLabel = new JLabel();

   private boolean mainProgressAsPercentage;
   private boolean mainProgressToolTip;

   private final AtomicInteger mainValue = new AtomicInteger();
   private final AtomicDouble secondaryFraction = new AtomicDouble(); // Negative is indeterminate
   private volatile String secondaryText = "";

   private final Runnable updater = this::updateNow;

   private final ProgressHandler mainProgressHandler = this::setMainProgressAsFraction;
   private final ProgressHandler secondaryProgressHandler = this::setSecondaryProgressAsFraction;

   public ProgressView(String mainText, int mainMax) {
      this(GuiUtils.makeMultiLineLabel(mainText), mainMax);
   }

   public ProgressView(Component mainTextComponent, int mainMax) {
      mainTextPanel.add(mainTextComponent);

      mainProgressBar.setMaximum(mainMax);
      mainProgressBar.setStringPainted(true);
      mainProgressBar.setString("0 / " + mainMax);

      secondaryProgressBar.setMaximum(1000000000);
      secondaryProgressBar.setStringPainted(true);
      secondaryProgressBar.setString("0 %");

      panel.configureVerticalBox();
      panel.add(mainTextPanel);
      panel.add(mainProgressBar);
   }

   public ProgressView mainProgressAsPercentage() {
      mainProgressAsPercentage = true;
      mainProgressBar.setString("0 %");
      return this;
   }

   public ProgressView mainProgressToolTip() {
      mainProgressToolTip = true;
      return this;
   }

   public ProgressView hideMainProgressLabel() {
      mainProgressBar.setStringPainted(false);
      return this;
   }

   public ProgressView useSecondaryLabel() {
      panel.add(secondaryLabel);
      return this;
   }

   public ProgressView useSecondaryProgress() {
      panel.add(secondaryLabel);
      panel.add(secondaryProgressBar);
      return this;
   }

   public ProgressView showRemainingTime() {
      long totalWork = (long) mainProgressBar.getMaximum() * secondaryProgressBar.getMaximum();
      RemainingTimeEstimator remainingTimeEstimator = new RemainingTimeEstimator(totalWork);

      Timer timer = new Timer(1000, _ -> updateTimeText(remainingTimeEstimator));

      timeLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
      timeLabel.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            updateTimeText(remainingTimeEstimator);
            if (timeLabel.isShowing()) {
               timer.start();
            } else {
               timer.stop();
            }
         }
      });

      panel.add(timeLabel);

      updateTimeText(remainingTimeEstimator);

      return this;
   }

   private void updateTimeText(RemainingTimeEstimator remainingTimeEstimator) {
      long remainingWork = (long) (mainProgressBar.getMaximum() - mainProgressBar.getValue()) * secondaryProgressBar.getMaximum() - secondaryProgressBar.getValue();
      remainingTimeEstimator.setRemainingWork(remainingWork);
      String text = "<html>" + remainingTimeEstimator.getHtmlTable();
      timeLabel.setText(text);
   }

   public JComponent getComponent() {
      return panel.getPanel();
   }

   private void updateLater() {
      SwingDelayer.invokeLater(this, updater);
   }

   private void updateNow() {
      updateNow(mainValue.get(), secondaryText, secondaryFraction.get());
   }

   private void updateNow(int mainValue, String secondaryText, double secondaryFraction) {
      mainProgressBar.setValue(mainValue);
      mainProgressBar.setString(getValueText(mainProgressBar, mainProgressAsPercentage));
      if (mainProgressToolTip) {
         mainProgressBar.setToolTipText(getValueText(mainProgressBar, !mainProgressAsPercentage));
      }

      secondaryLabel.setText(secondaryText);

      if (secondaryFraction < 0) {
         secondaryProgressBar.setIndeterminate(true);
         secondaryProgressBar.setValue(0);
         secondaryProgressBar.setString("");
      } else {
         if (secondaryProgressBar.isIndeterminate()) {
            secondaryProgressBar.setIndeterminate(false);
         }
         secondaryProgressBar.setValue((int) (secondaryFraction * secondaryProgressBar.getMaximum()));
         secondaryProgressBar.setString((int) Math.floor(secondaryFraction * 100) + " %");
      }
   }

   private static String getValueText(JProgressBar progressBar, boolean asPercentage) {
      int value = progressBar.getValue();
      int max = progressBar.getMaximum();
      return asPercentage && max > 0
            ? 100L * value / max + " %"
            : value + " / " + max;
   }

   public void setMainText(String mainText) {
      SwingUtilities.invokeLater(() -> {
         GuiUtils.replaceContent(mainTextPanel, new JLabel(mainText));
      });
   }

   public void setMainProgress(int mainValue, String secondaryText) {
      this.mainValue.set(mainValue);
      this.secondaryText = secondaryText;
      updateLater();
   }

   public void incrementMainProgress(String secondaryText) {
      addMainProgress(1, secondaryText);
   }

   public void addMainProgress(int mainValueAddition, String secondaryText) {
      mainValue.addAndGet(mainValueAddition);
      this.secondaryText = secondaryText;
      secondaryFraction.set(0);
      updateLater();
   }

   public ProgressHandler getMainProgressHandler() {
      return mainProgressHandler;
   }

   public ProgressHandler getSecondaryProgressHandler() {
      return secondaryProgressHandler;
   }

   public void setSecondaryText(String secondaryText) {
      this.secondaryText = secondaryText;
      updateLater();
   }

   public void setSecondaryIndeterminate() {
      secondaryFraction.set(-1);
      updateLater();
   }

   private void setSecondaryProgressAsFraction(double fraction) {
      secondaryFraction.set(fraction);
      updateLater();
   }

   private void setMainProgressAsFraction(double fraction) {
      mainValue.set((int) (fraction * mainProgressBar.getMaximum()));
      updateLater();
   }
}
