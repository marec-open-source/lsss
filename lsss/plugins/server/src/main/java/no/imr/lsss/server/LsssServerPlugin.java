package no.imr.lsss.server;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.LsssServerConf;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.listening.Listener;
import no.imr.tools.listening.Listeners;
import org.jspecify.annotations.Nullable;

final class LsssServerPlugin extends FeaturePlugin {
   private @Nullable LsssServer server;

   LsssServerPlugin(LsssServerService service, LSSS lsss) {
      super(service, lsss);
   }

   @Override
   public void setup() {
      LsssServerConf lsssServerConf = getLSSS().getConfigurationManager().getAppMiscConf().getLsssServerConf();
      lsssServerConf.setLsssServerPluginEnabled(true);
      Listener restartListener = Listeners.coalescingInExecutor(Exec.CACHED_THREAD_POOL, () -> restartServer(lsssServerConf));
      restartListener.addTo(
            lsssServerConf.serverActive,
            lsssServerConf.serverPort
      );
      restartListener.addTo(lsssServerConf.getLsssServerSettings().getParameters());
   }

   private synchronized void restartServer(LsssServerConf lsssServerConf) {
      close();
      if (lsssServerConf.serverActive.getBooleanValue()) {
         server = new LsssServer(getLSSS(), lsssServerConf.serverPort.getIntValue());
      }
   }

   @Override
   public synchronized void close() {
      if (server != null) {
         server.close();
         server = null;
      }
   }
}
