package no.imr.lsss.viewer;

import no.imr.korona.computation.ConcurrentPingModuleComputation;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.datamanager.PingSampler;
import no.imr.korona.data.track.SegmentData;
import no.imr.korona.data.util.DiscardZeroDepthSegmentData;
import no.imr.korona.data.util.ProcessingSegmentData;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.DataLoadingMode;
import no.imr.lsss.framework.config.application.preview.PreviewFeaturesConf;
import no.imr.lsss.framework.config.survey.preprocessing.OnTheFlySetup;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.statusbar.StatusBar;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

final class MainStatusBar {
   private static final int HEIGHT = 20;

   private final LSSS lsss;
   private final StatusBar statusBar;
   private final JLabel statusTextLabel = new JLabel();
   private final JLabel surveyTextLabel = new JLabel();
   private final Component previewSeparator = StatusBar.createSeparator();
   private final JLabel previewLabel = new JLabel("Preview features");
   private final Component frozenSeparator = StatusBar.createSeparator();
   private final JLabel frozenLabel = new JLabel("Frozen");
   private final Component onTheFlySeparator = StatusBar.createSeparator();
   private final JLabel onTheFlyLabel = new JLabel("On the fly");
   private final JLabel modeLabel = new JLabel();
   private final JLabel databaseLabel = new DatabaseLabel();
   private final JProgressBar progressBar = new JProgressBar();
   private boolean hasData;
   private boolean isLoading;
   private boolean isComputing;
   private @Nullable Object isComputingAfterDelayTester;

