package no.imr.tools.io;

import java.nio.file.attribute.BasicFileAttributes;

public record LastModifiedAndSize(long lastModified, long size) {

   public static final LastModifiedAndSize ZERO = new LastModifiedAndSize(0, 0);

   public LastModifiedAndSize(BasicFileAttributes attributes) {
      this(attributes.lastModifiedTime().toMillis(), attributes.size());
   }

   public LastModifiedAndSize combine(LastModifiedAndSize lastModifiedAndSize) {
      return new LastModifiedAndSize(
            Math.max(lastModified, lastModifiedAndSize.lastModified),
            size + lastModifiedAndSize.size
      );
   }
}
