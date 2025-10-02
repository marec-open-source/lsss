package no.imr.korona.computation.broadband.pulsecompressionfilter;

import no.imr.korona.data.ping.items.configuration.PulseForm;
import no.imr.tools.Utils;
import no.imr.tools.listening.Listener;
import no.imr.tools.math.ComplexArray;
import no.imr.tools.parameter.FloatCsvListParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalFloatParameter;
import no.imr.tools.parameter.OptionalIntParameter;
import no.imr.tools.parameter.OptionalParameter;
import no.imr.tools.parameter.OptionalStringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueParameter;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public final class PulseCompressionFilterConfig {
   public final OptionalFloatParameter beginSlope = new OptionalFloatParameter(
         new Name(PulseCompressionFiltersFileService.MIN_SLOPE, "Minimum slope"),
         Optional.empty(), Unit.NONE, ValueConstraints.gteLte(0.0f, 0.5f),
         "Lower bound for slope (inclusive)");

   public final OptionalFloatParameter endSlope = new OptionalFloatParameter(
         new Name(PulseCompressionFiltersFileService.MAX_SLOPE, "Maximum slope"),
         Optional.empty(), Unit.NONE, ValueConstraints.gteLte(0.0f, 0.5f),
         "Upper bound for slope (exclusive)");

   public final OptionalFloatParameter pulseDuration = new OptionalFloatParameter(
         new Name(PulseCompressionFiltersFileService.PULSE_DURATION, "Pulse duration"),
         Optional.empty(), Unit.SECONDS,
         "Pulse duration in seconds");

   public final OptionalStringParameter pulseForm = new OptionalStringParameter(
         new Name(PulseCompressionFiltersFileService.PULSE_FORM, "Pulse form"),
         Optional.empty(),
         "Pulse form identifier");

   public final OptionalIntParameter refStage = new OptionalIntParameter(
         new Name(PulseCompressionFiltersFileService.REF_STAGE, "Reference filter stage"),
         Optional.empty(), Unit.NONE,
         "Reference to one of the predefined stage filters");

   public final OptionalIntParameter decimationFactor = new OptionalIntParameter(
         new Name(PulseCompressionFiltersFileService.DECIMATION_FACTOR, "Decimation factor"),
         Optional.of(1), Unit.NONE, ValueConstraints.gt(0),
         "Reference to one of the predefined stage filters");

   public final FloatCsvListParameter coefficientsRe = new FloatCsvListParameter(
         new Name(PulseCompressionFiltersFileService.COEFFICIENTS_RE, "Re(filer coefficients)"),
         List.of(), Unit.NONE,
         "List of real parts of filter coefficients");

   public final FloatCsvListParameter coefficientsIm = new FloatCsvListParameter(
         new Name(PulseCompressionFiltersFileService.COEFFICIENTS_IM, "Im(filer coefficients)"),
         List.of(), Unit.NONE,
         "List of imaginary parts of filter coefficients");

   private volatile @Nullable ComplexArray coefficients; // Compute lazily.

   PulseCompressionFilterConfig() {
      refStage.addListenerAndNotify(optionalValue -> {
         boolean notRef = optionalValue.isEmpty();
         decimationFactor.setEnabled(notRef);
         coefficientsRe.setEnabled(notRef);
         coefficientsIm.setEnabled(notRef);
      });
      Listener.of(() -> coefficients = null).addTo(
            coefficientsRe,
            coefficientsIm
      );
   }

   public List<OptionalParameter<?>> getPulseParameters() {
      return List.of(beginSlope, endSlope, pulseDuration, pulseForm);
   }

   public List<ValueParameter<?>> getFilterParameters() {
      return List.of(decimationFactor, coefficientsRe, coefficientsIm);
   }

   public boolean isValid(float slope, float aPulseDuration, int aPulseForm) {
      return beginSlope.getValue().orElse(Float.NEGATIVE_INFINITY) <= slope
            && slope < endSlope.getValue().orElse(Float.POSITIVE_INFINITY)
            && pulseDuration.getValue().orElse(aPulseDuration) == aPulseDuration
            && PulseForm.stringPulseFormToInt(pulseForm.getValue().orElse(String.valueOf(aPulseForm))) == aPulseForm;
   }

   public ComplexArray getCoefficients() {
      ComplexArray coefficients = this.coefficients;
      if (coefficients == null) {
         coefficients = ComplexArray.of(
               Utils.toDoubles(coefficientsRe.getValue()),
               Utils.toDoubles(coefficientsIm.getValue()));
         this.coefficients = coefficients;
      }
      return coefficients;
   }
}
