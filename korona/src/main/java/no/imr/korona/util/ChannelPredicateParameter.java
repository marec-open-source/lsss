package no.imr.korona.util;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.Pair;
import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverter;
import no.imr.tools.parameter.ValueParameter;

import java.util.List;
import java.util.stream.IntStream;

public final class ChannelPredicateParameter extends ValueParameter<Pair<String, CompiledChannelPredicate>> {
   private static final Pair<String, CompiledChannelPredicate> ALL = new Pair<>("true", (f, c, n) -> true);
   private static final Pair<String, CompiledChannelPredicate> NONE = new Pair<>("false", (f, c, n) -> false);

   public ChannelPredicateParameter(Name name, boolean initiallyAll) {
      super(name, initiallyAll ? ALL : NONE, Unit.NONE, new CompileValueConverter(),
            "Boolean expression using f = frequency in kHz, c = channel number, n = channel count");
   }

   public List<Integer> selectedChannels(RawFileConfiguration rawFileConfiguration) {
      List<RawFileTransducer> transducers = rawFileConfiguration.getTransducers();
      int n = transducers.size();
      return IntStream.rangeClosed(1, n)
            .filter(c -> {
               float f = transducers.get(c - 1).getKHz();
               return getValue().second().test(f, c, n);
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

   private static final class CompileValueConverter implements ValueConverter<Pair<String, CompiledChannelPredicate>> {
      private CompileValueConverter() {
      }

      @Override
      public Pair<String, CompiledChannelPredicate> parse(String string) throws CompileException {
         for (Pair<String, CompiledChannelPredicate> value : List.of(ALL, NONE)) {
            if (string.equals(value.first())) {
               return value;
            }
         }
         return new Pair<>(string, compile(string));
      }

      @Override
      public String stringify(Pair<String, CompiledChannelPredicate> value) {
         return value.first();
      }
   }
}
