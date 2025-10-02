package no.imr.korona.data.datagrams;

import no.imr.korona.data.datagrams.subdatagrams.DatagramSubType;
import no.imr.korona.plugins.DatagramPlugin;
import no.imr.korona.plugins.DatagramService;
import no.imr.tools.logging.Log;
import no.imr.tools.plugins.BaseService;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Locates all instances of {@link DatagramPlugin}.
 */
public final class DatagramTypeManager {
   private final Map<Integer, DatagramType> intCodeToDatagramType = new HashMap<>();
   private final Map<Integer, DatagramSubType> intCodeToDatagramSubType = new HashMap<>();

   public DatagramTypeManager() {
      BaseService.getUsableServices(DatagramService.class)
            .map(DatagramService::createPlugin)
            .forEach(plugin -> {
               addDatagramTypes(plugin.getDatagramTypes());
               addDatagramSubTypes(plugin.getSubDatagramTypes());
            });
   }

   private void addDatagramTypes(List<DatagramType> datagramTypes) {
      for (DatagramType datagramType : datagramTypes) {
         DatagramType old = intCodeToDatagramType.put(datagramType.getIntCode(), datagramType);
         if (old != null) {
            Log.global.warning("Removed previously registered datagram type: " + datagramType);
         }
      }
   }

   private void addDatagramSubTypes(List<DatagramSubType> datagramSubTypes) {
      for (DatagramSubType datagramSubType : datagramSubTypes) {
         DatagramSubType old = intCodeToDatagramSubType.put(datagramSubType.getIntCode(), datagramSubType);
         if (old != null) {
            Log.global.warning("Removed previously registered sub-datagram type: " + datagramSubType);
         }
      }
   }

   public @Nullable DatagramType getDatagramType(int intCode) {
      return intCodeToDatagramType.get(intCode);
   }

   public Collection<DatagramType> getDatagramTypes() {
      return Collections.unmodifiableCollection(intCodeToDatagramType.values());
   }

   public @Nullable DatagramSubType getDatagramSubType(int subTypeId) {
      return intCodeToDatagramSubType.get(subTypeId);
   }

   public Collection<DatagramSubType> getDatagramSubTypes() {
      return Collections.unmodifiableCollection(intCodeToDatagramSubType.values());
   }
}
