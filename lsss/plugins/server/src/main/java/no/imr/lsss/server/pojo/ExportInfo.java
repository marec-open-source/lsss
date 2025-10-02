package no.imr.lsss.server.pojo;

import no.imr.lsss.framework.export.StreamingExporter;

public final class ExportInfo {
   public String id;

   public ExportInfo(StreamingExporter exporter) {
      id = exporter.getName().persistentName();
   }

   @Override
   public String toString() {
      return "ExportInfo{" +
            "id='" + id + '\'' +
            '}';
   }
}
