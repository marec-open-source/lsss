package no.imr.lsss.database.tables;

import org.jspecify.annotations.Nullable;

/**
 * Enumeration of all scatter types.
 */
public enum ScatterTypeEnum {
   PELAGIC(1000, true, false),
   BOTTOM(2000, false, false),
   PELAGIC_SCHOOL(3000, true, true),
   BOTTOM_SCHOOL(4000, false, true);

   private final short value;
   private final boolean pelagic;
   private final boolean school;

   ScatterTypeEnum(int value, boolean pelagic, boolean school) {
      this.value = (short) value;
      this.pelagic = pelagic;
      this.school = school;
   }

   public short getValue() {
      return value;
   }

   public boolean isPelagic() {
      return pelagic;
   }

   public boolean isSchool() {
      return school;
   }

   public ObservationTypeEnum getObservationTypeEnum() {
      return school ? ObservationTypeEnum.SCHOOL_OF_FISH_DATA : ObservationTypeEnum.SCATTERED_FISH_DATA;
   }

   public ObservationTypeEnum getScatterObjectObservationTypeEnum() {
      return school ? ObservationTypeEnum.SCATTER_OBJECT_SCHOOL : ObservationTypeEnum.SCATTER_OBJECT_PELAGIC;
   }

   public static @Nullable ScatterTypeEnum valueToScatterTypeEnum(short value) {
      for (ScatterTypeEnum scatterTypeEnum : values()) {
         if (scatterTypeEnum.value == value) {
            return scatterTypeEnum;
         }
      }
      return null;
   }
}
