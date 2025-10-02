package no.imr.korona.cli.commands.pojo;

import no.imr.korona.Korona;
import no.imr.tools.Utils;

import java.util.ArrayList;
import java.util.List;

public final class KoronaInfo {
   public String version = Korona.VERSION;
   public String buildTime = Utils.BUILD_TIME.toString();
   public String gitCommit = Utils.GIT_COMMIT;
   public List<String> activePlugins = new ArrayList<>();
   public List<KoronaModuleInfo> modules = new ArrayList<>();

   public KoronaInfo() {
   }
}
