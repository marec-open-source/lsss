package no.imr.lsss.database.tables;

import org.jspecify.annotations.Nullable;

/**
 * Enumeration of all observation types.
 */
public enum ObservationTypeEnum {
   NAVIGATION_DATA_INPUT(1000),
   SCATTERED_FISH_DATA(3000),
   SCHOOL_OF_FISH_DATA(4000),
   OCEANOGRAPHY_DATA_INPUT(5000),
   BIOLOGY_DATA_INPUT(6000),
   METEOROLOGY_DATA_INPUT(7000),
   SCATTER_OBJECT_DUMMY(8000),
   SCATTER_OBJECT_SCHOOL(9000),
   SCATTER_OBJECT_PELAGIC(10000);

   private final short value;

   ObservationTypeEnum(int value) {
      this.value = (short) value;
   }

   public short getValue() {
      return value;
   }

   public static @Nullable ObservationTypeEnum valueToObservationTypeEnum(short value) {
      for (ObservationTypeEnum observationTypeEnum : values()) {
         if (observationTypeEnum.value == value) {
            return observationTypeEnum;
         }
      }
      return null;
   }
}
