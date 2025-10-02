package no.imr.korona.cli.commands.pojo;

import java.util.List;

public record AnnotationCoordinates(
      String status,
      String objectType,
      int objectNumber,
      BoundingBox boundingBox,
      List<Category> categories
) {

   public record BoundingBox(
         String startTime,
         String stopTime,
         float startRange,
         float stopRange
   ) {
   }

   public record Category(
         int category,
         float annotation
   ) {
   }
}
