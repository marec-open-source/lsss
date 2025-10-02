package no.imr.lsss.server.jaxrs.resources.lsss;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import no.imr.korona.color.Colormap;
import no.imr.korona.color.Colormaps;
import no.imr.korona.viewer.coloring.ColorConverterContainer;
import no.imr.korona.viewer.coloring.SingleValueColorConverter;
import no.imr.korona.viewer.variables.ContinuousVariable;
import no.imr.lsss.modules.echogram.ColorBarModule;
import no.imr.lsss.server.pojo.values.FloatValue;
import no.imr.lsss.server.pojo.values.StringValue;

import java.util.stream.Stream;

public final class ColorBarModuleResource extends ModuleResource<ColorBarModule> {
   private final ColorConverterContainer colorConverterContainer;

   ColorBarModuleResource(ColorBarModule module) {
      super(module);

      colorConverterContainer = module.getLSSS().getInterpretationSettings().getColorConverterContainer();
   }

   @GET
   @Path("colormap")
   @Produces(MediaType.APPLICATION_JSON)
   public StringValue getColormap() {
      Colormap colormap = colorConverterContainer.getColorConverter().getColormap();
      if (colormap == null) {
         throw new BadRequestException("Not applicable for current visualization type: " + colorConverterContainer.getColorConverter().getType());
      }
      return new StringValue(colormap.getName());
   }

   @POST
   @Path("colormap")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setColormap(StringValue name) {
      Colormap colormap = Colormaps.getByName(name.value);
      if (colormap == null) {
         throw new NotFoundException(name.value);
      }
      colorConverterContainer.setColorConverter(new SingleValueColorConverter(colorConverterContainer.getSV(), colormap));
   }

   @GET
   @Path("colormaps")
   @Produces(MediaType.APPLICATION_JSON)
   public Stream<String> getColormaps() {
      return Colormaps.ALL.stream()
            .map(Colormap::getName);
   }

   @GET
   @Path("threshold/min")
   @Produces(MediaType.APPLICATION_JSON)
   public FloatValue getMin() {
      return new FloatValue(getContinuousVariable().getSettings().getRange().min());
   }

   @POST
   @Path("threshold/min")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setMin(FloatValue min) {
      getContinuousVariable().getSettings().setMin(min.value);
   }

   @GET
   @Path("threshold/max")
   @Produces(MediaType.APPLICATION_JSON)
   public FloatValue getMax() {
      return new FloatValue(getContinuousVariable().getSettings().getRange().max());
   }

   @POST
   @Path("threshold/max")
   @Consumes(MediaType.APPLICATION_JSON)
   public void setMax(FloatValue max) {
      getContinuousVariable().getSettings().setMax(max.value);
   }

   private ContinuousVariable getContinuousVariable() {
      ContinuousVariable continuousVariable = colorConverterContainer.getColorConverter().getContinuousVariable();
      if (continuousVariable == null) {
         throw new BadRequestException("Not applicable for current visualization type: " + colorConverterContainer.getColorConverter().getType());
      }
      return continuousVariable;
   }
}
