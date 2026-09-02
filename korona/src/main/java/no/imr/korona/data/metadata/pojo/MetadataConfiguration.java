package no.imr.korona.data.metadata.pojo;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;

import java.util.ArrayList;
import java.util.List;

public final class MetadataConfiguration {
   public String surveyName;
   public String sounderName;
   public String version;
   public List<MetadataChannel> channels = new ArrayList<>();

   public MetadataConfiguration(RawFileConfiguration rawFileConfiguration) {
      surveyName = rawFileConfiguration.getSurveyName();
      sounderName = rawFileConfiguration.getSounderName();
      version = rawFileConfiguration.getVersion();

      for (RawFileTransducer transducer : rawFileConfiguration.getTransducers()) {
         channels.add(new MetadataChannel(transducer));
      }
   }

   @Override
   public String toString() {
      return "MetadataConfiguration{" +
            "surveyName='" + surveyName + '\'' +
            ", sounderName='" + sounderName + '\'' +
            ", version='" + version + '\'' +
            ", transducers=" + channels +
            '}';
   }

   @Override
   public boolean equals(Object o) {
      return o instanceof MetadataConfiguration that
            && surveyName.equals(that.surveyName)
            && sounderName.equals(that.sounderName)
            && version.equals(that.version)
            && channels.equals(that.channels);
   }

   @Override
   public int hashCode() {
      int result = surveyName.hashCode();
      result = 31 * result + sounderName.hashCode();
      result = 31 * result + version.hashCode();
      result = 31 * result + channels.hashCode();
      return result;
   }
}
