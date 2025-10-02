package no.imr.lsss.modules.interpretation;

import no.imr.tools.Pair;
import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;
import no.imr.tools.math.Function1D;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverter;
import no.imr.tools.parameter.ValueParameter;

public final class FrequencyResponseFunctionParameter extends ValueParameter<Pair<String, Function1D>> {
   private static final Function1D DEFAULT_FUNCTION = F -> Math.pow(38000 / F, 0.4);
   private static final String DEFAULT_FUNCTION_STRING_VALUE = "pow(38000 / F, 0.4)";
   //Herring:  pow(38000 / F, 0.4)
   //Mackerel: -0.03 + pow(38000/F,0.5) + pow(F/125000,3) - pow(F/200000,5.5)

   FrequencyResponseFunctionParameter(Name name) {
      super(name, new Pair<>(DEFAULT_FUNCTION_STRING_VALUE, DEFAULT_FUNCTION), Unit.NONE, new CompileValueConverter(),
            "Example: \"pow( 38000/F, 0.4 )\";  38000, F[Hz] are frequencies");
   }

   public Function1D getFunction() {
      return getValue().second();
   }

   @Override
   public void setStringValue(String stringValue) {
      if (getStringValue().equals(stringValue)) {
         return;
      }
      super.setStringValue(stringValue);
   }

   private static Function1D compile(String expression) throws CompileException {
      if (expression.equals(DEFAULT_FUNCTION_STRING_VALUE)) {
         return DEFAULT_FUNCTION;
      }

      String source = "package no.marec.compile;\n" +
            "import static java.lang.Math.*;\n" +
            "public final class FrequencyResponseFunctionImpl implements " + Function1D.class.getName() + " {\n" +
            "   @Override\n" +
            "   public double eval(double F) {\n" +
            "      return " + expression + ";\n" +
            "   }\n" + // end method
            "}\n"; // end class

      return CompilerClassLoader.instantiate(Function1D.class, "no.marec.compile.FrequencyResponseFunctionImpl", source);
   }

   private static final class CompileValueConverter implements ValueConverter<Pair<String, Function1D>> {
      private CompileValueConverter() {
      }

      @Override
      public Pair<String, Function1D> parse(String string) throws CompileException {
         return new Pair<>(string, compile(string));
      }

      @Override
      public String stringify(Pair<String, Function1D> value) {
         return value.first();
      }
   }
}
