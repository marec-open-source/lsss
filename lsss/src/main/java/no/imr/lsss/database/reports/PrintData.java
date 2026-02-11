package no.imr.lsss.database.reports;

import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.tables.ScatterTypeEnum;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.AcousticCategoryPK;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.ObservationComment;
import no.imr.lsss.database.tables.hibernate.ObservationPK;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Purpose;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterData;
import no.imr.lsss.database.tables.hibernate.ScatterDataPK;
import no.imr.lsss.database.tables.hibernate.ScatterPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.Utils;
import org.jspecify.annotations.Nullable;

import java.util.List;

abstract class PrintData {
   private final ReportEngine mReportEngine;
   private final ReportEngine.Feedback mFeedback;
   private final short mScatterType;
   private final Nation nation;
   private final Platform platform;
   private final Survey survey;
   private String mCallsign = "?";
   private String mPlatformIocCode = "?";
   private int lastCountedDate = -1;
   private int lastCountedTime = -1;
   private List<Integer> mFrequencyList = List.of();
   private @Nullable IcesAcousticMetadata icesAcousticMetadata;
   private float mOneDistanceInterval;
   static final float MAX_DISTANCE = 10000; //Maximum distance used by log before reset to 0 (zero)

   PrintData(ReportEngine aReportEngine, ReportEngine.Feedback aFeedback, short aScatterType, Survey survey) {
      mReportEngine = aReportEngine;
      mFeedback = aFeedback;
      mScatterType = aScatterType;

      this.survey = survey;
      platform = survey.getPlatform();
      nation = platform.getNation();
   }

   void setFrequencyList(List<Integer> aFrequencyList) {
      mFrequencyList = aFrequencyList;
   }

   List<Integer> getFrequencyList() {
      return mFrequencyList;
   }

   int getFrequency(int aI) {
      return mFrequencyList.get(aI);
   }

   int getFrequencyIndex(int aFrequency) {
      int frequencyIndex = -1;
      for (int i = 0; i < mFrequencyList.size(); i++) {
         if (mFrequencyList.get(i).equals(aFrequency)) {
            frequencyIndex = i;
         }
      }
      return frequencyIndex;
   }

   // Extinction coefficient for specified acoustic category, e.g. "herring". So far only hard-coded (2015.01.25)
   private double getExtinctionCoefficient(AcousticCategory aAcousticCategory) {
      double extinctionCoefficient = 1;    // Default value

      if (!mReportEngine.getExtinctionCheck()) {
         return 1;
      } else {
         if (aAcousticCategory.getCompId().getNation() == 578 &&          // Norway
               aAcousticCategory.getCompId().getAcousticCategory() == 12) { // Herring
            extinctionCoefficient = 2.41;      // todo, should not be hard-coded
         }
      }

      return extinctionCoefficient;
   }

   // Data from Observation DB table
   private @Nullable Observation mObservation;
   private @Nullable Observation mObservationAccumulate;
   private @Nullable Observation mObservationStop;
   private @Nullable Observation mObservationAccumulateStop;

   // Count of schools
   private int mSchoolCount;
   private int mSchoolCountAccumulate;

   void addSchoolCountNative() {
      mSchoolCount++;
   }

   int getSchoolCount(ReportMode aMode) {
      if (aMode == ReportMode.NATIVE) {
         return mSchoolCount;
      } else {
         return mSchoolCountAccumulate;  // aMode == ReportMode.ACCUMULATE
      }
   }

   // Comment
   private static final int MAX_COMMENT = 200;
   private int mCommentCount = 0;
   private int mCommentCountAccumulate = 0;
   private int mLargestCommentCount = 0;
   private int mLargestCommentCountAccumulate = 0;
   private ObservationComment[] mObservationComment = new ObservationComment[MAX_COMMENT];
   private ObservationComment[] mObservationCommentAccumulate = new ObservationComment[MAX_COMMENT];

