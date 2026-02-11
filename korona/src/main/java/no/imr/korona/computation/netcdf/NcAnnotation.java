package no.imr.korona.computation.netcdf;

public final class NcAnnotation {
   public static final String CATEGORY = "category";
   public static final String PING_TIME = Nc.PING_TIME;
   public static final String RANGE = Nc.RANGE;

   public static final String ANNOTATION = "annotation";
   public static final String OBJECT_NUMBER = "object_number";
   public static final String OBJECT_TYPE = "object_type";
   public static final String LOWER_THRESHOLD = "lower_threshold";
   public static final String UPPER_THRESHOLD = "upper_threshold";

   public static final int OBJECT_NUMBER_NOTHING = -1;

   public static final int OBJECT_TYPE_LAYER = 3;
   public static final int OBJECT_TYPE_SCHOOL = 2;
   public static final int OBJECT_TYPE_DELETION = 1;
   public static final int OBJECT_TYPE_EXCLUSION = 0;
   public static final int OBJECT_TYPE_NOTHING = -1;

   private NcAnnotation() {
   }
}
