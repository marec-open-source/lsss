package no.imr.lsss.modules.korona.tracking;

import com.google.common.base.Splitter;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.time.NTDate;

import java.util.List;

public record TrackId(long rawFileConfigurationNTDate, int id) implements Comparable<TrackId> {

   private TrackId(RawFileConfiguration rawFileConfiguration, int id) {
      this(rawFileConfiguration.getNTDate(), id);
   }

   TrackId(DataFile dataFile, int id) {
      this(dataFile.getRawFileConfiguration(), id);
   }

   TrackId(Ping ping, int id) {
      this(ping.getRawFileConfiguration(), id);
   }

   static TrackId fromIdString(RawFileConfiguration rawFileConfiguration, String completeId) {
      List<String> parts = Splitter.on(':').splitToList(completeId);
      if (parts.size() == 2) {
         return new TrackId(Long.parseLong(parts.get(0)), Integer.parseInt(parts.get(1)));
      } else {
         return new TrackId(rawFileConfiguration, Integer.parseInt(completeId));
      }
   }

   String toIdString(RawFileConfiguration rawFileConfiguration) {
      if (rawFileConfigurationNTDate == rawFileConfiguration.getNTDate()) {
         return Integer.toString(id);
      } else {
         return rawFileConfigurationNTDate + ":" + id;
      }
   }

   @Override
   public String toString() {
      return "{" + NTDate.ntDateToInstant(rawFileConfigurationNTDate) + ", " + id + '}';
   }

   @Override
   public int compareTo(TrackId that) {
      int c = Long.compare(rawFileConfigurationNTDate, that.rawFileConfigurationNTDate);
      if (c != 0) {
         return c;
      }
      return Integer.compare(id, that.id);
   }
}
