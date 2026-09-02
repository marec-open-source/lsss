package no.imr.lsss.framework.config.survey.survey;

import no.imr.korona.data.ping.PingRange;
import no.imr.korona.data.track.SegmentInfo;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.DatabaseData;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.framework.config.survey.misc.ices.IcesCode;
import no.imr.lsss.framework.config.survey.misc.ices.IcesUtils;
import no.imr.tools.Max;
import no.imr.tools.Min;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.database.queries.StatelessDatabaseQuery;
import no.imr.tools.geo.GeoBoxBuilder;
import no.imr.tools.listening.Listener;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.DateParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.SeparatorParameter;
import no.imr.tools.parameter.StringParameter;
import no.imr.tools.parameter.TextParameter;
import no.imr.tools.parameter.TimeParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.time.TimeUtils;
import no.marec.lsss.api.util.GeoPoint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.geom.Rectangle2D;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

/**
 * For editing a surveys.
 */
public final class SurveyEditor implements ParameterContainer {
   private final LSSS mLSSS;
   private final JDialog mDialog;
   private final NavigableSet<Integer> unavailableSurveyIds = new TreeSet<>();
   private final Survey mSurvey;
   private @Nullable Survey mStoredSurvey;
   private final ParameterEditor parameterEditor;

   private final ObjectParameter<String> mNation = new ObjectParameter<>(
         new Name("Nation"),
         "");

   private final ObjectParameter<String> mPlatformAndName = new ObjectParameter<>(
         new Name("Platform"),
         "");

   private final SeparatorParameter mSeparator = SeparatorParameter.line();
   private final IntParameter surveyId = new IntParameter(
         new Name("SurveyId", "Survey number"),
         1, Unit.NONE, ValueConstraints.gte(1).withExtraValidation(value -> {
      return unavailableSurveyIds.contains(value) ? "Survey number already in use" : null;
   }));

   private final StringParameter mSurveyTitle = new StringParameter(
         new Name("SurveyTitle", "Survey title (optional)"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_SURVEY_TITLE_LENGTH));

   private final DateParameter mStartDate = new DateParameter(
         new Name("StartDate", "Start date (UTC) (optional)"));
   private final TimeParameter mStartTime = new TimeParameter(
         new Name("StartTime", "Start time (UTC) (optional)"));
   private final DateParameter mStopDate = new DateParameter(
         new Name("StopDate", "Stop date (UTC) (optional)"));
   private final TimeParameter mStopTime = new TimeParameter(
         new Name("StopTime", "Stop time (UTC) (optional)"));

