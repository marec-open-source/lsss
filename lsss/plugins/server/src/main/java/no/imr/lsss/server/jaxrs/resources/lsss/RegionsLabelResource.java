package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import no.imr.lsss.LSSS;
import no.imr.lsss.util.LabelUtils;

public final class RegionsLabelResource {
   private final LSSS lsss;
   private final String label;

   RegionsLabelResource(LSSS lsss, String label) {
      this.lsss = lsss;
      this.label = label;
   }

   @POST
   public void add() {
      LabelUtils.add(lsss.getRegionManager().getSelectedRegions(), label);
   }

   @DELETE
   public void remove() {
      LabelUtils.remove(lsss.getRegionManager().getSelectedRegions(), label);
   }
}
