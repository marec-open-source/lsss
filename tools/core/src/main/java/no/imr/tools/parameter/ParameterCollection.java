package no.imr.tools.parameter;

import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * A configurable based on {@link ParameterContainer}.
 */
public final class ParameterCollection extends Configurable {
   public static final String XML_PARAMETERS = "parameters";

   private final ParameterContainer parameterContainer;

   public ParameterCollection(ParameterContainer parameterContainer) {
      super(new Name(XML_PARAMETERS));

      this.parameterContainer = parameterContainer;
   }

   public ParameterCollection(List<? extends BaseParameter<?>> parameters) {
      this(() -> parameters);
   }

   @Override
   protected @Nullable Configurable possiblyCreateNewSubConfigurable(String persistentName) {
      return parameterContainer.possiblyCreateNewParameter(persistentName);
   }

   @Override
   public boolean handleUnknownXml(String name, Element subElement) {
      return parameterContainer.handleUnknownXml(name, subElement);
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return getParameters();
   }

   public List<? extends BaseParameter<?>> getParameters() {
      return parameterContainer.getParameters();
   }
}
