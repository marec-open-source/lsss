package no.imr.korona.plugins;

import no.imr.korona.data.datagrams.DatagramType;
import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.tools.parameter.Name;
import no.imr.tools.plugins.BasePlugin;

import java.util.List;

/**
 * Base class for plugins defining new datagram types.
 */
public abstract class DatagramPlugin extends BasePlugin {
   protected DatagramPlugin(Name name) {
      super(name);
   }

   public abstract List<DatagramType> getDatagramTypes();

   public abstract List<DatagramSubType> getSubDatagramTypes();
}
