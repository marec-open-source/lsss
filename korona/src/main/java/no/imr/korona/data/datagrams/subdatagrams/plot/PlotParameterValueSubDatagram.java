package no.imr.korona.data.datagrams.subdatagrams.plot;

import no.imr.korona.data.datagrams.DatagramFormatException;
import no.imr.korona.data.datagrams.subdatagrams.BaseSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubTypeId;
import no.imr.korona.data.datagrams.subdatagrams.plot.pojo.PlotParameterValues;
import no.imr.korona.data.formats.ek60.io.ByteBufferUtils;
import no.imr.tools.parameter.Name;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class PlotParameterValueSubDatagram extends BaseSubDatagram {
   public static final DatagramSubType SUB_TYPE = new DatagramSubType(DatagramSubTypeId.PLOT_PARAMETER_VALUE,
         "Plot parameter value", PlotParameterValueSubDatagram::new);

   private final PlotParameterValues plotParameterValues;

   PlotParameterValueSubDatagram(long ntDate) {
      super(ntDate);

      plotParameterValues = new PlotParameterValues();
   }

   private PlotParameterValueSubDatagram(long ntDate, ByteBuffer byteBuffer) throws DatagramFormatException {
      super(ntDate);

      plotParameterValues = ByteBufferUtils.readJson(byteBuffer, PlotParameterValues.class);
   }

   @Override
   public void write(ByteBuffer byteBuffer) {
      ByteBufferUtils.writeJson(byteBuffer, plotParameterValues);
   }

   @Override
   public DatagramSubType getDatagramSubType() {
      return SUB_TYPE;
   }

   public Map<String, Float> getPerPingParameterValues() {
      return plotParameterValues.perPing;
   }

   public Map<String, Map<Integer, Float>> getPerChannelValues() {
      return plotParameterValues.perChannel;
   }

   public PlotParameterValueSubDatagram putPerPing(Name name, float value) {
      plotParameterValues.perPing.put(name.persistentName(), value);
      return this;
   }

   public PlotParameterValueSubDatagram putPerChannel(Name name, int channel, float value) {
      plotParameterValues.perChannel.computeIfAbsent(name.persistentName(), _ -> new HashMap<>()).put(channel, value);
      return this;
   }
}
