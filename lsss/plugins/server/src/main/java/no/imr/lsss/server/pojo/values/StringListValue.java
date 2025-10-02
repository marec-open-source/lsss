package no.imr.lsss.server.pojo.values;

import no.imr.tools.annotations.ReflectionEntryPoint;

import java.util.ArrayList;
import java.util.List;

public final class StringListValue {
   public List<String> value = new ArrayList<>();

   @ReflectionEntryPoint
   public StringListValue() {
   }

   @Override
   public String toString() {
      return "StringListValue{" +
            "value=" + value +
            '}';
   }
}
