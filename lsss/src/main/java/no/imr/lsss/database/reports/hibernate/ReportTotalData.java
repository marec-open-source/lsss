package no.imr.lsss.database.reports.hibernate;

public record ReportTotalData(
      int frequency,
      short transceiver,
      short scatterType,
      int acousticCategory,
      int channelNumber,
      double sumSa) {
}
