package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.tools.Utils;
import no.imr.tools.io.Print;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

final class PrintUser4 extends BaseOneSpeciesPerFileReport {
   PrintUser4(ReportEngine reportEngine) {
      super(4, reportEngine);
   }

   @Override
   void printHeader(PrintWriter aPrintWriter, PrintData.Pelagic aPrintData, int printType, ReportMode aMode) {
      int maxPrintPelagic = getReportEngine().getMaxPrintPelagicCh();  //Channel 0 is not counted  //OLD: ReportEngine.MAX_PRINT_PELAGIC;
      int maxPrintBottom = getReportEngine().getMaxPrintBottomCh();    //Channel 0 is not counted

      aPrintWriter.print(" StartDate");
      aPrintWriter.print(" StartTime");
      aPrintWriter.print(" START_LOG");
      aPrintWriter.print("  STOP_LOG");
      aPrintWriter.print("  LATITUDE");
      aPrintWriter.print(" LONGITUDE");
      aPrintWriter.print(" MIN_DEPTH");
      aPrintWriter.print(" MAX_DEPTH");
      aPrintWriter.print(" FREQUENCY");
      aPrintWriter.print("    TRANSC");
      aPrintWriter.print(" THRESHOLD");
      aPrintWriter.print("   SPECIES");
      aPrintWriter.print("    CH_PEL");
      aPrintWriter.print(" NO_CH_PEL");
      for (int i = 0; i <= maxPrintPelagic; i++) {
         aPrintWriter.printf(" Pel_CH_%03d", i);
      }
      aPrintWriter.print("    CH_BOT");
      aPrintWriter.print(" NO_CH_BOT");
      for (int i = 0; i <= maxPrintBottom; i++) {
         aPrintWriter.printf(" Bot_CH_%03d", i);
      }
      aPrintWriter.println(); //Newline
   }

   /**
    * Report format designed general use by the Bergen Echo Integrator (BEI) by Rolf Korneliussen (IMR) due
    * to request by Asgeir Aglen (IMR) to be used by the Excel.
    *
    * @param aPrintWriter           print file
    * @param aPrintData             pelagic data
    * @param aPrintDataBottom       bottom data
    * @param aAcousticCategoryIndex index in species-array
    *                               DATA: 1) Date(10d ) Time(10d ) Startlog(10.3f ) Stoplog(10.3f )
    *                               Min_bottomdepth(10.3f ) Max_bottomdepth(10.3f ) Frequency(10d )
    *                               Transceiver(10d ) Threshold(10.3f ) Species_code(10d )
    *                               PelagicChannelThickness(10.3f)
    *                               Number_pelagic_channels_with_values(10d ) Sum_channel(10.3f )
    *                               Number_pelagic_channels_with_values*Pelagic_channels(10.3f)
    *                               BottomChannelThickness(10.3f)
    *                               Number_bottom_channels_with_values(10d ) Sum_channel(10.3f )
    *                               Number_bottom_channels_with_values*Bottom_channels(10.3f) (NEWLINE)
    */
   @Override
   void print(
         PrintWriter aPrintWriter,
         PrintData.Pelagic aPrintData,
         PrintData.Bottom aPrintDataBottom,
         ReportMode aMode,
         int aAcousticCategoryIndex                 // Acoustic category
   ) {
      DecimalFormatSymbols dfs = Utils.createDecimalFormatSymbols();
      DecimalFormat df = new DecimalFormat("#0.000", dfs);

      float[][] dataPrint = aPrintData.getSa(aMode);
      float[][] bottomDataPrint = aPrintDataBottom.getSa(aMode);

      int ch;
      int maxBottomCh = 0;
      int maxPrintPelagic = getReportEngine().getMaxPrintPelagicCh();  //Channel 0 is not counted
      int maxPrintBottom = getReportEngine().getMaxPrintBottomCh();   //Channel 0 is not counted

      // General information
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getObservationDate());
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getObservationTime());
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getDistance()));
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getDistance() + aPrintData.getScatter(aMode).getDistanceInterval()));
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getLatitude()));
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getObservation(aMode).getLongitude()));
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getMinBottomDepth()));
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getMaxBottomDepth()));
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getFrequency());
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getScatter(aMode).getCompId().getTransceiver());
      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getThreshold()));
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getAcousticCategory(aAcousticCategoryIndex).getCompId().getAcousticCategory());

      Print.leftPaddedValue(aPrintWriter, 10, df.format(aPrintData.getScatter(aMode).getChannelThickness()));
      Print.leftPaddedValue(aPrintWriter, 10, aPrintData.getMaxChannel(aMode));

      for (ch = 0; ch <= maxPrintPelagic; ch++) {  // ch=0 is sum channel, ch>0 is real channels
         Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(dataPrint[aAcousticCategoryIndex][ch]));
      }

      //float bottomWindowThickness = Math.abs(bottomScatter.getLowerInterpretationDepth() - bottomScatter.getUpperInterpretationDepth());
      //maxBottomCh = (int) Math.ceil( bottomWindowThickness / bottomScatter.getChannelThickness() );
      Scatter bottomScatter = aPrintDataBottom.getScatter(aMode);
      if (bottomScatter != null) {
         float bottomDepth = bottomScatter.getMaxBottomDepth();
         float upper = bottomDepth + bottomScatter.getUpperInterpretationDepth();
         if (upper < bottomScatter.getUpperDepth()) {
            upper = bottomScatter.getUpperDepth();
         }
         float bottomThickness = bottomDepth - upper;
         Print.leftPaddedValue(aPrintWriter, 10, df.format(bottomScatter.getChannelThickness()));
         maxBottomCh = (int) Math.ceil(bottomThickness / bottomScatter.getChannelThickness());
      } else {
         Print.leftPaddedValue(aPrintWriter, 10, df.format(0));
      }
      Print.leftPaddedValue(aPrintWriter, 10, maxBottomCh);

      for (ch = 0; ch <= maxPrintBottom; ch++) {  // ch=0 is sum channel, ch>0 is real channels
         Print.spaceAndLeftPaddedValue(aPrintWriter, 10, df.format(bottomDataPrint[aAcousticCategoryIndex][ch]));
      }

      aPrintWriter.println();
   } // print()
}
