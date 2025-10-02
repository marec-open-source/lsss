package no.imr.lsss.framework.config.application;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;

import java.util.List;

public final class LsssServerSettings implements ParameterContainer {
   public final BooleanParameter prettyPrint = new BooleanParameter(
         new Name("PrettyPrint", "Pretty print"),
         true,
         "Uses indentation and line breaks");

   public final BooleanParameter quoteNonNumericNumbers = new BooleanParameter(
         new Name("QuoteNonNumericNumbers", "Quote non-numeric numbers"),
         true,
         "Applies to NaN and Infinity. Quotes are needed for strictly valid JSON");

   LsssServerSettings() {
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            prettyPrint,
            quoteNonNumericNumbers
      );
   }
}
