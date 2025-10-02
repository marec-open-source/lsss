package no.imr.tools.parameter;

import com.google.common.collect.ImmutableMap;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;

/**
 * A parameter consisting of several (sub)parameters.
 */
public non-sealed class MultiParameter<T extends ValueParameter<?>> extends BaseParameter<Object> {
   private ImmutableMap<String, T> nameToParameter = ImmutableMap.of(); // Preserves ordering.

   public MultiParameter(Name name) {
      this(name, "");
   }

   public MultiParameter(Name name, String description) {
      super(name, Unit.NONE, description);
   }

   @Override
   Object getValue() {
      return Optional.empty();
   }

   @Override
   public Collection<T> getSubConfigurables() {
      return getParameters();
   }

   /**
    * Returns all (sub)parameters in this MultiParameter.
    *
    * @return all (sub)parameters in this MultiParameter
    */
   public Collection<T> getParameters() {
      return nameToParameter.values();
   }

   /**
    * Returns a named (sub)parameter in this MultiParameter.
    *
    * @param name the name of the (sub)parameter
    * @return the (sub)parameter, or {@code null} if not found
    */
   public @Nullable T getParameter(String name) {
      return nameToParameter.get(name);
   }

   /**
    * Adds a (sub)parameter to this MultiParameter.
    *
    * @param parameter the (sub)parameter
    * @return the newly added parameter
    */
   public <P extends T> P addParameter(P parameter) {
      T old = nameToParameter.get(parameter.getPersistentName());
      if (old != null) {
         old.getChangeManager().removeListener(getChangeManager());
         Log.global.warning(getPersistentName() + ": Duplicate parameter: " + parameter.getPersistentName());
      }
      nameToParameter = ImmutableUtils.put(nameToParameter, parameter.getPersistentName(), parameter);
      parameter.getChangeManager().addListener(getChangeManager());
      notifyListeners();
      return parameter;
   }

   /**
    * Removes all (sub)parameters from this MultiParameter.
    */
   public void clear() {
      nameToParameter.values().forEach(parameter -> parameter.getChangeManager().removeListener(getChangeManager()));
      nameToParameter = ImmutableMap.of();
      notifyListeners();
   }
}