   private final OptionalFloatParameter mBoundaryNorth = new OptionalFloatParameter(
         new Name("BoundaryNorth", "Northern survey boundary (N+, S-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   private final OptionalFloatParameter mBoundarySouth = new OptionalFloatParameter(
         new Name("BoundarySouth", "Southern survey boundary (N+, S-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   private final OptionalFloatParameter mBoundaryWest = new OptionalFloatParameter(
         new Name("BoundaryWest", "Western survey boundary (E+, W-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   private final OptionalFloatParameter mBoundaryEast = new OptionalFloatParameter(
         new Name("BoundaryEast", "Eastern survey boundary (E+, W-) (optional)"),
         Optional.empty(), Unit.DEGREES);

   private final TextParameter mSurveyDescription = new TextParameter(
         new Name("Comment", "Short survey description (optional)"),
         "", ValueConstraints.maxLength(DatabaseData.MAX_COMMENT_LENGTH));

   SurveyEditor(LSSS aLSSS, Platform aPlatform, @Nullable Survey aSurvey) {
      mLSSS = aLSSS;

      List<String> icesSurveyCodes = mLSSS.getConfigurationManager().getSurveyMiscConf().getIcesConf().readIcesCodes(IcesUtils.SURVEY_SCHEMA).stream()
            .map(IcesCode::key)
            .toList();
      mSurveyTitle.setSuggestedValues(icesSurveyCodes);

      String title;
      if (aSurvey != null) {
         title = "Edit survey";
         mSurvey = aSurvey;
         surveyId.setEnabled(false);
      } else {
         title = "New survey";

         for (Survey survey : mLSSS.getConfigurationManager().getSurveyConf().getSurveys()) {
            unavailableSurveyIds.add(survey.getCompId().getSurvey());
         }

         DatabaseTime currentTime = new DatabaseTime(Instant.now());
         int newSurveyId = unavailableSurveyIds.isEmpty() ? 1 : unavailableSurveyIds.last() + 1;
         SurveyPK surveyPK = new SurveyPK(
               aPlatform.getCompId().getNation(),
               aPlatform.getCompId().getPlatform(),
               newSurveyId);
         mSurvey = new Survey(surveyPK,
               "",
               currentTime.getDate(), 0,
               currentTime.getDate(), 23_59_00_00,
               "",
               0, 0, 0, 0);
         mSurvey.setPlatform(aPlatform);
      }

      String nationName = aPlatform.getNation().getNationName();
      mNation.setEnabled(false);
      mNation.setAllowedValuesAndValue(List.of(nationName), nationName);
      mPlatformAndName.setEnabled(false);
      surveyId.setIntValue(mSurvey.getCompId().getSurvey());
      mSurveyTitle.setValue(mSurvey.getSurveyTitle());

      mStartDate.setIntValue(mSurvey.getStartDate());
      mStartTime.setIntValue(mSurvey.getStartTime());
      mStopDate.setIntValue(mSurvey.getStopDate());
      mStopTime.setIntValue(mSurvey.getStopTime());

      mBoundaryNorth.setFloatValue(mSurvey.getBoundaryNorth());
      mBoundarySouth.setFloatValue(mSurvey.getBoundarySouth());
      mBoundaryWest.setFloatValue(mSurvey.getBoundaryWest());
      mBoundaryEast.setFloatValue(mSurvey.getBoundaryEast());

      mSurveyDescription.setValue(mSurvey.getSurveyDescription());

      Listener.of(this::updatePlatformAndName).addToAndNotify(
            mStartDate
      );

      parameterEditor = new ParameterEditor(getParameters(), new GUIConfig()
            .setHorizontalFill(true)
      );

      Component referenceComponent = mLSSS.getReferenceComponent();
      mDialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.DOCUMENT_MODAL);
      mDialog.getContentPane().add(createMainPanel());
      mDialog.pack();
      GuiUtils.expandSizeWith(mDialog, 100, 0);
      mDialog.setLocationRelativeTo(referenceComponent);
      mDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      mDialog.setVisible(true);
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            mNation,
            mPlatformAndName,
            //---
            mSeparator,
            surveyId,
            mSurveyTitle,
            mStartDate,
            mStartTime,
            mStopDate,
            mStopTime,
            mBoundaryNorth,
            mBoundarySouth,
            mBoundaryWest,
            mBoundaryEast,
            mSurveyDescription
      );
   }

   private JComponent createMainPanel() {
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(GuiUtils.createScrollPane(parameterEditor.getEditorComponent()));
      mainPanel.add(createButtonsPanel(), BorderLayout.SOUTH);
      return mainPanel;
   }

   private JPanel createButtonsPanel() {
      JButton computeButton = new JButton("Compute bounds");
      computeButton.setToolTipText("Compute geographical bounding box and start / end times from configured data files");
      computeButton.addActionListener(_ -> computeBounds());

      JButton okButton = new JButton("OK");
      okButton.addActionListener(_ -> {
         if (!CurrentInputComponent.commitEdit()) {
            return;
         }
         store(mLSSS.getDatabaseManager().getDatabaseConnection());
         mDialog.dispose();
      });
      mDialog.getRootPane().setDefaultButton(okButton);

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> mDialog.dispose());

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(computeButton);
      panel.add(okButton);
      panel.add(cancelButton);
      return panel;
   }

