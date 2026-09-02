package no.imr.korona.data.metadata.pojo;

import java.util.ArrayList;
import java.util.List;

public final class Metadata {
   public List<ConfigurationInstance> configurations = new ArrayList<>();
   public MetadataErrors errors = new MetadataErrors();

   public Metadata() {
   }

   public static final class ConfigurationInstance {
      public MetadataConfiguration configuration;
      public List<String> files;

      public ConfigurationInstance(MetadataConfiguration configuration, List<String> files) {
         this.configuration = configuration;
         this.files = files;
      }
   }

   public static class MetadataErrors {
      public List<String> unusable = new ArrayList<>();
      public List<String> nonMonotonicVariables = new ArrayList<>();

      public MetadataErrors() {
      }
   }
}
