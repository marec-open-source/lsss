package no.imr.lsss.database.reports;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

final class RewindableBufferedReader implements AutoCloseable {
   private final Path mFile;
   private final Charset mCharset;
   private BufferedReader mReader;

   RewindableBufferedReader(Path aFile, Charset aCharset) throws IOException {
      mFile = aFile;
      mCharset = aCharset;
      mReader = Files.newBufferedReader(aFile, aCharset);
   }

   BufferedReader getReader() {
      return mReader;
   }

   void rewind() throws IOException {
      mReader.close();
      mReader = Files.newBufferedReader(mFile, mCharset);
   }

   @Override
   public void close() throws IOException {
      mReader.close();
   }
}
