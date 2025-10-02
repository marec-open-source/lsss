package no.imr.korona.computation.feature;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.UncheckedExecutionException;
import no.imr.tools.compile.CompileException;
import no.imr.tools.compile.CompilerClassLoader;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FeatureRequirementFactory {
   private static final LoadingCache<String, FeatureRequirement> CACHE = CacheBuilder.newBuilder()
         .maximumSize(100)
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(new CacheLoader<>() {
            @Override
            public FeatureRequirement load(String key) throws CompileException {
               return compile(key);
            }
         });

   private FeatureRequirementFactory() {
   }

   public static FeatureRequirement create(String expression) throws CompileException {
      if (expression.isBlank()) {
         return FeatureRequirement.ALWAYS_TRUE;
      }

      try {
         return CACHE.get(expression);
      } catch (ExecutionException e) {
         Throwable cause = e.getCause();
         switch (cause) {
            case CompileException compileException -> throw compileException;
            case RuntimeException runtimeException -> throw runtimeException;
            case null, default -> throw new UncheckedExecutionException(cause);
         }
      }
   }

   private static FeatureRequirement compile(String expression) throws CompileException {
      StringBuilder source = new StringBuilder(""
            + "package no.marec.compile;\n"
            + "import java.util.Set;\n"
            + "public final class FeatureRequirementImpl implements " + FeatureRequirement.class.getName() + " {\n"
            + "   @Override\n"
            + "   public boolean isValid(Set<String> featureNames) {\n");

      for (String featureName : getFeatureNames(expression)) {
         source.append("      boolean " + featureName + " = featureNames.contains(\"" + featureName + "\");\n");
      }

      source.append(""
            + "      return " + expression + ";\n"
            + "   }\n" // end method
            + "}\n"); // end class

      return CompilerClassLoader.instantiate(FeatureRequirement.class, "no.marec.compile.FeatureRequirementImpl", source.toString());
   }

   private static Set<String> getFeatureNames(String expression) {
      Set<String> featureNames = new HashSet<>();

      Pattern pattern = Pattern.compile(getRegex());
      Matcher matcher = pattern.matcher(expression);
      while (matcher.find()) {
         String featureName = matcher.group();
         featureNames.add(featureName);
      }

      return featureNames;
   }

   private static String getRegex() {
      StringBuilder regex = new StringBuilder("\\b(" + FeatureExtractor.FREQUENCY_FEATURE_PREFIX + "\\d+");
      for (String additionalFeature : FeatureExtractor.ALL_ADDITIONAL_FEATURES) {
         regex.append('|').append(additionalFeature);
      }
      regex.append(")\\b");
      return regex.toString();
   }
}