   void setCommentNative(ObservationComment aObservationComment) {
      mObservationComment[mCommentCount] = aObservationComment;
      mCommentCount++;
      if (mCommentCount > mLargestCommentCount) {
         mLargestCommentCount = mCommentCount;
      }
   }

   private void addComments(ObservationComment[] aObservationComment, int aCommentCount) {
      for (int i = 0; i < aCommentCount; i++) {
         ObservationComment c = aObservationComment[i];
         mObservationCommentAccumulate[mCommentCountAccumulate] = new ObservationComment(
               c.getCompId(),
               c.getStandardComment(),
               c.getMantissa(),
               c.getExp(),
               c.getText()
         );
         mCommentCountAccumulate++;
      }
   }

   ObservationComment[] getComment(ReportMode aMode) {
      if (aMode == ReportMode.NATIVE) {
         return mObservationComment;
      } else {
         return mObservationCommentAccumulate;
      }
   }

   ObservationComment getComment(ReportMode aMode, int i) {
      if (aMode == ReportMode.NATIVE) {
         return mObservationComment[i];
      } else {
         return mObservationCommentAccumulate[i];
      }
   }

   int getCommentCount(ReportMode aMode) {
      if (aMode == ReportMode.NATIVE) {
         return mCommentCount;
      } else {
         return mCommentCountAccumulate;
      }
   }

   private void clearComment(ReportMode aMode) {
      if (aMode == ReportMode.NATIVE) {
         for (int i = 0; i < mLargestCommentCount; i++) {
            mObservationComment[i].setText("");
            mObservationComment[i].setMantissa(0);
            mObservationComment[i].setExp(0);
         }
         mCommentCount = 0;
      } else if (aMode == ReportMode.ACCUMULATE) {
         for (int i = 0; i < mLargestCommentCountAccumulate; i++) {
            mObservationCommentAccumulate[i].setText("");
            mObservationCommentAccumulate[i].setMantissa(0);
            mObservationCommentAccumulate[i].setExp(0);
         }
         mCommentCountAccumulate = 0;
      }
   }

   // Species purpose
   private Purpose[] mPurposePrint = new Purpose[MAX_PRINT_SPECIES];

   short getPurposePrint(int aAcousticCategory) {
      for (int i = 0; i <= mPrintCount; i++) {
         if (mPurposePrint[i].getCompId().getAcousticCategory() == aAcousticCategory) {
            return mPurposePrint[i].getPurpose();
         }
      }
      return -1;  //Unknown
   }

   void addPurposePrint(Purpose aPurpose) {
      mPurposePrint[mPrintCount - 1] = aPurpose;
   }

   void addUnknownPurposePrint(Survey aSurvey, AcousticCategory aAcousticCategory) {
      Purpose purpose = new Purpose(aSurvey, aAcousticCategory, (short) 0); // Unknown purpose.
      mPurposePrint[mPrintCount - 1] = purpose;
   } //addUnknownPurposePrint()

   // Species list
   private int mPrintCount = 0;
   private AcousticCategory[] mAcousticCategoryPrint = new AcousticCategory[MAX_PRINT_SPECIES];

   void printCountReset() {
      mPrintCount = 0;
   }

   void setAcousticCategoryPrint(AcousticCategory aAcousticCategoryPrint) {
      mAcousticCategoryPrint[mPrintCount] = aAcousticCategoryPrint;
      mPrintCount++;
   }

   int getPrintCount() {
      return mPrintCount;
   }

   int getPrintOrder(short aNation, short aPlatform, int aAcousticCategory) {
      for (int i = 0; i < mPrintCount; i++) {
         AcousticCategoryPK pk = mAcousticCategoryPrint[i].getCompId();
         if (pk.getAcousticCategory() == aAcousticCategory &&
               pk.getNation() == aNation &&
               pk.getPlatform() == aPlatform) {
            return i;
         }
      } //for
      return -1;
   }

