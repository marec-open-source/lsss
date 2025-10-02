package no.imr.tools.compile;

import javax.tools.SimpleJavaFileObject;
import java.net.URI;

/**
 * In memory source file.
 */
final class InMemorySourceFileObject extends SimpleJavaFileObject {
   private final String content;

   InMemorySourceFileObject(URI uri, String content) {
      super(uri, Kind.SOURCE);

      this.content = content;
   }

   @Override
   public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return content;
   }
}
