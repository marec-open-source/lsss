package no.imr.lsss.modules.interpretation;

import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverter;
import no.imr.tools.parameter.ValueParameter;

import java.util.function.DoubleUnaryOperator;

public final class FrequencyResponseFunctionParameter extends ValueParameter<FrequencyResponseFunctionParameter.CompiledValue> {
   private static final DoubleUnaryOperator DEFAULT_FUNCTION = F -> Math.pow(38000 / F, 0.4);
   private static final String DEFAULT_FUNCTION_STRING_VALUE = "pow(38000 / F, 0.4)";
   //Herring:  pow(38000 / F, 0.4)
   //Mackerel: -0.03 + pow(38000/F,0.5) + pow(F/125000,3) - pow(F/200000,5.5)

   FrequencyResponseFunctionParameter(Name name) {
      super(name, new CompiledValue(DEFAULT_FUNCTION_STRING_VALUE, DEFAULT_FUNCTION), Unit.NONE, new CompiledValueConverter(),
            "Example: \"pow( 38000/F, 0.4 )\";  38000, F[Hz] are frequencies");
   }

   public DoubleUnaryOperator getFunction() {
      return getValue().function();
   }

   @Override
   public void setStringValue(String stringValue) {
      if (getStringValue().equals(stringValue)) {
         return;
      }
      super.setStringValue(stringValue);
   }

   private static DoubleUnaryOperator compile(String expression) throws CompileException {
      if (expression.equals(DEFAULT_FUNCTION_STRING_VALUE)) {
         return DEFAULT_FUNCTION;
      }

      String source = "package no.marec.compile;\n" +
            "import static java.lang.Math.*;\n" +
            "public final class FrequencyResponseFunctionImpl implements " + DoubleUnaryOperator.class.getName() + " {\n" +
            "   @Override\n" +
            "   public double applyAsDouble(double F) {\n" +
            "      return " + expression + ";\n" +
            "   }\n" + // end method
            "}\n"; // end class

      return CompilerClassLoader.instantiate(DoubleUnaryOperator.class, "no.marec.compile.FrequencyResponseFunctionImpl", source);
   }

   public record CompiledValue(String source, DoubleUnaryOperator function) {
   }

   private static final class CompiledValueConverter implements ValueConverter<CompiledValue> {
      private CompiledValueConverter() {
      }

      @Override
      public CompiledValue parse(String string) throws CompileException {
         return new CompiledValue(string, compile(string));
      }

      @Override
      public String stringify(CompiledValue value) {
         return value.source();
      }
   }
}
