package no.imr.tools.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/// An [InputStream] that copies every byte read from the underlying stream into an [OutputStream].
///
/// Bytes are copied to `out` as they are read, including bytes consumed via `skip`
/// (skipping is served through `read`, so skipped bytes are copied too).
///
/// `mark`/`reset` are deliberately not supported: bytes already written to `out` cannot be
/// un-written, so rewinding would duplicate them.
///
/// `out` is flushed but not closed by [#close]; the caller retains ownership of it.
public final class TeeInputStream extends InputStream {
   private final InputStream in;
   private final OutputStream out;

   public TeeInputStream(InputStream in, OutputStream out) {
      this.in = in;
      this.out = out;
   }

   @Override
   public int read() throws IOException {
      int nextByte = in.read();
      if (nextByte >= 0) {
         out.write(nextByte);
      }
      return nextByte;
   }

   @Override
   public int read(byte[] b) throws IOException {
      return read(b, 0, b.length);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      int bytesRead = in.read(b, off, len);
      if (bytesRead > 0) {
         out.write(b, off, bytesRead);
      }
      return bytesRead;
   }

   @Override
   public int available() throws IOException {
      return in.available();
   }

   @Override
   public void close() throws IOException {
      try {
         out.flush();
      } finally {
         in.close();
      }
   }
}
