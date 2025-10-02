package no.imr.lsss.server.pojo.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class DebugData {
   public long reloadedPings;
   public long availablePings;
   public long enabledModules;
   public List<String> windows = new ArrayList<>();
   public Map<String, List<String>> overlaysWithData = new TreeMap<>();

   public DebugData() {
   }

   @Override
   public String toString() {
      return "DebugData{" +
            "enabledModules=" + enabledModules +
            ", windows=" + windows +
            '}';
   }
}
