package no.imr.lsss.database.reports;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Nation;
import no.imr.lsss.database.tables.hibernate.Platform;

final class GetPgnapes {
   private GetPgnapes() {
   }

   /*
    * Getting 2-letter nation string used by PGNAPES. For nations with unknown 2-letter string, the 2 first letters in
    * the nation-name are used.
    */
   static String nation(Nation nation) {
      return switch (nation.getNation()) {
         case 234 -> "FO";
         case 578 -> "NO";
         case 528 -> "NL";
         case 372 -> "IR";
         case 352 -> "IS";
         case 643 -> "RU";
         default -> "??"; // nation.getNationName().substring(0,2);
      };
   }

   /*
    * Getting 4-letter callsign of ship. Just dummy now. Need content in PlatformCode database table.
    */
   static String callsign(Platform platform) {
      return switch (platform.getCompId().getNation()) {
         case 234 -> // Faroe islands
               switch (platform.getCompId().getPlatform()) {
                  case 1001 -> "XPXP"; // Finnur Friði
                  case 1002 -> "XPXM"; // Tróndur ì gøtu
                  case 1003 -> "XPYG"; // Norðborg
                  case 1004 -> "OW2252"; // Magnus Heinason
                  case 1005 -> "XPZO"; // Jákup Sverri
                  default -> "????";
               };
         case 578 -> // Norway
               switch (platform.getCompId().getPlatform()) {
                  case 1002 -> "LJIT"; // Haakon Mosby
                  case 1019 -> "LDGJ"; // Johan Hjort
                  case 1024 -> "LLZG"; // G.O. Sars (old)
                  case 4174 -> "LMEL"; // G.O. Sars
                  case 4405 -> "LIWG"; // Brennholm
                  case 3578 -> "LIVA"; // Eros
                  case 3317 -> "LCNG"; // Eros
                  case 2603 -> "LMOG"; // Gardar
                  case 3303 -> "LMQI"; // Libas
                  case 1017 -> "LHUW"; // Michael Sars
                  case 1847 -> "LJBD"; // Nybo
                  default -> "????";
               };
         case 528 -> // Netherlands
               switch (platform.getCompId().getPlatform()) {
                  case 7156 -> "PBVO"; // Tridens
                  default -> "????";
               };
         case 372 -> // Ireland
               switch (platform.getCompId().getPlatform()) {
                  //case ???? -> "EIGB"; // Celtic Explorer
                  default -> "????";
               };
         case 352 -> // Iceland
               switch (platform.getCompId().getPlatform()) {
                  case 1001 -> "TFEA"; // Bjarni Saemundsson
                  case 1002 -> "TFNA"; // Arni Fridriksson
                  case 1003 -> "TFBS"; // Thorunn Thordardottir
                  //case ???? -> "TJFA"; // Arni Fridrikson (old)
                  default -> "????";
               };
         case 643 -> // Russia
               switch (platform.getCompId().getPlatform()) {
                  case 5003 -> "UALU"; // Atlantida
                  case 5004 -> "UANA"; // Fridtjof Nansen
                  case 5005 -> "UANB"; // Professor Marty
                  case 5006 -> "????"; // Pinro
                  //case ???? -> "UHOB"; // Atlantniro
                  default -> "????";
               };
         case 752 -> // Sweden
               switch (platform.getCompId().getPlatform()) {
                  //case ???? -> return "SEPI"; // Argos
                  default -> "????";
               };
         default -> "????";
      };
   }

   /*
    * Getting 3-letter species string used by PGNAPES. Only some species have known 3-letter string, so the rest get the
    * 3 first letter of the english initials.
    */
   static String species(AcousticCategory acousticCategory) {
      return switch (acousticCategory.getCompId().getAcousticCategory()) {
         case 4 -> "CNI"; // Jellyfish / maneter
         case 6 -> "PLK"; // Plankton / plankton
         case 7 -> "MYX"; // Lantern fishes / mesopelagisk fisk
         case 8 -> "POC"; // Polar cod / polartorsk
         case 12 -> "HER"; // Herring / sild
         case 16 -> "CAP"; // Capelin / lodde
         case 18 -> "WHG"; // Whiting / hvitting
         case 21 -> "MAC"; // Mackerel / makrell
         case 22 -> "POK"; // Saithe / sei
         case 23 -> "ARU"; // Greater argentine / vassild
         case 24 -> "WHB"; // Blue whiting / kolmule
         case 25 -> "USK"; // Brosm / brosme
         case 28 -> "NOP"; // Norway pout / øyepål
         case 29 -> "RED"; // Redfish / uer
         case 30 -> "HAD"; // Haddock / hyse
         case 31 -> "COD"; // Cod / torsk
         case 39 -> "PLS"; // Pearlside / laksesild
         case 77 -> "KRZ"; // Krill / krill
         case 5003 -> "HOM"; // Taggmakrell / taggmakrell (hestmakrell)
         case 5010 -> "HKE"; // European hake / lysing
         case 5029 -> "REB"; // Deepwater redfish / snabeluer
         //default:   return acousticCategory.getEnglishInitials().substring(0,3);
         default -> "???";
      };
   }
}
