package no.imr.korona.data.ping;

import java.nio.file.Path;

/**
 * Base class for ping readers.
 */
public interface PingReader extends PingSource {
   Path getFile();

   float getReadFraction();
}