   AcousticCategory getAcousticCategory(int i) {
      return mAcousticCategoryPrint[i];
   }

   void setIcesAcousticMetadata(@Nullable IcesAcousticMetadata aIcesAcousticMetadata) {
      icesAcousticMetadata = aIcesAcousticMetadata;
   }

   IcesAcousticMetadata getIcesAcousticMetadata() {
      if (icesAcousticMetadata == null) {
         icesAcousticMetadata = new IcesAcousticMetadata();
      }
      return icesAcousticMetadata;
   }

   void setOneDistanceInterval(float aOneDistanceInterval) {
      mOneDistanceInterval = aOneDistanceInterval;
   }

   float getOneDistanceInterval() {
      return mOneDistanceInterval;
   }

   //--------------------------------------------------------------------------------------------

   // Data from Scatter DB table
   private @Nullable Scatter mScatter;
   private @Nullable Scatter mScatterAccumulate;

   // Flags that marks conflicting data at different segments
   private boolean mAccumulateConflict_Serious;     // Marks serious conflict
   private boolean mAccumulateConflict_NotSerious;        // Marks conflict that can usually be ignored

   // Fields that gives serious conflict for some prints
   private int mObjectAccumulate;
   private boolean mAccumulateConflict_Object;
   private float mChannelThicknessAccumulate;
   private boolean mAccumulateConflict_ChannelThickness;  // Conflict that can be ignored for some prints

   // Fields that is used during printing
   private int mMaxChannel = -1;
   private int mMaxChannelAccumulate = -1;

   // Data from ScatterData DB table
   static final int MAX_PRINT_SPECIES = 25;
   static final int MAX_DEPTH_CHANNEL = 2000;

   private float[][] mSa = new float[MAX_PRINT_SPECIES][MAX_DEPTH_CHANNEL + 1];
   private float[][] mSaAccumulate = new float[MAX_PRINT_SPECIES][MAX_DEPTH_CHANNEL + 1];
   private int mCountAccumulate = 0;

   private float mStopDistanceAccumulate;
   private float gDistanceIntervalAccumulate;

   float getStopDistanceAccumulate() {
      return mStopDistanceAccumulate;
   }

   void setStopDistanceAccumulate(float aLastDistance, float aDistanceInterval) {
      double stopDist = aLastDistance + aDistanceInterval + 0.05;
      if (aDistanceInterval == 2.0f ||
            aDistanceInterval == 2.5f ||
            aDistanceInterval == 5.0f ||
            aDistanceInterval == 10.0f ||
            aDistanceInterval == 25.0f) {
         double x = 1.0 / aDistanceInterval;
         mStopDistanceAccumulate = (float) Math.floor(stopDist * x) / (float) x;
      } else {
         mStopDistanceAccumulate = (float) Math.floor(stopDist);
      }
      if (mStopDistanceAccumulate < 0) {
         mStopDistanceAccumulate = 0;
      } else if (mStopDistanceAccumulate >= MAX_DISTANCE + aDistanceInterval) {
         mStopDistanceAccumulate = 0;
      }
   }

   short getPrintDataScatterType() {
      return mScatterType;
   }

   Nation getNation() {
      return nation;
   }

   Platform getPlatform() {
      return platform;
   }

   Survey getSurvey() {
      return survey;
   }

   void setPlatformIocCode(String aPlatformIocCode) {
      mPlatformIocCode = aPlatformIocCode;
   }

   String getPlatformIocCode() {
      return mPlatformIocCode;
   }

   String getCallsign() {
      return mCallsign;
   }

   void setCallsign(String aCallsign) {
      mCallsign = aCallsign;
   }

   void setObservation(Observation aObservation) {
      mObservation = aObservation;
   }

   void setObservationStop(Observation aObservation, ReportMode mode) {
      if (mode == ReportMode.ACCUMULATE) {
         mObservationAccumulateStop = aObservation;
      } else {
         mObservationStop = aObservation;
      }
   }

