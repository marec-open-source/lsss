package no.imr.korona.data.formats.ek60.io;

import no.imr.korona.data.DatagramSource;
import no.imr.korona.data.datagrams.BaseDatagram;
import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.DatagramType;
import no.imr.korona.data.datagrams.DatagramTypeManager;
import no.imr.korona.data.datagrams.UnknownDatagram;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.function.Predicate;

/**
 * Base abstract class for reading datagrams.
 * <p>
 * A successful reading of a datagram from the current position, p, requires the following:
 * <ol>
 * <li>The int at position p, which is the number of bytes in the datagram, n, is in the range [12, {@link BaseDatagram#MAX_DATAGRAM_SIZE}].</li>
 * <li>The int at position p + 4 + n equals n.</li>
 * <li>Parsing the datagram from position p + 4 requires exactly n bytes.</li>
 * </ol>
 * If these conditions are not met then a valid datagram cannot be read from position p.
 * A search for the next position p' where a datagram can be read is started. The search requires in addition the following:
 * <ol>
 * <li>The int at position p' + 4 must correspond to a known {@link DatagramType}.</li>
 * <li>The long at position p' + 8 must be a time, t, such that if no datagrams have been read yet
 * <blockquote>
 * t must be in the range [year 1900, year 9999]
 * </blockquote>
 * otherwise
 * <blockquote>
 * t must be in the range [t<sub>0</sub> - 1 hour, t<sub>0</sub> + 1 year],<br>
 * where t<sub>0</sub> is the time of the last datagram.
 * </blockquote>
 * </li>
 * </ol>
 */
public abstract class BaseDatagramReader implements DatagramSource, Closeable {
   static final long MAX_NT_DATE = 2650466880000000000L;

   private final DatagramTypeManager datagramTypeManager;
   private Predicate<DatagramType> readPredicate = _ -> true;
   private Predicate<BaseDatagram> acceptPredicate = _ -> true;

   private long totalRead;
   private long bytesSkipped;

   protected BaseDatagramReader(DatagramTypeManager datagramTypeManager) {
      this.datagramTypeManager = datagramTypeManager;
   }

   /**
    * Returns the ByteBuffer to read from.
    * This buffer may change if a reallocation is needed.
    *
    * @return a ByteBuffer
    */
   protected abstract ByteBuffer getReadBuffer();

   /**
    * Skips some bytes from the current position.
    * The current position is updated.
    *
    * @param bytesToSkip number of bytes to skip from the current position
    * @throws BufferUnderflowException if not enough bytes remain to skip
    * @throws IOException              if some IO error occurs
    */
   protected abstract void skip(int bytesToSkip) throws IOException;

   /**
    * Reads data to the read buffer.
    * The buffer might have to be reallocated.
    * Data before the current position may be discarded.
    * Data after the current position must be intact.
    * The current position might change.
    *
    * @param bytesToRemain the number of bytes to remain
    * @throws IOException if some IO error occurs
    */
   protected abstract void readToEnsureRemaining(int bytesToRemain) throws IOException;

   private boolean ensureRemaining(int bytesToRemain) throws IOException {
      if (getReadBuffer().remaining() >= bytesToRemain) {
         return true;
      } else {
         readToEnsureRemaining(bytesToRemain);
         return getReadBuffer().remaining() >= bytesToRemain;
      }
   }

   public void setReadPredicate(Predicate<DatagramType> readPredicate) {
      this.readPredicate = readPredicate;
   }

   public void setAcceptPredicate(Predicate<BaseDatagram> acceptPredicate) {
      this.acceptPredicate = acceptPredicate;
   }

   /**
    * Get total read (including skipped bytes when searching for valid datagram after bad data).
    *
    * @return total number of bytes
    */
   public long getTotalRead() {
      return totalRead;
   }

   public long getBytesSkipped() {
      return bytesSkipped;
   }

   @Override
   public @Nullable BaseDatagram nextDatagram() throws IOException {
      return nextDatagram(new AsyncHandle());
   }

