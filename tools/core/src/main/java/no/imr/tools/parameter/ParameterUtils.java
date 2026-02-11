package no.imr.tools.parameter;

import java.util.Collection;
import java.util.Iterator;

public final class ParameterUtils {
   private ParameterUtils() {
   }

   public static void copyValues(Collection<? extends BaseParameter<?>> parameters1, Collection<? extends BaseParameter<?>> parameters2) {
      Iterator<? extends BaseParameter<?>> it1 = parameters1.iterator();
      Iterator<? extends BaseParameter<?>> it2 = parameters2.iterator();
      while (it1.hasNext() && it2.hasNext()) {
         BaseParameter<?> parameter1 = it1.next();
         BaseParameter<?> parameter2 = it2.next();
         copyValue(parameter1, parameter2);
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> void copyValue(BaseParameter<T> parameter1, BaseParameter<?> parameter2) {
      switch (parameter1) {
         case BaseValueParameter<T> baseValueParameter1 -> {
            baseValueParameter1.setValue(((BaseValueParameter<T>) parameter2).getValue());
         }
         case MultiParameter<?> multiParameter1 -> {
            copyValues(multiParameter1.getParameters(), ((MultiParameter<?>) parameter2).getParameters());
         }
         case VoidParameter _ -> {
         }
      }
   }

   public static boolean equals(Collection<? extends BaseParameter<?>> parameters1, Collection<? extends BaseParameter<?>> parameters2) {
      if (parameters1.size() != parameters2.size()) {
         return false;
      }
      Iterator<? extends BaseParameter<?>> it1 = parameters1.iterator();
      Iterator<? extends BaseParameter<?>> it2 = parameters2.iterator();
      while (it1.hasNext()) {
         BaseParameter<?> parameter1 = it1.next();
         BaseParameter<?> parameter2 = it2.next();
         if (!equals(parameter1, parameter2)) {
            return false;
         }
      }
      return true;
   }

   public static boolean equals(BaseParameter<?> parameter1, BaseParameter<?> parameter2) {
      return switch (parameter1) {
         case BaseValueParameter<?> baseValueParameter1 -> {
            yield parameter2 instanceof BaseValueParameter<?> baseValueParameter2 &&
                  baseValueParameter1.getValue().equals(baseValueParameter2.getValue());
         }
         case MultiParameter<?> multiParameter1 -> {
            yield parameter2 instanceof MultiParameter<?> multiParameter2 &&
                  equals(multiParameter1.getParameters(), multiParameter2.getParameters());
         }
         case VoidParameter _ -> {
            yield parameter2 instanceof VoidParameter;
         }
      };
   }

   public static int hashCode(Collection<? extends BaseParameter<?>> parameters) {
      int result = 1;
      for (BaseParameter<?> parameter : parameters) {
         result = 31 * result + hashCode(parameter);
      }
      return result;
   }

   public static int hashCode(BaseParameter<?> parameter) {
      return switch (parameter) {
         case BaseValueParameter<?> baseValueParameter -> baseValueParameter.getValue().hashCode();
         case MultiParameter<?> multiParameter -> hashCode(multiParameter.getParameters());
         case VoidParameter _ -> 1;
      };
   }
}
