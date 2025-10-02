package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser5 extends BaseOneSpeciesPerFileReport {
   PrintUser5(ReportEngine reportEngine) {
      super(5, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
   }

   /***********************************************************************************************
    * HISTORY:     The program is requested by O. R. Godoe (IMR) to be used by the
    *              SAS statistics package.
    *
    * @param aPrintWriter             print file
    * @param aPrintData               pelagic data
    * @param aPrintDataBottom         bottom data
    * @param aAcousticCategoryIndex   index in species-array
    *
    * DATA: 1) Survey(10d) Nation(10d) Ship(10d) Date(10d ) Time(10d )
    *          Startlog(10.3f ) Stoplog(10.3f )
    *          Min_bottomdepth(10.3f ) Max_bottomdepth(10.3f ) Frequency(10d )
    *          Transceiver(10d ) Threshold(10.3f ) Species_code(10d ) (NEWLINE)
    *       2) Pelagic_channel_thickness(10.3f )
    *          Number_pelagic_channels_with_values(10d ) Sum_channel(10.3f )
    *          12*Pelagic_channels(10.3f ) (NEWLINE)
    *       3) Bottom_channel_thickness(10.3f )
    *          Number_bottom_channels_with_values(10d ) Sum_channel(10.3f )
    *          5*Bottom_channels(10.3f ) (NEWLINE)
    */
   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         PrintData.Bottom aPrintDataBottom,
         ReportMode aMode,
         int aAcousticCategoryIndex                 // Index in AcousticCategory array in PrintData
   ) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.000", dfs);

      float[][] dataPrint = aPrintData.getSa(aMode);
      float[][] bottomDataPrint = aPrintDataBottom.getSa(aMode);

      // Line 1: general information
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getSurvey());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getNation());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getPlatform());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getObservationDate());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getObservationTime());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getDistance()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getDistance() + aPrintData.getScatter(aMode).getDistanceInterval()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getLatitude()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getLongitude()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getMinBottomDepth()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getMaxBottomDepth()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getFrequency());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getTransceiver());
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getThreshold()));
      Print.spaceAndLeftPaddedValue(aPrintWriter, 10, aPrintData.getAcousticCategory(aAcousticCategoryIndex).getCompId().getAcousticCategory());
      aPrintWriter.print(' ');
      aPrintWriter.println();

      // Line 2:
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getChannelThickness()));

      /* OLD: to be removed
       * float lower = aPrintData.getScatter().getMaxBottomDepth();
       * if ( aPrintData.getScatter().getLowerDepth() < lower ) {
       *    lower = aPrintData.getScatter().getLowerDepth();
       * }
       * if ( aPrintData.getScatter().getLowerInterpretationDepth() < lower ) {
       *    lower = aPrintData.getScatter().getLowerInterpretationDepth();
       * }
       * int maxPelagicCh = (int) Math.ceil(lower/aPrintData.getScatter().getChannelThickness());
       */

      //New: aPrintData.getMaxChannel() used instead of maxPelagicCh. aPrintData contains pelagic data.
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getMaxChannel(aMode));

      for (int ch = 0; ch <= aPrintData.getMaxChannel(aMode); ch++) { // ch=0 is sum channel, ch>0 is real channels
         Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(dataPrint[aAcousticCategoryIndex][ch]));
      }
      aPrintWriter.println();

      // Line 3:

      int maxBottomCh = 0;
      Scatter bottomScatter = aPrintDataBottom.getScatter(aMode);
      if (bottomScatter != null) {
         float bottomDepth = bottomScatter.getMaxBottomDepth();
         float upper = bottomDepth + bottomScatter.getUpperInterpretationDepth();
         if (upper < bottomScatter.getUpperDepth()) {
            upper = bottomScatter.getUpperDepth();
         }
         float bottomThickness = bottomDepth - upper;
         maxBottomCh = (int) Math.ceil(bottomThickness / bottomScatter.getChannelThickness());
         Print.leftPaddedValue(aPrintWriter, 10, df.format(bottomScatter.getChannelThickness()));
      } else {
         Print.leftPaddedValue(aPrintWriter, 10, df.format(0));
      }
      Print.leftPaddedValue(aPrintWriter, 10, maxBottomCh);
      for (int ch = 0; ch <= maxBottomCh; ch++) { // ch=0 is sum channel, ch>0 is real channels
         Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(bottomDataPrint[aAcousticCategoryIndex][ch]));
      }
      aPrintWriter.println();
      //todo: Check for overflow - too high channel number. Check for maxBottomCh in BEI
   } //print()
}