   public @Nullable BaseDatagram nextDatagram(AsyncHandle asyncHandle) throws IOException {
      while (true) {
         BaseDatagram datagram = readDatagram();
         if (datagram == null) {
            datagram = searchForNextDatagram(asyncHandle);
            if (datagram == null) {
               return null;
            }
         }
         if (acceptPredicate.test(datagram)) {
            return datagram;
         }
      }
   }

   private @Nullable BaseDatagram readDatagram() throws IOException {
      while (true) {
         if (!ensureRemaining(4 + 4 + 8)) { // size + code + date
            return null;
         }
         int p = getReadBuffer().position();

         int datagramSize = getReadBuffer().getInt(p);
         if (datagramSize < 12 || datagramSize > BaseDatagram.MAX_DATAGRAM_SIZE) {
            return null;
         }

         int intCode = getReadBuffer().getInt(p + 4);
         long ntDate = getReadBuffer().getLong(p + 8);
         if (!isValidNTDate(ntDate)) {
            return null;
         }

         DatagramType datagramType = datagramTypeManager.getDatagramType(intCode);
         if (datagramType == null) {
            datagramType = UnknownDatagram.type(intCode);
         }
         if (!readPredicate.test(datagramType)) {
            try {
               skip(datagramSize + 8);
               continue;
            } catch (BufferUnderflowException _) {
               // Skip failed
               return null;
            }
         }

         if (!ensureRemaining(datagramSize + 8)) {
            return null;
         }
         p = getReadBuffer().position();

         int datagramEndSizePosition = p + 4 + datagramSize;
         int datagramSizeEnd = getReadBuffer().getInt(datagramEndSizePosition);
         if (datagramSize != datagramSizeEnd) {
            return null;
         }

         int savedLimit = getReadBuffer().limit();
         getReadBuffer().limit(datagramEndSizePosition); // This avoids reading too far during datagram parsing

         getReadBuffer().position(p + (4 + 4 + 8)); // Skip size + code + date
         BaseDatagram datagram;
         try {
            datagram = datagramType.getFactory().read(ntDate, getReadBuffer(), datagramTypeManager);
         } catch (DatagramFormatException | BufferUnderflowException _) {
            // Parsing failed
            datagram = null;
         }

         getReadBuffer().limit(savedLimit); // Restore saved limit

         if (datagram == null || getReadBuffer().position() != datagramEndSizePosition) {
            getReadBuffer().position(p); // Reset position
            return null;
         } else {
            // Successfully read a datagram!
            getReadBuffer().position(getReadBuffer().position() + 4); // Skip last int
            totalRead += datagramSize + 8;
            return datagram;
         }
      }
   }

   private @Nullable BaseDatagram searchForNextDatagram(AsyncHandle asyncHandle) throws IOException {
      while (true) {
         if (asyncHandle.isCancelled()) {
            return null;
         }
         int minBytes = 1 + 4 + 4 + 8; // skip + size + code + date
         if (getReadBuffer().remaining() < minBytes) {
            ensureRemaining(4096);
            if (getReadBuffer().remaining() < minBytes) {
               // End of input. Consume remaining.
               while (getReadBuffer().hasRemaining()) {
                  skipOneByte();
               }
               return null;
            }
         }

         skipOneByte();
         int p = getReadBuffer().position();

         // Do extra tests before trying to read datagram

         int datagramCode = getReadBuffer().getInt(p + 4);
         if (datagramTypeManager.getDatagramType(datagramCode) == null) {
            continue;
         }

         long ntDate = getReadBuffer().getLong(p + 8);
         if (!isValidNTDate(ntDate)) {
            continue;
         }

         // Extra test passed, try to read datagram

         BaseDatagram datagram = readDatagram();

         if (datagram != null) {
            return datagram;
         }
      }
   }

   private void skipOneByte() {
      getReadBuffer().get();
      bytesSkipped++;
      totalRead++;
   }

   private static boolean isValidNTDate(long ntDate) {
      return ntDate >= 0 && ntDate <= MAX_NT_DATE;
   }
}
