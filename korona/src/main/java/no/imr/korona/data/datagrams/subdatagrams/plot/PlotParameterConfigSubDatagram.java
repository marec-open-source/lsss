package no.imr.korona.data.datagrams.subdatagrams.plot;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.datagrams.subdatagrams.plot.pojo.PlotParameterConfig;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;

import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class PlotParameterConfigSubDatagram extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.PLOT_PARAMETER_CONFIG,
         "Plot parameter config", PlotParameterConfigSubDatagram::new);

   private final List<PlotParameterConfig> plotParameterConfigs = new ArrayList<>();

   public PlotParameterConfigSubDatagram(Instant instant) {
      super(instant);
   }

   private PlotParameterConfigSubDatagram(Instant instant, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(instant);

      Collections.addAll(plotParameterConfigs, ByteBufferUtils.readJson(byteBuffer, PlotParameterConfig[].class));
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeJson(byteBuffer, plotParameterConfigs);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   public List<PlotParameterConfig> getPlotParameterConfigs() {
      return plotParameterConfigs;
   }

   public PlotParameterConfigSubDatagram addConfig(PlotParameterConfig plotParameterConfig) {
      plotParameterConfigs.add(plotParameterConfig);
      return this;
   }
}
