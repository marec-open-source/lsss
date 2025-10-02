package no.imr.tools.parameter.misc;

import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

public final class ReferenceDirectory extends FileParameter {
   public ReferenceDirectory(Name name) {
      this(name, null);
   }

   public ReferenceDirectory(Name name, @Nullable Path initialValue) {
      super(name, initialValue, Mode.DIRECTORY);
   }
}
