package no.imr.lsss.modules.korona.tracking;

import com.google.common.base.Splitter;
import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.time.NTDate;

import java.time.Instant;
import java.util.List;

public record TrackId(Instant rawFileConfigurationInstant, int id) implements Comparable<TrackId> {

   private TrackId(RawFileConfiguration rawFileConfiguration, int id) {
      this(rawFileConfiguration.getInstant(), id);
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
         return new TrackId(NTDate.ntDateStringToInstant(parts.get(0)), Integer.parseInt(parts.get(1)));
      } else {
         return new TrackId(rawFileConfiguration, Integer.parseInt(completeId));
      }
   }

   String toIdString(RawFileConfiguration rawFileConfiguration) {
      if (rawFileConfigurationInstant.equals(rawFileConfiguration.getInstant())) {
         return Integer.toString(id);
      } else {
         return NTDate.instantToNTDateString(rawFileConfigurationInstant) + ":" + id;
      }
   }

   @Override
   public String toString() {
      return "{" + rawFileConfigurationInstant + ", " + id + '}';
   }

   @Override
   public int compareTo(TrackId other) {
      int c = rawFileConfigurationInstant.compareTo(other.rawFileConfigurationInstant);
      if (c != 0) {
         return c;
      }
      return Integer.compare(id, other.id);
   }
}
