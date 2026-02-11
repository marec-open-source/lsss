package no.imr.korona.data.formats.netcdf;

import no.imr.korona.data.datagrams.Bot0Datagram;
import no.imr.korona.data.ping.PingConfiguration;
import no.imr.korona.data.ping.PingData;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.WrapAround;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.track.SegmentData;
import no.imr.tools.concurrent.AsyncHandle;
import org.jspecify.annotations.Nullable;
import ucar.ma2.InvalidRangeException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

final class NetcdfSegmentData extends SegmentData {
   private final NetcdfFileData netcdfFileData;
   private final List<PingIndex> pingIndexes;
   private final PingConfiguration pingConfiguration;
   private final List<Bot0Datagram> bot0Datagrams;

   NetcdfSegmentData(Path file) throws IOException {
      netcdfFileData = new NetcdfFileData(file);
      try {
         pingIndexes = netcdfFileData.createPingIndexes();

         RawFileConfiguration rawFileConfiguration = netcdfFileData.createRawFileConfiguration(pingIndexes.getFirst().getNTDate());
         pingConfiguration = new PingConfiguration(rawFileConfiguration);

         bot0Datagrams = netcdfFileData.createBot0Datagrams(pingIndexes);
      } catch (Exception e) {
         try {
            netcdfFileData.close();
         } catch (IOException suppressed) {
            e.addSuppressed(suppressed);
         }
         throw e;
      }
   }

   @Override
   public List<PingIndex> getPingIndices() {
      return pingIndexes;
   }

   @Override
   public @Nullable WrapAround getWrapAround() {
      return null;
   }

   @Override
   public List<Bot0Datagram> getBot0Datagrams() {
      return bot0Datagrams;
   }

   @Override
   public void close() throws IOException {
      synchronized (netcdfFileData) {
         netcdfFileData.close();
      }
   }

   @Override
   public PingConfiguration getPingConfiguration() {
      return pingConfiguration;
   }

   @Override
   public PingData loadPingData(PingIndex pingIndex, AsyncHandle asyncHandle) throws IOException {
      int pingTimeIndex = (int) (pingIndex.getPingNumber() - pingIndexes.getFirst().getPingNumber());
      PingData pingData = new PingData(pingConfiguration);
      synchronized (netcdfFileData) {
         try {
            netcdfFileData.addPingItems(pingData, pingIndex, pingTimeIndex, asyncHandle);
         } catch (InvalidRangeException e) {
            throw new IOException(e);
         }
      }
      return pingData;
   }
}
