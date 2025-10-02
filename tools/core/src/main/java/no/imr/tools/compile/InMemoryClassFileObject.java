package no.imr.tools.compile;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;

/**
 * In memory class file.
 */
final class InMemoryClassFileObject extends SimpleJavaFileObject {
   private final ByteArrayOutputStream out = new ByteArrayOutputStream();

   InMemoryClassFileObject(URI uri) {
      super(uri, Kind.CLASS);
   }

   byte[] getBytes() {
      return out.toByteArray();
   }

   @Override
   public OutputStream openOutputStream() {
      return out;
   }
}