   void clearObservationStop(ReportMode mode) {
      if (mode == ReportMode.ACCUMULATE) {
         mObservationAccumulateStop = null;
      } else {
         mObservationStop = null;
      }
   }

   Observation getObservation(ReportMode mode) {
      if (mode == ReportMode.ACCUMULATE) {
         if (mObservationAccumulate == null) {
            throw new IllegalStateException("Observation accumulate is null");
         }
         return mObservationAccumulate;
      } else {
         if (mObservation == null) {
            throw new IllegalStateException("Observation is null");
         }
         return mObservation;
      }
   }

   @Nullable Observation getObservationStop(ReportMode mode) {
      if (mode == ReportMode.ACCUMULATE) {
         return mObservationAccumulateStop;
      } else {
         return mObservationStop;
      }
   }

   void setScatter(Scatter aScatter) {
      if (aScatter.getCompId().getScatterType() == mScatterType) {
         mScatter = aScatter;
         initializeMaxChannel(aScatter);
      }
   }

   @Nullable Scatter getScatterInternal(ReportMode mode) {
      if (mode == ReportMode.ACCUMULATE) {
         return mScatterAccumulate;
      } else {
         return mScatter;
      }
   }

   private void initializeMaxChannel(Scatter aScatter) {
      mMaxChannel = 1; // Initial value

      if (aScatter.getCompId().getScatterType() == ScatterTypeEnum.PELAGIC.getValue() ||
            aScatter.getCompId().getScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue()) {
         float lower = aScatter.getMaxBottomDepth();

         // If bottom detection is wrong and there is data below detected bottom
         if (aScatter.getLowerInterpretationDepth() < lower) lower = aScatter.getLowerInterpretationDepth();
         if (aScatter.getLowerDepth() < lower) lower = aScatter.getLowerDepth();

         // Pelagic channel 1 always starts at surface and increase downwards (0 is sum-channel)
         mMaxChannel = (int) Math.ceil(lower / aScatter.getChannelThickness());
         mFeedback.registerMaxChannel(mMaxChannel);
         if (mMaxChannel > MAX_DEPTH_CHANNEL) mMaxChannel = MAX_DEPTH_CHANNEL;
      } else if (aScatter.getCompId().getScatterType() == ScatterTypeEnum.BOTTOM.getValue()) { // Echogram bottom channels
         // Note that aScatter.getUpperDepth() is negative distance from bottom.
         // Bottom channels are referred to bottom and increasing upwards, i.e. detected bottom is needed.
         float bottomDepth = aScatter.getMaxBottomDepth();
         float upper = bottomDepth + aScatter.getUpperInterpretationDepth();
         if (upper < aScatter.getUpperDepth()) {
            upper = aScatter.getUpperDepth();
         }

         float bottomThickness = bottomDepth - upper;
         mMaxChannel = (int) Math.ceil(bottomThickness / aScatter.getChannelThickness());
         if (mMaxChannel < 0) mMaxChannel = 0;
         if (mMaxChannel > MAX_DEPTH_CHANNEL) mMaxChannel = MAX_DEPTH_CHANNEL;
      } else if (aScatter.getCompId().getScatterType() == ScatterTypeEnum.BOTTOM_SCHOOL.getValue()) { // School bottom channels
         // Note that aScatter.getUpperDepth() is negative distance from bottom.
         // Bottom channels are referred to bottom and increasing upwards, i.e. detected bottom is needed.
         float bottomDepth = aScatter.getMaxBottomDepth();
         float upper = bottomDepth + aScatter.getUpperInterpretationDepth();
         if (upper < aScatter.getUpperDepth()) {
            upper = aScatter.getUpperDepth();
         }

         float bottomThickness = bottomDepth - upper;
         mMaxChannel = (int) Math.ceil(bottomThickness / aScatter.getChannelThickness());
         if (mMaxChannel < 0) mMaxChannel = 0;
         if (mMaxChannel > MAX_DEPTH_CHANNEL) mMaxChannel = MAX_DEPTH_CHANNEL;
      }
   }

