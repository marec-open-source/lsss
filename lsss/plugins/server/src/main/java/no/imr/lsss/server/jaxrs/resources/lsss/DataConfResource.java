package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.lsss.framework.config.survey.data.DataConf;
import no.imr.lsss.server.pojo.ApiSegmentHandle;
import no.imr.lsss.server.pojo.FileSelection;
import no.imr.lsss.server.util.LsssServerUtils;

import java.util.List;
import java.util.stream.Stream;

public final class DataConfResource extends ConfigurationUnitResource {
   private final DataConf dataConf;

   DataConfResource(DataConf dataConf) {
      super(dataConf);

      this.dataConf = dataConf;
   }

   @GET
   @Path("files")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<ApiSegmentHandle> getFiles() {
      return dataConf.getAllOriginalSegmentHandles().stream()
            .map(ApiSegmentHandle::new);
   }

   @GET
   @Path("files/selection")
   @Produces(MediaType.APPLICATION_JSON)
   public FileSelection getSelection() {
      List<SegmentHandle> allSegmentHandles = dataConf.getAllOriginalSegmentHandles();
      List<SegmentHandle> selectedSegmentHandles = dataConf.getSelectedOriginalSegmentHandles();
      if (selectedSegmentHandles.isEmpty()) {
         return new FileSelection(-1, -1);
      } else {
         int first = allSegmentHandles.indexOf(selectedSegmentHandles.getFirst());
         int last = allSegmentHandles.indexOf(selectedSegmentHandles.getLast());
         return new FileSelection(first, last);
      }
   }

   @POST
   @Path("files/selection")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setSelection(FileSelection fileSelection) {
      List<SegmentHandle> allSegmentHandles = dataConf.getAllOriginalSegmentHandles();
      if (allSegmentHandles.isEmpty()) {
         return;
      }
      SegmentHandle fistSegmentHandle = allSegmentHandles.get(Math.clamp(fileSelection.firstIndex, 0, allSegmentHandles.size() - 1));
      SegmentHandle lastSegmentHandle = allSegmentHandles.get(Math.clamp(fileSelection.lastIndex, 0, allSegmentHandles.size() - 1));
      LsssServerUtils.doInGuiThread(() -> {
         dataConf.selectSegmentHandles(List.of(fistSegmentHandle, lastSegmentHandle));
         dataConf.getConfigurationManager().ok();
      });
   }
}