   MainStatusBar(LSSS lsss) {
      this.lsss = lsss;

      statusBar = new StatusBar()
            .addTopBorder()
            .addSpace()
            .add(statusTextLabel)
            .addFiller()
            .add(previewSeparator)
            .add(previewLabel)
            .add(frozenSeparator)
            .add(frozenLabel)
            .add(onTheFlySeparator)
            .add(onTheFlyLabel)
            .addSeparator()
            .add(surveyTextLabel)
            .addSeparator()
            .add(modeLabel)
            .addSeparator()
            .add(databaseLabel)
            .addSpace()
            .add(progressBar);

      previewSeparator.setVisible(false);
      previewLabel.setVisible(false);
      previewLabel.setOpaque(true);
      previewLabel.setBackground(ColorUtils.YELLOWGREEN);
      previewLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
      previewLabel.setToolTipText("Preview features are activated");
      previewLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      previewLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               lsss.getConfigurationManager().getAppMiscConf().getPreviewFeaturesConf().showInConfigurationDialog();
            }
         }
      });

      frozenSeparator.setVisible(false);
      frozenLabel.setVisible(false);
      frozenLabel.setOpaque(true);
      frozenLabel.setBackground(ColorUtils.TOMATO);
      frozenLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
      frozenLabel.setToolTipText("<html>Selected ping and geographical position is frozen<br>Click to unfreeze");
      frozenLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      frozenLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               lsss.getInterpretationSettings().mouseover().setFrozen(false);
            }
         }
      });

      onTheFlySeparator.setVisible(false);
      onTheFlyLabel.setVisible(false);
      onTheFlyLabel.setOpaque(true);
      onTheFlyLabel.setBackground(ColorUtils.KHAKI);
      onTheFlyLabel.setBorder(BorderFactory.createEmptyBorder(0, 3, 0, 3));
      onTheFlyLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      onTheFlyLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               OnTheFlySetup onTheFlySetup = lsss.getConfigurationManager().getSurveyConfiguration().getPreprocessingConf().getOnTheFlySetup();
               if (onTheFlySetup != null) {
                  onTheFlySetup.showEditor(lsss.getReferenceComponent(), e.isControlDown());
               }
            }
         }
      });

      surveyTextLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      surveyTextLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               lsss.getConfigurationManager().getSurveyConf().showInConfigurationDialog();
            }
         }
      });

      setFixedSize(modeLabel, new Dimension(55, HEIGHT));
      modeLabel.setToolTipText("Click to change mode");
      modeLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      modeLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               DataLoadingMode dataLoadingMode = lsss.getInterpretationSettings().getDataLoadingMode();
               lsss.getInterpretationSettings().setDataLoadingMode(Utils.shift(dataLoadingMode, 1));
            }
         }
      });

      setFixedSize(databaseLabel, new Dimension(55, HEIGHT));
      databaseLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      databaseLabel.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
               lsss.getConfigurationManager().getApplicationConfiguration().getDatabaseConf().showInConfigurationDialog();
            }
         }
      });

      setFixedSize(progressBar, new Dimension(100, HEIGHT));
      progressBar.setStringPainted(true);
      progressBar.setString("");
   }

   private static void setFixedSize(Component component, Dimension size) {
      component.setMinimumSize(size);
      component.setPreferredSize(size);
   }

   JComponent getComponent() {
      return statusBar.getComponent();
   }

   void setup() {
      GuiListeners.coalescingLater(this::updateProgressBar).addToAndNotify(
            lsss.getInterpretationSettings().getPingRangeChangeManager(),
            lsss.getInterpretationSettings().getPingSampler().getNewPingsChangeManager()
      );
      GuiListeners.coalescingLater(this::updatePreviewLabel).addToAndNotify(
            lsss.getConfigurationManager().getAppMiscConf().getPreviewFeaturesConf().showStatusBarIndicator,
            lsss.getConfigurationManager().getAppMiscConf().getPreviewFeaturesConf().getToggles().getChangeManager()
      );
      GuiListeners.coalescingLater(this::updateFrozenLabel).addToAndNotify(
            lsss.getInterpretationSettings().mouseover().frozen()
      );
      GuiListeners.coalescingLater(this::updateOnTheFlyLabel).addToAndNotify(
            lsss.getInterpretationSettings().getDataFileChangeManager()
      );
      GuiListeners.coalescingLater(this::updateModeLabel).addToAndNotify(
            lsss.getInterpretationSettings().getDataLoadingModeChangeManager()
      );
      GuiListeners.coalescingLater(this::updateIsComputing).addToAndNotify(
            lsss.getInterpretationSettings().getExecutorObservation().getChangeManager()
      );
      GuiListeners.coalescingLater(this::updateDatabaseLabel).addToAndNotify(
            lsss.getDatabaseManager().getConnectionChangeManager(),
            lsss.getDatabaseManager().getBusyChangeManager()
      );
      GuiListeners.coalescingLater(this::updateSurveyLabel).addToAndNotify(
            lsss.getConfigurationManager().getSurveyConf().mSurvey
      );
      GuiListeners.coalescingLater(this::updateVisibility).addToAndNotify(
            lsss.getActions().showStatusBar.getChangeManager()
      );
   }

   private void updateSurveyLabel() {
      Survey survey = lsss.getConfigurationManager().getSurveyConf().getSurvey();
      if (survey != null) {
         surveyTextLabel.setText(survey.getPlatform().getNation().getNationName()
               + " / " + survey.getPlatform().findPlatformName(survey)
               + " / " + survey.getSurveyTitle() + " (" + survey.getCompId().getSurvey() + ")");
      } else {
         surveyTextLabel.setText("No survey selected");
      }
   }

   private void updateIsComputing() {
      boolean isComputing = lsss.getInterpretationSettings().getExecutorObservation().getCount() > 0;
      if (isComputing) {
         if (isComputingAfterDelayTester == null) {
            Object tester = new Object();
            isComputingAfterDelayTester = tester;
            Timer timer = new Timer(300, _ -> {
               if (tester.equals(isComputingAfterDelayTester)) {
                  setIsComputing(true);
               }
            });
            timer.setRepeats(false);
            timer.start();
         }
      } else {
         isComputingAfterDelayTester = null;
         setIsComputing(false);
      }
   }

   private void setIsComputing(boolean computing) {
      if (isComputing == computing) {
         return;
      }
      isComputing = computing;
      updateStatusText();
   }

   private void updateStatusText() {
      statusTextLabel.setText(getStatusText());
   }

   private String getStatusText() {
      if (isLoading) {
         return "Loading pings...";
      } else if (isComputing) {
         return "Computing...";
      } else if (hasData) {
         return "Done";
      } else {
         return "";
      }
   }

   private void updatePreviewLabel() {
      PreviewFeaturesConf previewFeaturesConf = lsss.getConfigurationManager().getAppMiscConf().getPreviewFeaturesConf();
      boolean show = previewFeaturesConf.showStatusBarIndicator.getBooleanValue()
            && previewFeaturesConf.getToggles().getParameters().stream().anyMatch(BooleanParameter::getBooleanValue);
      previewSeparator.setVisible(show);
      previewLabel.setVisible(show);
   }

   private void updateFrozenLabel() {
      boolean frozen = lsss.getInterpretationSettings().mouseover().isFrozen();
      frozenSeparator.setVisible(frozen);
      frozenLabel.setVisible(frozen);
   }

   private void updateOnTheFlyLabel() {
      DataFileSet dataFileSet = lsss.getInterpretationSettings().getDataFileSet();
      SegmentData segmentData = dataFileSet.isEmpty() ? null : dataFileSet.getDataFiles().getFirst().getSegmentData();
      if (segmentData instanceof DiscardZeroDepthSegmentData discardZeroDepthSegmentData) {
         segmentData = discardZeroDepthSegmentData.getSegmentData();
      }
      boolean show;
      if (segmentData instanceof ProcessingSegmentData processingSegmentData) {
         show = true;
         HtmlStringBuilder sb = new HtmlStringBuilder()
               .html("On the fly processing is active"
                     + "<br>Click to edit"
                     + "<br>Ctrl + Click to edit modules");
         List<ConcurrentPingModuleComputation> computations = processingSegmentData.getModuleComputations();
         for (int i = 0; i < computations.size(); i++) {
            if (i > 3 && i < computations.size() - 2) {
               sb.html("<br>...");
               i = computations.size() - 1;
            }
            sb.html("<br>").text(i + 1).html(". ").text(computations.get(i).getModule().getDisplayName());
         }
         onTheFlyLabel.setToolTipText(sb.toString());
      } else {
         show = false;
      }
      onTheFlySeparator.setVisible(show);
      onTheFlyLabel.setVisible(show);
   }

   private void updateModeLabel() {
      modeLabel.setText(lsss.getInterpretationSettings().getDataLoadingMode().toString());
   }

   private void updateProgressBar() {
      PingSampler pingSampler = lsss.getInterpretationSettings().getPingSampler();
      int available = pingSampler.getAvailablePings().size();
      int requested = pingSampler.getRequestedPingIndices().size();
      int total = lsss.getInterpretationSettings().getPingRange().getPingCount();

      setPingLoadProgress(available, requested, total);
   }

   private void setPingLoadProgress(int available, int requested, int total) {
      if (total == 0) {
         hasData = false;
         isLoading = false;
         updateProgressBar(0, 100, "");
         progressBar.setToolTipText(null);
      } else {
         hasData = true;
         isLoading = available < requested;
         updateProgressBar(available, requested, getPercentString(requested, total) + " %");
         progressBar.setToolTipText("<html><table cellpadding=0 cellspacing=0><tr><td>Loaded:</td><td align=right>" + available
               + "</td></tr><tr><td>Requested:</td><td align=right>" + requested
               + "</td></tr><tr><td>Total:</td><td align=right>&nbsp;" + total
               + "</td></tr></table>");
      }
      updateStatusText();
   }

   private static String getPercentString(int requested, int total) {
      if (requested == total) {
         return "100";
      } else {
         double percentRequested = 100 * (double) requested / (double) total;
         percentRequested = Math.min(percentRequested, 99.0); // avoid rounding up to 100 %
         return Utils.format("%.2g", percentRequested);
      }
   }

   private void updateProgressBar(int value, int maximum, String text) {
      int progressBarValue;
      if (value == maximum) {
         progressBarValue = progressBar.getMaximum();
      } else {
         progressBarValue = (int) Math.floor(progressBar.getMaximum() * (float) value / (float) maximum);
      }
      progressBar.setValue(progressBarValue);
      progressBar.setString(text);
   }

   private void updateDatabaseLabel() {
      DatabaseConnection databaseConnection = lsss.getDatabaseManager().getDatabaseConnection();
      JLabel label = databaseLabel;
      if (!databaseConnection.isConnected()) {
         label.setForeground(Color.LIGHT_GRAY);
         label.setText("DB: none");
         label.setToolTipText("Database: no connection");
      } else if (databaseConnection.isBusy()) {
         label.setForeground(Color.GREEN);
         label.setText("DB: busy");
         label.setToolTipText("Database: busy");
         label.repaint(); // Necessary for updating waiting count indicator
      } else {
         label.setForeground(Color.BLACK);
         label.setText("DB: idle");
         label.setToolTipText("Database: idle");
      }
   }

   private void updateVisibility() {
      getComponent().setVisible(lsss.getActions().showStatusBar.get());
   }

   private final class DatabaseLabel extends JLabel {
      private DatabaseLabel() {
      }

      @Override
      protected void paintComponent(Graphics g) {
         drawWaitingCount(g);
         super.paintComponent(g);
      }

      private void drawWaitingCount(Graphics g) {
         int waitingCount = lsss.getDatabaseManager().getDatabaseConnection().getWaitingCount();
         if (waitingCount == 0) {
            return;
         }
         int x = getWidth() * waitingCount / DatabaseConnection.MAX_WAITING_COUNT;
         int y = getHeight() - 1;
         g.drawLine(0, y, x, y);
      }
   }
}
