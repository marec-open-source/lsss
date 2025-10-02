package no.imr.lsss.database.tables;

import org.jspecify.annotations.Nullable;

public enum QualityEnum {
   HIGH((short) 1, "High"),
   INTERMEDIATE((short) 2, "Intermediate"),
   LOW((short) 3, "Low");

   public final short value;
   public final String label;

   QualityEnum(short value, String label) {
      this.value = value;
      this.label = label;
   }

   public static @Nullable QualityEnum valueToQualityEnum(short value) {
      for (QualityEnum qualityEnum : values()) {
         if (qualityEnum.value == value) {
            return qualityEnum;
         }
      }
      return null;
   }

   public static String valueToText(short value) {
      QualityEnum qualityEnum = valueToQualityEnum(value);
      return qualityEnum != null ? qualityEnum.label : Short.toString(value);
   }
}
