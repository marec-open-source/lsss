package no.imr.lsss.modules.trawl;

enum FishSex {
   UNSPECIFIED(' ', "Unspecified"),
   C1('♀', "♀"), // female
   C2('♂', "♂"), // male
   C3('⚥', "⚥"), // intersex
   C4('♀', "♀(IV)"), // female
   C5('♀', "♀(V)"),
   C6('♀', "♀(VI)"),
   C7('♀', "♀(VII)"),
   C8('♀', "♀(VIII)");

   final char symbol;
   final String string;

   FishSex(char symbol, String string) {
      this.symbol = symbol;
      this.string = string;
   }

   static FishSex of(char sex) {
      return switch (sex) {
         case '1' -> C1; // female
         case '2' -> C2; // male
         case '3' -> C3; // intersex
         case '4' -> C4;
         case '5' -> C5;
         case '6' -> C6;
         case '7' -> C7;
         case '8' -> C8;
         default -> UNSPECIFIED;
      };
   }
}