   private void initializeMaxChannelAccumulate() {
      mMaxChannelAccumulate = mMaxChannel;
   }

   int getMaxChannel(ReportMode mode) {
      if (mode == ReportMode.ACCUMULATE) {
         return mMaxChannelAccumulate;
      } else {
         return mMaxChannel;
      }
   }

   private void setMaxChannel(int channel) {
      mMaxChannel = Math.min(channel, MAX_DEPTH_CHANNEL);
   }

   // Get data from Observation table
   // Set observation data
   // Get data from scatter
   // Set scatter data

   /*
    * Functions for data of native resolutions.
    */
   void clearData(ReportMode aMode) {
      // Always reset NATIVE resolution
      if (aMode == ReportMode.NATIVE) {
         Utils.fill(mSa, 0);
         mSchoolCount = 0;
      } else if (aMode == ReportMode.ACCUMULATE) {
         Utils.fill(mSaAccumulate, 0);
         mSchoolCountAccumulate = 0;
      }

      clearComment(aMode);
   }

   // Read acoustic data, but possibly correct for extinction
   float[][] getSa(ReportMode mode) {
      // Read from database: (size of data array: dataPrint[MAX_PRINT_SPECIES][MAX_DEPTH_CHANNEL + 1])
      float[][] dataPrint = getSaRaw(mode);   //  The "original" getSa(): reads data from the database

      // Should data be corrected for extinction?
      if (mReportEngine.getExtinctionCheck()) {
         for (int i = 0; i < mPrintCount; i++) { // Check that  mPrintCount is set
            double ext = getExtinctionCoefficient(mAcousticCategoryPrint[i]);
            if (ext != 1) {
               for (int j = 0; j < mMaxChannel; j++) {
                  if (dataPrint[i][j] > 0.1) { // Do not bother to correct smaller values
                     double k = 2.0 / (1852.0 * 1852.0);
                     double a = k * ext * dataPrint[i][j];
                     double b = 1.0 / (1.0 - a);

                     double d = Math.log(b);
                     double c = d / a;                      // The " > 0.1" requirement guarantees that a > 0
                     dataPrint[i][j] = (float) c * dataPrint[i][j];
                  }
               }
            }
         }
      }
      return dataPrint;
   }

   // Get sA data as stored  in the database: the "original" getSa
   private float[][] getSaRaw(ReportMode mode) {
      if (mode == ReportMode.NATIVE) {
         return mSa;
      } else {
         return mSaAccumulate;
      }
   }

   float getSaIJ(ReportMode mode, int i, int j) {
      if (mode == ReportMode.NATIVE) {
         return mSa[i][j];
      } else {
         return mSaAccumulate[i][j];
      }
   }

   void setSa(ScatterData aScatterData) {
      ScatterDataPK scatterDataPK = aScatterData.getCompId();
      if (scatterDataPK.getScatterType() != mScatterType) {
         return;
      }
      int acousticCategory = scatterDataPK.getAcousticCategory();
      // Do nothing if unscrutinized raw data
      if (acousticCategory == 0) {
         return;
      }
      int i = getPrintOrder(scatterDataPK.getNation(), scatterDataPK.getPlatform(), acousticCategory);
      if (i < 0) {
         return;
      }
      int j = scatterDataPK.getChannelNumber();
      if (j > getMaxChannel(ReportMode.NATIVE)) {
         setMaxChannel(j);  //In case mMaxChannel is too small  //todo Check
      }
      if (j < 0 || j > MAX_DEPTH_CHANNEL) {
         return;
      }
      mSa[i][j] = aScatterData.getSa();
      mSa[mPrintCount][j] += aScatterData.getSa();    // Total sum, all species
   }

