package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.ScatterTypeEnum;

/**
 * Returns postfix for use in file-names.
 */
final class GetPostfix {
   private GetPostfix() {
   }

   static String getPostfix(PrintData aPrintData) {
      if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC.getValue()) {
         return "";
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.BOTTOM.getValue()) {
         return "_BOTTOM";
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.PELAGIC_SCHOOL.getValue()) {
         return "_SCHOOL";
      } else if (aPrintData.getPrintDataScatterType() == ScatterTypeEnum.BOTTOM_SCHOOL.getValue()) {
         return "_SCHOOL_BOTTOM";
      } else {
         return "";
      }
   }
}
