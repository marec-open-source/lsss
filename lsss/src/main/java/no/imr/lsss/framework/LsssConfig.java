package no.imr.lsss.framework;

import com.google.common.util.concurrent.Runnables;
import org.dom4j.Document;
import org.jspecify.annotations.Nullable;

public final class LsssConfig {
   public final ServiceCollection serviceCollection;
   public boolean visible = true;
   public boolean loadSettings = true;
   public @Nullable Document applicationXml;
   public boolean isPrimaryLSSS = true;
   public Runnable onClose = Runnables.doNothing();
   public String preferencesNode = "/no/marec/lsss";
   public int serverPort = 8000;

   public LsssConfig(ServiceCollection serviceCollection) {
      this.serviceCollection = serviceCollection;
   }

   public LsssConfig invisible() {
      visible = false;
      return this;
   }

   public LsssConfig skipLoadSetting() {
      loadSettings = false;
      return this;
   }
}