   /**
    * Requires that all data for  native resolution exist.
    */
   void addToAccumulate() {
      // =============== 1. - CHECK DATA: do nothing if no data or wrong ScatterType  ==================================
      if (mScatter == null || mScatterAccumulate == null || mObservation == null || mScatter.getCompId().getScatterType() != mScatterType) return;

      if (mScatter.getCompId().getFrequency() != mScatterAccumulate.getCompId().getFrequency()) return;
      if (mScatter.getCompId().getTransceiver() != mScatterAccumulate.getCompId().getTransceiver()) return;
      if (mObservation.getCompId().getObservationDate() != lastCountedDate ||
            mObservation.getCompId().getObservationTime() != lastCountedTime) {
         lastCountedDate = mObservation.getCompId().getObservationDate();
         lastCountedTime = mObservation.getCompId().getObservationTime();
         mCountAccumulate++;
      } else {
         return;  //Data already counted
      }
      // =============== 1. FINISHED (CHECK)===============

      // =============== 2. Update ObservationStop ====================================================================
      mObservationAccumulateStop = mObservationStop;
      // =============== 2. FINISHED (Update ObservationStop) =============

      // =============== 3. Update Scatter (only fields that needs updating) ===========================================
      mScatterAccumulate.setDuration(mScatterAccumulate.getDuration() + mScatter.getDuration());
      mScatterAccumulate.setDistanceInterval(mScatterAccumulate.getDistanceInterval() + mScatter.getDistanceInterval());
      mScatterAccumulate.setMinBottomDepth(Math.min(mScatterAccumulate.getMinBottomDepth(), mScatter.getMinBottomDepth()));
      mScatterAccumulate.setMaxBottomDepth(Math.max(mScatterAccumulate.getMaxBottomDepth(), mScatter.getMaxBottomDepth()));
      mScatterAccumulate.setUpperDepth(Math.min(mScatterAccumulate.getUpperDepth(), mScatter.getUpperDepth()));
      mScatterAccumulate.setLowerDepth(Math.max(mScatterAccumulate.getLowerDepth(), mScatter.getLowerDepth()));
      mScatterAccumulate.setUpperInterpretationDepth(Math.min(mScatterAccumulate.getUpperInterpretationDepth(), mScatter.getUpperInterpretationDepth()));
      mScatterAccumulate.setLowerInterpretationDepth(Math.max(mScatterAccumulate.getLowerInterpretationDepth(), mScatter.getLowerInterpretationDepth()));
      mScatterAccumulate.setSa(mScatterAccumulate.getSa() + mScatter.getSa());      //Further processing needed
      mScatterAccumulate.setPctSa(mScatterAccumulate.getPctSa() + mScatter.getPctSa());   //Further processing needed
      // =============== 3. FINISHED (Update Scatter fields) ===============

      if (mScatterAccumulate.getChannelThickness() != mScatter.getChannelThickness()) {
         mAccumulateConflict_ChannelThickness = true;
      }

      if (mScatterAccumulate.getCompId().getObject() != mScatter.getCompId().getObject()) {
         mAccumulateConflict_Object = true;
      }

      // =============== 4. Update cell-data (ScatterData) =============================================================
      for (int i = 0; i < MAX_PRINT_SPECIES; i++) {
         for (int j = 0; j < MAX_DEPTH_CHANNEL; j++) {
            mSaAccumulate[i][j] += mSa[i][j];
         }
      }
      // =============== 4. FINISHED (Update cell-data)

      // =============== 5. Add new comments ===========================================================================
      addComments(mObservationComment, mCommentCount);
      // =============== 5. FINISHED (Add new comments)

      // =============== 6. Other ===========================================================================
      mSchoolCountAccumulate += mSchoolCount;
      // =============== 6. FINISHED (other)

      // =============== 7. Update conflict flags that may  lead to seriously conflicting data =========================
      if (mScatterAccumulate.getCompId().getFrequency() != mScatter.getCompId().getFrequency() ||
            mScatterAccumulate.getCompId().getTransceiver() != mScatter.getCompId().getTransceiver()) {
         mAccumulateConflict_Serious = true;
      }

      // Fields in mScatterAccumulate that cannot be correctly updated if they change
      if (mScatterAccumulate.getCompId().getScatterType() != mScatter.getCompId().getScatterType() ||
            mScatterAccumulate.getThreshold() != mScatter.getThreshold() ||
            mScatterAccumulate.getBubbleCorrection() != mScatter.getBubbleCorrection() ||
            mScatterAccumulate.getQuality() != mScatter.getQuality() ||
            mScatterAccumulate.getBottomActive() != mScatter.getBottomActive()) {
         mAccumulateConflict_NotSerious = true;
      }
      // =============== 7. FINISHED (Update conflict flags)
   } //addToAccumulate()

