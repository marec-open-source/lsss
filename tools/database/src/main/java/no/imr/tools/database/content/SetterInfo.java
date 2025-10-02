package no.imr.tools.database.content;

import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Function;

final class SetterInfo {
   private final Method setter;
   private @Nullable Function<String, Object> parameterParser;

   SetterInfo(Method setter) {
      this.setter = setter;
   }

   private Function<String, Object> getParameterParser() {
      if (parameterParser == null) {
         parameterParser = findParameterParser(setter.getParameterTypes()[0]);
      }
      return parameterParser;
   }

   private static Function<String, Object> findParameterParser(Class<?> clazz) {
      if (String.class == clazz) {
         return value -> value;
      } else if (int.class == clazz) {
         return Integer::valueOf;
      } else if (short.class == clazz) {
         return Short::valueOf;
      } else {
         throw new IllegalArgumentException(clazz.toString());
      }
   }

   void callSet(Object object, String value) throws ReflectiveOperationException {
      Object parameter = getParameterParser().apply(value);
      setter.invoke(object, parameter);
   }
}
