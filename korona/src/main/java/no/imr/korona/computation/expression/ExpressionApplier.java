package no.imr.korona.computation.expression;

import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.channel.PowerData;
import no.imr.korona.data.util.ResampledFloatArray;
import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;
import no.imr.tools.logging.Log;
import no.imr.tools.math.MathUtils;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Creates a {@link CompiledExpression} from a string and applies it to a {@link Ping}.
 */
final class ExpressionApplier {
   private final String expression;
   private final CompiledExpression compiledExpression;
   private final int resultChannel;
   private final int[] channels;
   private final NavigableSet<String> unavailableVariables = new TreeSet<>();

   ExpressionApplier(String expression, int[] kHzs, int resultChannel) throws CompileException {
      this.expression = expression;
      this.resultChannel = resultChannel;
      List<Variable> variables = findVariables(kHzs);
      channels = variables.stream()
            .mapToInt(Variable::channel)
            .distinct()
            .sorted()
            .toArray();
      compiledExpression = compileExpression(expression, variables, channels);
   }

   @Override
   public String toString() {
      return expression;
   }

   NavigableSet<String> getUnavailableVariables() {
      return unavailableVariables;
   }

   private List<Variable> findVariables(int[] kHzs) {
      Map<Integer, Integer> kHzToChannel = HashMap.newHashMap(kHzs.length);
      for (int i = 0; i < kHzs.length; i++) {
         kHzToChannel.put(kHzs[i], i + 1);
      }

      List<Variable> variables = new ArrayList<>();
      Set<String> uniqueNames = new HashSet<>();

      Pattern pattern = Pattern.compile("\\bC\\d+\\b");
      Matcher matcher = pattern.matcher(expression);
      while (matcher.find()) {
         String name = matcher.group();
         if (!uniqueNames.add(name)) {
            continue;
         }
         int channel;
         try {
            channel = Integer.parseInt(name.substring(1));
         } catch (NumberFormatException _) {
            continue;
         }
         if (channel == 0) {
            continue;
         }
         if (channel > kHzs.length) {
            unavailableVariables.add(name);
         }
         variables.add(new Variable(name, "C", channel, channel));
      }

      pattern = Pattern.compile("\\bF\\d+\\b");
      matcher = pattern.matcher(expression);
      while (matcher.find()) {
         String name = matcher.group();
         if (!uniqueNames.add(name)) {
            continue;
         }
         int kHz;
         try {
            kHz = Integer.parseInt(name.substring(1));
         } catch (NumberFormatException _) {
            continue;
         }
         Integer channel = kHzToChannel.get(kHz);
         if (channel == null) {
            unavailableVariables.add(name);
            channel = 0;
         }
         variables.add(new Variable(name, "F", kHz, channel));
      }

      variables.sort(Comparator.comparing(Variable::prefix).thenComparingInt(Variable::suffix));
      return variables;
   }

   private static CompiledExpression compileExpression(String expression, List<Variable> variables, int[] channels) throws CompileException {
      String resampledArray = ResampledFloatArray.class.getName();

      StringBuilder source = new StringBuilder(""
            + "package no.marec.compile;\n"
            + "import static java.lang.Math.*;\n"
            + "public final class CompiledExpressionImpl implements " + CompiledExpression.class.getName() + " {\n"
            + "   @Override\n"
            + "   public void run(float[] result, " + resampledArray + "[] resampledArrays) {\n");
      for (int channel : channels) {
         source.append("      " + resampledArray + " arr" + channel + " = resampledArrays[" + channel + "];\n");
      }
      source.append("      for (int i = 0; i < result.length; i++) {\n");
      for (Variable variable : variables) {
         source.append("         double " + variable.name() + " = arr" + variable.channel() + ".getValueForReferenceIndex(i);\n");
      }
      source.append(""
            + "         result[i] = (float) eval(");
      for (int i = 0; i < variables.size(); i++) {
         if (i > 0) {
            source.append(", ");
         }
         source.append(variables.get(i).name());
      }
      source.append(");\n"
            + "      }\n" // end for
            + "   }\n" // end method run
            + "   private static double eval(");
      for (int i = 0; i < variables.size(); i++) {
         if (i > 0) {
            source.append(", ");
         }
         source.append("double " + variables.get(i).name());
      }
      source.append(") {\n"
            + "      return " + expression + ";\n"
            + "   }\n" // end method eval
            + "}\n"); // end class

      return CompilerClassLoader.instantiate(CompiledExpression.class, "no.marec.compile.CompiledExpressionImpl", source.toString());
   }

   @Nullable PowerData apply(Ping ping) {
      PowerData resultRaw;
      ResampledFloatArray[] resampledArrays = new ResampledFloatArray[resultChannel];

      if (channels.length == 0) {
         PowerData sourceRaw = ping.getFirstAvailablePowerData();
         if (sourceRaw == null) {
            return null;
         }
         resultRaw = sourceRaw.makeCopyWithNoData();
         resultRaw.setChannel(resultChannel);
      } else {
         float minDepth = Float.NEGATIVE_INFINITY;
         float maxDepth = Float.POSITIVE_INFINITY;
         for (int channel : channels) {
            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null) {
               return null;
            }
            minDepth = Math.max(minDepth, powerData.getMinDepth());
            maxDepth = Math.min(maxDepth, powerData.getMaxDepth());
         }
         if (minDepth > maxDepth) {
            return null;
         }

         PowerData sourceRaw = ping.getPowerData(getChannelToCopyFrom());
         if (sourceRaw == null) {
            return null;
         }
         resultRaw = sourceRaw.makeCopyWithNoData();
         resultRaw.setChannel(resultChannel);
         resultRaw.setRange(minDepth, maxDepth);

         for (int channel : channels) {
            PowerData powerData = ping.getPowerData(channel);
            if (powerData == null) {
               return null;
            }
            resampledArrays[channel] = ResampledFloatArray.create(powerData.getSv(), powerData, resultRaw);
         }
      }

      compiledExpression.run(resultRaw.getSv(), resampledArrays);

      if (MathUtils.avoidInfinities(resultRaw.getSv())) {
         Log.global.warning("ExpressionApplier : Result contains infinite values. Values will be clamped to min/max float value.");
      }

      return resultRaw;
   }

   int getChannelToCopyFrom() {
      return channels.length == 0 ? 1 : channels[0];
   }

   private record Variable(String name, String prefix, int suffix, int channel) {
   }
}