   void finalizeAccumulate() {
      if (mCountAccumulate > 0 && mScatterAccumulate != null) {
         mScatterAccumulate.setSa(mScatterAccumulate.getSa() / mCountAccumulate);
         mScatterAccumulate.setPctSa(mScatterAccumulate.getPctSa() / mCountAccumulate);

         for (int i = 0; i < MAX_PRINT_SPECIES; i++) {
            for (int j = 0; j < MAX_DEPTH_CHANNEL; j++) {
               mSaAccumulate[i][j] /= mCountAccumulate;
            }
         }
      }
   } //finalizeAccumulate

   /**
    * First entry of native data. Requires that all data for  native resolution exist
    *
    * @param aDistanceInterval - Distance until print
    */
   void initializeDataAccumulate(float aDistanceInterval) {
      if (mScatter == null || mObservation == null) {
         mCountAccumulate = 0;
      } else if (mScatter.getCompId().getScatterType() == mScatterType) {
         //int i, j;
         mCountAccumulate = 0;  //Only initialize key-data here (does not actually use any cell-data)

         // Setting key data: nation, platform, survey
         ScatterPK scatterPK = new ScatterPK(
               mScatter.getCompId().getNation(),
               mScatter.getCompId().getPlatform(),
               mScatter.getCompId().getSurvey(),
               mScatter.getCompId().getObject(),
               mScatter.getCompId().getObservationDate(),
               mScatter.getCompId().getObservationTime(),
               mScatter.getCompId().getFrequency(),
               mScatter.getCompId().getTransceiver(),
               mScatter.getCompId().getScatterType());
         mScatterAccumulate = new Scatter(scatterPK);
         mScatterAccumulate.setThreshold(mScatter.getThreshold());
         mScatterAccumulate.setBubbleCorrection(mScatter.getBubbleCorrection());
         mScatterAccumulate.setQuality(mScatter.getQuality());
         mScatterAccumulate.setBottomActive(mScatter.getBottomActive());

         // Updated later: just initialize here
         mScatterAccumulate.setObservationType(mScatter.getObservationType());
         mScatterAccumulate.setDuration(0);                                    //Duration at init (=zero)
         mScatterAccumulate.setDistanceInterval(0);                            //Distance interval at init (=zero)
         gDistanceIntervalAccumulate = aDistanceInterval;                           //Desired distance interval
         mScatterAccumulate.setMinBottomDepth(mScatter.getMinBottomDepth());
         mScatterAccumulate.setMaxBottomDepth(mScatter.getMaxBottomDepth());
         mScatterAccumulate.setChannelThickness(mScatter.getChannelThickness());
         mScatterAccumulate.setUpperDepth(mScatter.getUpperDepth());
         mScatterAccumulate.setLowerDepth(mScatter.getLowerDepth());
         mScatterAccumulate.setUpperInterpretationDepth(mScatter.getUpperInterpretationDepth());
         mScatterAccumulate.setLowerInterpretationDepth(mScatter.getLowerInterpretationDepth());

         // Updated later: just initialize here
         mScatterAccumulate.setSa(mScatter.getSa());
         mScatterAccumulate.setPctSa(mScatter.getPctSa());

         // Setting key data: nation, platform, survey
         ObservationPK observationPK = new ObservationPK(
               mObservation.getCompId().getNation(),
               mObservation.getCompId().getPlatform(),
               mObservation.getCompId().getSurvey(),
               mObservation.getCompId().getObservationDate(),
               mObservation.getCompId().getObservationTime(),
               mObservation.getCompId().getObservationType());
         mObservationAccumulate = new Observation(observationPK);
         mObservationAccumulate.setDistance(mObservation.getDistance());
         mObservationAccumulate.setLatitude(mObservation.getLatitude());
         mObservationAccumulate.setLongitude(mObservation.getLongitude());
         mObservationAccumulate.setBottomDepth(mObservation.getBottomDepth());

         lastCountedDate = -1;
         lastCountedTime = -1;

         setStopDistanceAccumulate(mObservationAccumulate.getDistance(), aDistanceInterval);

         mAccumulateConflict_Serious = false;
         mAccumulateConflict_NotSerious = false;
         mAccumulateConflict_ChannelThickness = false;
         mAccumulateConflict_Object = false;

         initializeMaxChannelAccumulate();

         //Reset ScatterData-Accumulate and comments
         clearData(ReportMode.ACCUMULATE);
      } //if_ScatterType
   } //initializeDataAccumulate()

