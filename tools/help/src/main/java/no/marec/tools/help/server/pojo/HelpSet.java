package no.marec.tools.help.server.pojo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class HelpSet {
   public String id = "";
   public String version = "";
   public String buildTime = "";
   public String name = "";
   public String icon = "";
   public String path = "";
   public List<String> pageIds = new ArrayList<>(); // Also defined in SimplePojoHelpSet
   public List<String> pageHrefs = new ArrayList<>();
   public List<String> pageTitles = new ArrayList<>();
   public Map<String, List<Object>> aliases = new LinkedHashMap<>(); // List = [ pageIndex, anchor ] // Also defined in SimplePojoHelpSet
   public List<TocItem> toc = new ArrayList<>();

   public HelpSet() {
   }
}