   @Nullable Survey getStoredSurvey() {
      return mStoredSurvey;
   }

   private void store(DatabaseConnection aDatabaseConnection) {
      mSurvey.getCompId().setSurvey(surveyId.getIntValue());
      mSurvey.setSurveyTitle(mSurveyTitle.getValue());
      mSurvey.setStartDate(mStartDate.getIntValue());
      mSurvey.setStartTime(mStartTime.getIntValue());
      mSurvey.setStopDate(mStopDate.getIntValue());
      mSurvey.setStopTime(mStopTime.getIntValue());
      mSurvey.setSurveyDescription(mSurveyDescription.getValue());
      mSurvey.setBoundaryNorth(mBoundaryNorth.getValue().orElse(0f));
      mSurvey.setBoundarySouth(mBoundarySouth.getValue().orElse(0f));
      mSurvey.setBoundaryWest(mBoundaryWest.getValue().orElse(0f));
      mSurvey.setBoundaryEast(mBoundaryEast.getValue().orElse(0f));

      aDatabaseConnection.executeStatelessQuery(StatelessDatabaseQuery.upsert(mSurvey));

      mStoredSurvey = mSurvey;
   }

   private void updatePlatformAndName() {
      Platform platform = mSurvey.getPlatform();
      String name = platform.findPlatformName(mStartDate.getIntValue());
      String platformAndName = new SurveyConf.PlatformAndName(platform, name).toString();
      mPlatformAndName.setAllowedValuesAndValue(List.of(platformAndName), platformAndName);
   }

   private void computeBounds() {
      Instant minTime = Instant.MAX;
      Instant maxTime = Instant.MIN;
      GeoBoxBuilder geoBoxBuilder = new GeoBoxBuilder();
      for (DataConf dataConf : mLSSS.getConfigurationManager().getDataConf().getAllDataConfs()) {
         for (SegmentInfo segmentInfo : dataConf.getAllOriginalSegmentInfos()) {
            PingRange pingRange = segmentInfo.pingRange();
            if (pingRange.isEmpty()) {
               continue;
            }

            minTime = Min.of(minTime, pingRange.begin().getInstant());
            maxTime = Max.of(maxTime, pingRange.end().getInstant());

            GeoPoint beginGeoPos = pingRange.begin().getGeographicalPosition();
            if (beginGeoPos != null) {
               geoBoxBuilder.add(beginGeoPos);
            }
            GeoPoint endGeoPos = pingRange.end().getGeographicalPosition();
            if (endGeoPos != null) {
               geoBoxBuilder.add(endGeoPos);
            }
         }
      }

      StringBuilder message = new StringBuilder();
      if (minTime.isAfter(maxTime)) {
         message.append("No times found in data files.\n");
      } else {
         DateParameter.setFromInstant(minTime.truncatedTo(ChronoUnit.SECONDS), mStartDate, mStartTime);
         DateParameter.setFromInstant(TimeUtils.ceiledTo(maxTime, ChronoUnit.SECONDS), mStopDate, mStopTime);
      }

      Rectangle2D geoBox = geoBoxBuilder.build();
      if (geoBox == null) {
         message.append("No geographical positions found in data files.\n");
      } else {
         int delta = 10000;
         mBoundaryNorth.setFloatValue((float) (Math.ceil(geoBox.getMaxY() * delta) / delta));
         mBoundarySouth.setFloatValue((float) (Math.floor(geoBox.getMinY() * delta) / delta));
         mBoundaryEast.setFloatValue((float) (Math.ceil(geoBox.getMaxX() * delta) / delta));
         mBoundaryWest.setFloatValue((float) (Math.floor(geoBox.getMinX() * delta) / delta));
      }

      if (!message.isEmpty() && mLSSS.getInterpretationSettings().isInteractiveMode()) {
         JOptionPane.showMessageDialog(mLSSS.getReferenceComponent(), message.toString());
      }
   }
}
