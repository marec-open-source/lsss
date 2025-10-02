package no.imr.korona.cli;

import joptsimple.util.PathConverter;
import joptsimple.util.PathProperties;

import java.nio.file.Path;

public final class AbsolutePathConverter extends PathConverter {
   public AbsolutePathConverter(PathProperties... pathProperties) {
      super(pathProperties);
   }

   @Override
   public Path convert(String value) {
      Path path = super.convert(value);
      return path.toAbsolutePath().normalize();
   }
}
