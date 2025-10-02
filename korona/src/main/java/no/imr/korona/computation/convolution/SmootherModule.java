package no.imr.korona.computation.convolution;

import no.imr.korona.computation.ComputationContext;
import no.imr.korona.computation.GeneralPingModuleComputation;
import no.imr.korona.data.ping.PingSource;
import no.imr.tools.UnionList;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.FloatParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ObjectParameter;
import no.imr.tools.parameter.Unit;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.io.IOException;
import java.util.List;

/**
 * Performs smoothing by convolution.
 */
public final class SmootherModule extends BaseFilterModule {
   public static final String GAUSSIAN = "gaussian";
   public static final String TOPHAT = "tophat";
   public static final List<String> KERNEL_TYPES = List.of(GAUSSIAN, TOPHAT);

   public final ObjectParameter<String> horizontalKernelType = new ObjectParameter<>(
         new Name("HorizontalKernelType", "Horizontal kernel type"),
         GAUSSIAN, KERNEL_TYPES,
         "Convolution kernel type horizontally");

   public final ObjectParameter<String> verticalKernelType = new ObjectParameter<>(
         new Name("VerticalKernelType", "Vertical kernel type"),
         GAUSSIAN, KERNEL_TYPES,
         "Convolution kernel type vertically");

   public final FloatParameter horizontalWidth = new FloatParameter(
         new Name("HorizontalWidth", "Horizontal width"),
         8, Unit.METER, ValueConstraints.gt(0f),
         "Convolution kernel width");

   public final FloatParameter verticalWidth = new FloatParameter(
         new Name("VerticalWidth", "Vertical width"),
         0.5f, Unit.METER, ValueConstraints.gt(0f),
         "Convolution kernel height");

   public final BooleanParameter logarithmicValues = new BooleanParameter(
         new Name("LogarithmicValues", "Logarithmic values"),
         false,
         "If checked use log(sv), otherwise use (linear) sv");

   public SmootherModule() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return new UnionList<>(super.getParameters(), List.of(
            horizontalKernelType,
            verticalKernelType,
            horizontalWidth,
            verticalWidth,
            logarithmicValues
      ));
   }

   public static ConvolutionKernel createConvolutionKernel(String type, float width) {
      if (type.equals(GAUSSIAN)) {
         return SmootherKernel.gaussian(width);
      }
      if (type.equals(TOPHAT)) {
         return SmootherKernel.tophat(width);
      }
      throw new IllegalArgumentException(type);
   }

   @Override
   public GeneralPingModuleComputation createComputation(ComputationContext computationContext, PingSource pingSource) throws IOException {
      return new SmootherModuleComputation(this, computationContext, pingSource);
   }
}
