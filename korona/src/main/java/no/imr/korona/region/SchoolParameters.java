package no.imr.korona.region;

import com.google.common.collect.ImmutableMap;
import org.jspecify.annotations.Nullable;

/**
 * School parameters as an immutable value.
 */
public final class SchoolParameters {
   static final SchoolParameters EMPTY = new SchoolParameters(false, false, ImmutableMap.of(), ImmutableMap.of());

   private final boolean upToDate;
   private final boolean dataProcessed;
   private final ImmutableMap<String, Float> values;
   private final ImmutableMap<Integer, ImmutableMap<String, Float>> perChannelValues;

   private SchoolParameters(boolean upToDate, boolean dataProcessed, ImmutableMap<String, Float> values, ImmutableMap<Integer, ImmutableMap<String, Float>> perChannelValues) {
      this.upToDate = upToDate;
      this.dataProcessed = dataProcessed;
      this.values = values;
      this.perChannelValues = perChannelValues;
   }

   public static SchoolParameters of(boolean dataProcessed, ImmutableMap<String, Float> values, ImmutableMap<Integer, ImmutableMap<String, Float>> perChannelValues) {
      return new SchoolParameters(true, dataProcessed, values, perChannelValues);
   }

   public boolean isUpToDate() {
      return upToDate;
   }

   public boolean isDataProcessed() {
      return dataProcessed;
   }

   public ImmutableMap<String, Float> getValues() {
      return values;
   }

   public ImmutableMap<Integer, ImmutableMap<String, Float>> getPerChannelValues() {
      return perChannelValues;
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof SchoolParameters that
            && upToDate == that.upToDate
            && dataProcessed == that.dataProcessed
            && values.equals(that.values)
            && perChannelValues.equals(that.perChannelValues);
   }

   @Override
   public int hashCode() {
      int result = Boolean.hashCode(upToDate);
      result = 31 * result + Boolean.hashCode(dataProcessed);
      result = 31 * result + values.hashCode();
      result = 31 * result + perChannelValues.hashCode();
      return result;
   }
}
