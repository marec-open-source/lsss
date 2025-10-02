package no.imr.lsss.database.tables;

public enum SchoolObjectTypeEnum {
   SchoolDetectedUncorrected(10),
   SchoolDetectedCorrected(11),
   SchoolKernelUnCorrected(40),
   SchoolKernelCorrected(41),
   EchogramSlice(100);

   private final short value;

   SchoolObjectTypeEnum(int value) {
      this.value = (short) value;
   }

   public short getValue() {
      return value;
   }
}
