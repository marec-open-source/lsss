package no.imr.lsss.database.tables;

public enum SchoolObjectTypeEnum {
   SCHOOL_DETECTED_UNCORRECTED(10),
   SCHOOL_DETECTED_CORRECTED(11),
   SCHOOL_KERNEL_UNCORRECTED(40),
   SCHOOL_KERNEL_CORRECTED(41),
   ECHOGRAM_SLICE(100);

   private final short value;

   SchoolObjectTypeEnum(int value) {
      this.value = (short) value;
   }

   public short getValue() {
      return value;
   }
}
