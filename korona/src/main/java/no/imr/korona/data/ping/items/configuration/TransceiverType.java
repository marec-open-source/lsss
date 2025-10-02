package no.imr.korona.data.ping.items.configuration;

public final class TransceiverType {
   public static final String WBT = "WBT";

   private TransceiverType() {
   }

   public static boolean isWBT(String transceiverType) {
      // Using 'contains' since types "WBT Tube" etc. should be handled similarly.
      // See email from Sverre Berg (Simrad), 17.08.2018 12.43.
      return transceiverType.contains(WBT);
   }
}
