package no.imr.tools.parameter;

import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * An object containing parameters.
 * Used by {@link ParameterCollection}.
 */
@FunctionalInterface
public interface ParameterContainer {
   List<? extends BaseParameter<?>> getParameters();

   default boolean handleUnknownXml(String name, Element subElement) {
      return false;
   }

   default @Nullable BaseParameter<?> possiblyCreateNewParameter(String persistentName) {
      return null;
   }
}
