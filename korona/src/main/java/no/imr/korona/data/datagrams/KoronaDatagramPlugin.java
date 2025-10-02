package no.imr.korona.data.datagrams;

import no.imr.korona.data.datagrams.subdatagrams.DataIncoherenceSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.data.datagrams.subdatagrams.config.ExtraRawFileConfiguration;
import no.imr.korona.data.datagrams.subdatagrams.echoline.EchoLineSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.GraphicalInfoSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.graphical.GraphicalInfoTocSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterConfigSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.plot.PlotParameterValueSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.range.BottomRangesSubDatagram;
import no.imr.korona.data.datagrams.subdatagrams.ts.TsDatagram;
import no.imr.korona.plugins.DatagramPlugin;
import no.imr.tools.parameter.Name;

import java.util.List;

/**
 * The datagram types in KORONA.
 */
public final class KoronaDatagramPlugin extends DatagramPlugin {
   public static final DatagramType CAS0 = new DatagramType.Simple("CAS0", Cas0Datagram::new);
   public static final DatagramType CAT0 = new DatagramType.Simple("CAT0", Cat0Datagram::new);

   KoronaDatagramPlugin(Name name) {
      super(name);
   }

   @Override
   public List<DatagramType> getDatagramTypes() {
      return List.of(
            Bot0Datagram.TYPE,
            Cac0Datagram.TYPE,
            Cad0Datagram.TYPE,
            CAS0,
            CAT0,
            Cds0Datagram.TYPE,
            Con0Datagram.TYPE,
            Con1Datagram.TYPE,
            Dep0Datagram.TYPE,
            Eop0Datagram.TYPE,
            Fil0Datagram.TYPE,
            Fil1Datagram.TYPE,
            Idx0Datagram.TYPE,
            LsssDatagram.TYPE,
            Mru0Datagram.TYPE,
            Mru1Datagram.TYPE,
            Nme0Datagram.TYPE,
            Nqp0Datagram.TYPE,
            Pco0Datagram.TYPE_PCO0,
            Pco1Datagram.TYPE_PCO1,
            Phy0Datagram.TYPE,
            Pic0Datagram.TYPE,
            Pid0Datagram.TYPE,
            Pin0Datagram.TYPE_PIN0,
            Pin1Datagram.TYPE_PIN1,
            Raw0Datagram.TYPE,
            Raw1Datagram.TYPE,
            Raw2Datagram.TYPE,
            Raw3Datagram.TYPE,
            Raw4Datagram.TYPE,
            RegionBorderDatagram.TYPE,
            RegionInfoDatagram.TYPE,
            RegionTableOfContentsDatagram.TYPE,
            Sen0Datagram.TYPE,
            Sin0Datagram.TYPE,
            Tag0Datagram.TYPE,
            TBR0Datagram.TYPE,
            TNF0Datagram.TYPE,
            TTC0Datagram.TYPE,
            Ver0Datagram.TYPE,
            Xml0Datagram.TYPE
      );
   }

   @Override
   public List<DatagramSubType> getSubDatagramTypes() {
      return List.of(
            DataIncoherenceSubDatagram.SUB_TYPE,
            EchoLineSubDatagram.SUB_TYPE,
            ExtraRawFileConfiguration.SUB_TYPE,
            GraphicalInfoSubDatagram.SUB_TYPE,
            GraphicalInfoTocSubDatagram.SUB_TYPE,
            PlotParameterConfigSubDatagram.SUB_TYPE,
            PlotParameterValueSubDatagram.SUB_TYPE,
            BottomRangesSubDatagram.SUB_TYPE,
            TsDatagram.SUB_TYPE
      );
   }
}
