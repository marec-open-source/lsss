package no.imr.tools.help.pojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple version of no.marec.tools.help.server.pojo.HelpSet.
 */
public final class SimplePojoHelpSet {
   public List<String> pageIds = new ArrayList<>();
   public Map<String, Object> aliases = new HashMap<>();

   public SimplePojoHelpSet() {
   }
}
