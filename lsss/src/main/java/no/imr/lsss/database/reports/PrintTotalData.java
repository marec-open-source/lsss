package no.imr.lsss.database.reports;

import no.imr.lsss.database.reports.hibernate.AcCat;
import no.imr.lsss.database.reports.hibernate.DistCount;
import no.imr.lsss.database.reports.hibernate.ReportTotalData;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.Utils;

/**
 * For printing of summed depth-dependant data.
 */
final class PrintTotalData {
   private final Nation nation;
   private final Platform platform;
   private final Survey survey;
   private int mFrequency;
   private short mTransceiver;
   private short mScatterType;
   private int mAcousticCategoryCount = 0;
   private int mAcCatCount = 0;
   private int mMaxCh = 0;
   private int mDistCount_Count = 0;

   private static final int MAX_PRINT_SPECIES = PrintData.MAX_PRINT_SPECIES;  //25
   private static final int MAX_DEPTH_CHANNEL = PrintData.MAX_DEPTH_CHANNEL; //2000;
   private double[][] mSumSa = new double[MAX_PRINT_SPECIES][MAX_DEPTH_CHANNEL + 1];
   private AcCat[] mAcCat = new AcCat[MAX_PRINT_SPECIES];
   private int[] mAcousticCategory = new int[MAX_PRINT_SPECIES];
   private DistCount[] mDistCount = new DistCount[1000];

   PrintTotalData(Survey survey) {
      this.survey = survey;
      platform = survey.getPlatform();
      nation = platform.getNation();
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

   void setFrequency(int aFrequency) {
      mFrequency = aFrequency;
   }

   int getFrequency() {
      return mFrequency;
   }

   void setTransceiver(short aTransceiver) {
      mTransceiver = aTransceiver;
   }

   short getTransceiver() {
      return mTransceiver;
   }

   short getScatterType() {
      return mScatterType;
   }

   static String getScatterTypeName(short scatterType) {
      return switch (scatterType) {
         case 1000 -> "_PELAGIC";
         case 2000 -> "_BOTTOM";
         default -> "";
      };
   }

   void setAcCat(AcCat aAcCat) {
      mAcCat[mAcCatCount] = aAcCat;
      mAcCatCount++;
   }

   AcCat getAcCat(int i) {
      return mAcCat[i];
   }

   int getAcCatCount() {
      return mAcCatCount;
   }

   void setDistCount(DistCount aDistCount) {
      mDistCount[mDistCount_Count] = aDistCount;
      mDistCount_Count++;
   }

   DistCount getDistCount(int i) {
      return mDistCount[i];
   }

   int getDistCount_Count() {
      return mDistCount_Count;
   }

   int getDistCount_Count(int aFrequency, short aTransceiver) {
      int aCount = 0;
      for (int i = 0; i < mDistCount_Count; i++) {
         if (mDistCount[i].frequency() == aFrequency && mDistCount[i].transceiver() == aTransceiver) {
            aCount++;
         }
      }
      return mDistCount_Count;
   }

   DistCount getDistCount(int aFrequency, short aTransceiver, int n) {
      int i;
      int hits = 0;
      for (i = 0; i < mDistCount_Count; i++) {
         if (mDistCount[i].frequency() == aFrequency && mDistCount[i].transceiver() == aTransceiver) {
            hits++;
            if (hits == n) {
               return mDistCount[i];
            }
         }
      }
      return mDistCount[0];
   }

   int getMaxCh() {
      return mMaxCh;
   }

   boolean setData(ReportTotalData reportTotalData) {
      if (mFrequency > 0 &&
            !(mFrequency == reportTotalData.frequency() &&
                  mTransceiver == reportTotalData.transceiver() &&
                  mScatterType == reportTotalData.scatterType())) {
         return false;  //Data not set: signal that data should be printed
      }

      mFrequency = reportTotalData.frequency();
      mTransceiver = reportTotalData.transceiver();
      mScatterType = reportTotalData.scatterType();

      int iCat = 0;
      boolean found = false;
      if (mAcousticCategoryCount <= 0) { // First acoustic category
         mAcousticCategoryCount = 1;
         found = true;
         mAcousticCategory[0] = reportTotalData.acousticCategory();
      } else { // Known acoustic category
         for (iCat = 0; iCat < mAcousticCategoryCount; iCat++) {
            if (mAcousticCategory[iCat] == reportTotalData.acousticCategory()) {
               found = true;
               break;
            }
         }
      }

      if (!found) { // New acoustic category
         mAcousticCategory[mAcousticCategoryCount] = reportTotalData.acousticCategory();
         mAcousticCategoryCount++;
      }

      int jCh = reportTotalData.channelNumber();
      mSumSa[iCat][jCh] = reportTotalData.sumSa();
      mFrequency = reportTotalData.frequency();
      mTransceiver = reportTotalData.transceiver();
      mScatterType = reportTotalData.scatterType();
      mMaxCh = Math.max(mMaxCh, jCh);
      return true; //Data set
   }

   void clearData() {
      Utils.fill(mSumSa, 0);
      mFrequency = -1;
      mTransceiver = -1;
      mScatterType = -1;
      mAcousticCategoryCount = 0;
      mMaxCh = 0;
   }

   double[][] getData() {
      return mSumSa;
   }

   double getData(int iCat, int jCh) {
      return mSumSa[iCat][jCh];
   }
}