   boolean getSeriousConflict() {
      return mAccumulateConflict_Serious;
   }

   boolean getWeakConflict() {
      return mAccumulateConflict_NotSerious;
   }

   boolean getObjectConflict() {
      return mAccumulateConflict_Object;
   }

   boolean getChannelThicknessConflict() {
      return mAccumulateConflict_ChannelThickness;
   }

   /*
    * Return true if stop-distance is reached (or passed).
    */
   boolean accumulateDistanceReached() {
      if (mObservation == null || mScatter == null) {
         return false;
      }
      // Round to first decimal:
      double compareDistance = Math.floor(10.0 * (mObservation.getDistance() + mScatter.getDistanceInterval() + 0.05)) / 10.0;
      return compareDistance >= mStopDistanceAccumulate || mStopDistanceAccumulate - compareDistance > 1000;
   }

   /*
    * Return true if accumulate-distance is full (or more than full).
    */
   boolean accumulateDistanceIntervalFull() {
      if (mObservationAccumulate == null) {
         return false;
      }
      // Round to first decimal:
      double compareDistance = Math.floor(10.0 * (mStopDistanceAccumulate - mObservationAccumulate.getDistance() + 0.05)) / 10.0;
      return compareDistance >= gDistanceIntervalAccumulate;
   }

   /*
    * Return true if accumulate-distance is full (or more than full).
    */
   int getCountAccumulate() {
      return mCountAccumulate;
   }

   static final class Pelagic extends PrintData {
      Pelagic(ReportEngine aReportEngine, ReportEngine.Feedback aFeedback, short aScatterType, Survey survey) {
         super(aReportEngine, aFeedback, aScatterType, survey);
      }

      Scatter getScatter(ReportMode mode) {
         Scatter scatter = getScatterInternal(mode);
         if (scatter == null) {
            throw new IllegalStateException("Scatter is null");
         }
         return scatter;
      }
   }

   static final class Bottom extends PrintData {
      Bottom(ReportEngine aReportEngine, ReportEngine.Feedback aFeedback, short aScatterType, Survey survey) {
         super(aReportEngine, aFeedback, aScatterType, survey);
      }

      @Nullable Scatter getScatter(ReportMode mode) {
         return getScatterInternal(mode);
      }
   }
}  // class PrintData()
