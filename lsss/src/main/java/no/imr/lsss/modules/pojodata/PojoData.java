package no.imr.lsss.modules.pojodata;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import no.imr.korona.data.ping.PingIndex;
import no.imr.korona.data.ping.PingMapping;
import no.imr.lsss.modules.pojodata.pojo.PojoDataInfo;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ParameterExport;
import org.jfree.chart.plot.XYPlot;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

public final class PojoData {
   private final Map<String, Object> properties = new LinkedHashMap<>();

   private PojoData() {
   }

   @JsonAnyGetter
   public Map<String, Object> getProperties() {
      return properties;
   }

   public static PojoData empty() {
      return new PojoData();
   }

   public static Builder newBuilder(String id) {
      return newBuilder(new PojoDataInfo(id));
   }

   public static Builder newBuilder(PojoDataInfo info) {
      return new Builder(info)
            .with("info", info);
   }

   public static final class Builder {
      private final PojoData pojoData = new PojoData();
      private final PojoDataInfo info;

      private Builder(PojoDataInfo info) {
         this.info = info;
      }

      public PojoDataInfo getInfo() {
         return info;
      }

      public Builder newBuilder() {
         return new Builder(info);
      }

      public Builder withThisBuilder(Consumer<Builder> consumer) {
         consumer.accept(this);
         return this;
      }

      public Builder with(String name, Object value) {
         pojoData.properties.put(name, value);
         return this;
      }

      public Builder with(String name, Unit unit, Object value) {
         if (!unit.equals(Unit.NONE)) {
            String previousUnit = info.units.put(name, unit.formalName());
            if (previousUnit != null && !previousUnit.equals(unit.formalName())) {
               throw new IllegalArgumentException("Conflicting units for " + name + ": " + previousUnit + " != " + unit.formalName());
            }
         }
         pojoData.properties.put(name, value);
         return this;
      }

      public Builder with(ParameterExport parameterExport, DoubleStream values) {
         return with(parameterExport, values.map(parameterExport.transform()).toArray());
      }

      public Builder with(ParameterExport parameterExport, double[] values) {
         return with(parameterExport.name(), parameterExport.unit(), values);
      }

      public Builder with(PingMapping pingMapping, Stream<PingIndex> pingIndexes) {
         return with(PojoDataUtils.getParameterExport(pingMapping), pingIndexes.mapToDouble(pingMapping::valueOf));
      }

      public Builder with(PojoData data) {
         pojoData.properties.putAll(data.properties);
         return this;
      }

      public Builder withCoordinateVariable(ParameterExport... parameterExports) {
         return with("coordinateVariable", Stream.of(parameterExports).map(ParameterExport::name).toList());
      }

      public Builder withDataVariable(ParameterExport parameterExport) {
         return with("dataVariable", parameterExport.name());
      }

      public Builder withDatasets(XYPlot plot) {
         List<PojoData> datasets = PojoDataUtils.toPojoDatasets(this, plot);
         return with(PojoDataUtils.DATASETS, datasets);
      }

      public Builder withDatasets(Stream<XYPlot> plots) {
         List<PojoData> datasets = plots
               .flatMap(plot -> PojoDataUtils.toPojoDatasets(this, plot).stream())
               .toList();
         return with(PojoDataUtils.DATASETS, datasets);
      }

      public PojoData build() {
         return pojoData;
      }
   }
}
