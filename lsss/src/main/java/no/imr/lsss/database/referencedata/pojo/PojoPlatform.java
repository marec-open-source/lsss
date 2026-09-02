package no.imr.lsss.database.referencedata.pojo;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

public record PojoPlatform(
      short nation,
      int platformId,
      int platformType,
      int platformSubType,
      @Nullable LocalDate firstValidDate,
      @Nullable LocalDate lastValidDate,
      @Nullable List<Name> names,
      @Nullable List<Code> codes
) {

   public record Name(
         String name,
         @Nullable LocalDate firstValidDate,
         @Nullable LocalDate lastValidDate
   ) {
   }

   public record Code(
         String codeName,
         String codeValue,
         @Nullable LocalDate firstValidDate,
         @Nullable LocalDate lastValidDate
   ) {
   }
}
