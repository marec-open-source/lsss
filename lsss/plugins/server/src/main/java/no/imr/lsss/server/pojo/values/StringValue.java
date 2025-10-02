package no.imr.lsss.server.pojo.values;

import no.imr.tools.annotations.ReflectionEntryPoint;

public final class StringValue {
   public String value = "";

   @ReflectionEntryPoint
   public StringValue() {
   }

   public StringValue(String value) {
      this.value = value;
   }

   @Override
   public String toString() {
      return "StringValue{" +
            "value='" + value + '\'' +
            '}';
   }
}
