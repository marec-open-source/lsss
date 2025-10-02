package no.imr.tools.parameter;

import no.marec.lsss.api.util.parameters.ValueStringifier;

public interface ValueConverter<T> extends ValueStringifier<T> {
   /**
    * Converts a string to a value.
    *
    * @param string a string
    * @return a value
    */
   T parse(String string) throws Exception;

   @Override
   String stringify(T value);
}
