package no.imr.korona.computation.categorization.netcdf.pojo;

import java.util.List;

public record AnnotationConfig(
      List<AnnotationCategory> categories
) {

   public record AnnotationCategory(
         int id,
         KoronaCategory koronaCategory
   ) {
   }

   public record KoronaCategory(
         String name,
         String legend,
         String color
   ) {
   }
}
