package no.imr.tools.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
   public long skip(long n) throws IOException {
      return in.skip(n);
   }

   @Override
   public int available() throws IOException {
      return in.available();
   }

   @Override
   public void close() throws IOException {
      in.close();
   }

   @Override
   public void mark(int readlimit) {
      in.mark(readlimit);
   }

   @Override
   public void reset() throws IOException {
      in.reset();
   }

   @Override
   public boolean markSupported() {
      return in.markSupported();
   }
}
