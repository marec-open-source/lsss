package no.imr.tools.io;

import no.imr.tools.Max;

import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;

public record LastModifiedAndSize(Instant lastModified, long size) {

   public LastModifiedAndSize(BasicFileAttributes attributes) {
      this(attributes.lastModifiedTime().toInstant(), attributes.size());
   }

   public LastModifiedAndSize combine(LastModifiedAndSize lastModifiedAndSize) {
      return new LastModifiedAndSize(
            Max.of(lastModified, lastModifiedAndSize.lastModified),
            size + lastModifiedAndSize.size
      );
   }
}
