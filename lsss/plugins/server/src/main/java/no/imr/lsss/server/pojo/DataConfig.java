package no.imr.lsss.server.pojo;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.tools.parameter.Unit;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DataConfig {
   public @Nullable Map<String, Object> info;
   public @Nullable List<PojoData> transducers;

   public DataConfig(DataFileSet dataFileSet) {
      if (dataFileSet.isEmpty()) {
         return;
      }
      PojoData.Builder pojoDataBuilder = PojoData.newBuilder("");
      info = new LinkedHashMap<>();
      info.put("units", pojoDataBuilder.getInfo().units);
      RawFileConfiguration rawFileConfiguration = dataFileSet.getRawFileConfiguration();
      transducers = rawFileConfiguration.getTransducers().stream()
            .map(transducer -> {
               return pojoDataBuilder.newBuilder()
                     .with("id", transducer.getChannelId())
                     .with("name", transducer.getXml0Info() != null ? transducer.getXml0Info().getName() : "")
                     .with("serialNumber", transducer.getXml0Info() != null ? transducer.getXml0Info().getTransducerSerialNumber() : "")
                     .with("frequency", Unit.HZ, transducer.getFrequency())
                     .with("angleOffsetAlongship", Unit.DEGREES, transducer.getAngleOffsetAlongship())
                     .with("angleOffsetAthwartship", Unit.DEGREES, transducer.getAngleOffsetAthwartship())
                     .with("angleSensitivityAlongship", transducer.getAngleSensitivityAlongship())
                     .with("angleSensitivityAthwartship", transducer.getAngleSensitivityAthwartship())
                     .with("beamWidthAlongship", Unit.DEGREES, transducer.getBeamWidthAlongship())
                     .with("beamWidthAthwartship", Unit.DEGREES, transducer.getBeamWidthAthwartship())
                     .with("equivalentBeamAngle", Unit.DB, transducer.getEquivalentBeamAngle())
                     .with("pulseDuration", Unit.SECONDS, transducer.getPulseDurationTable())
                     .with("gain", Unit.DB, transducer.getGainTable())
                     .with("saCorrection", Unit.DB, transducer.getSaCorrectionTable())
                     .build();
            })
            .toList();
   }

   @Override
   public String toString() {
      return "DataConfig{" +
            "info=" + info +
            ", transducers=" + transducers +
            '}';
   }
}
