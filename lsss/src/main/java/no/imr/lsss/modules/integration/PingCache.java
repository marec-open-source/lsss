package no.imr.lsss.modules.integration;

public final class PingCache {
   private float verticallyIntegratedSvTotal;
   private float verticallyIntegratedSvPelagic;
   private float verticallyIntegratedSvBottom;

   PingCache() {
   }

   PingCache(float verticallyIntegratedSvTotal, float verticallyIntegratedSvPelagic, float verticallyIntegratedSvBottom) {
      this.verticallyIntegratedSvTotal = verticallyIntegratedSvTotal;
      this.verticallyIntegratedSvPelagic = verticallyIntegratedSvPelagic;
      this.verticallyIntegratedSvBottom = verticallyIntegratedSvBottom;
   }

   void accumulate(PingCache other) {
      verticallyIntegratedSvTotal += other.verticallyIntegratedSvTotal;
      verticallyIntegratedSvPelagic += other.verticallyIntegratedSvPelagic;
      verticallyIntegratedSvBottom += other.verticallyIntegratedSvBottom;
   }

   void accumulate(PingCache other, float weight) {
      verticallyIntegratedSvTotal += weight * other.verticallyIntegratedSvTotal;
      verticallyIntegratedSvPelagic += weight * other.verticallyIntegratedSvPelagic;
      verticallyIntegratedSvBottom += weight * other.verticallyIntegratedSvBottom;
   }

   float getVerticallyIntegratedSvTotal() {
      return verticallyIntegratedSvTotal;
   }

   float getVerticallyIntegratedSvPelagic() {
      return verticallyIntegratedSvPelagic;
   }

   float getVerticallyIntegratedSvBottom() {
      return verticallyIntegratedSvBottom;
   }

   public float getVerticallyIntegratedSv(IntegrationArea integrationArea) {
      return switch (integrationArea) {
         case TOTAL -> verticallyIntegratedSvTotal;
         case PELAGIC -> verticallyIntegratedSvPelagic;
         case BOTTOM -> verticallyIntegratedSvBottom;
      };
   }
}
