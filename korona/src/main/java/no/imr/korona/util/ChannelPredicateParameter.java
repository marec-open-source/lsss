package no.imr.korona.util;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverter;
import no.imr.tools.parameter.ValueParameter;

import java.util.List;
import java.util.stream.IntStream;

public final class ChannelPredicateParameter extends ValueParameter<ChannelPredicateParameter.CompiledValue> {
   private static final CompiledValue ALL = new CompiledValue("true", (_, _, _) -> true);
   private static final CompiledValue NONE = new CompiledValue("false", (_, _, _) -> false);

   private static final CompiledValueConverter CONVERTER = new CompiledValueConverter();

   private ChannelPredicateParameter(Name name, CompiledValue initialValue) {
      super(name, initialValue, Unit.NONE, CONVERTER,
            "Boolean expression using f = frequency in kHz, c = channel number, n = channel count");
   }

   public ChannelPredicateParameter(Name name, boolean initiallyAll) {
      this(name, initiallyAll ? ALL : NONE);
   }

   public ChannelPredicateParameter(Name name, String initialExpression) {
      CompiledValue value;
      try {
         value = CONVERTER.parse(initialExpression);
      } catch (CompileException e) {
         throw new IllegalArgumentException(e);
      }
      this(name, value);
   }

   public List<Integer> selectedChannels(RawFileConfiguration rawFileConfiguration) {
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      int n = transducers.size();
      return IntStream.rangeClosed(1, n)
            .filter(c -> {
               float f = transducers.get(c - 1).getKHz();
               return getValue().predicate().test(f, c, n);
            })
            .boxed()
            .toList();
   }

   private static CompiledChannelPredicate compile(String expression) throws CompileException {
      String source = ""
            + "package no.marec.compile;\n"
            + "public final class CompiledChannelPredicateImpl implements " + CompiledChannelPredicate.class.getName() + " {\n"
            + "   @Override\n"
            + "   public boolean test(float f, int c, int n) {\n"
            + "      return " + expression + ";\n"
            + "   }\n"
            + "}\n";

      return CompilerClassLoader.instantiate(CompiledChannelPredicate.class, "no.marec.compile.CompiledChannelPredicateImpl", source);
   }

   public record CompiledValue(String source, CompiledChannelPredicate predicate) {
   }

   private static final class CompiledValueConverter implements ValueConverter<CompiledValue> {
      private CompiledValueConverter() {
      }

      @Override
      public CompiledValue parse(String string) throws CompileException {
         for (CompiledValue value : List.of(ALL, NONE)) {
            if (string.equals(value.source())) {
               return value;
            }
         }
         return new CompiledValue(string, compile(string));
      }

      @Override
      public String stringify(CompiledValue value) {
         return value.source();
      }
   }
}
