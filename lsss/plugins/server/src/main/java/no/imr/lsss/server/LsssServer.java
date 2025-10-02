package no.imr.lsss.server;

import no.imr.lsss.LSSS;
import no.imr.lsss.server.jaxrs.JaxRsApplication;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.logging.Log;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Level;

final class LsssServer {
   private final JaxRsApplication jaxRsApplication;
   private final HttpServer httpServer;

   LsssServer(LSSS lsss, int port) {
      URI uri = URI.create("http://localhost:" + port + "/");
      Log.global.info("Starting LSSS server: " + uri);
      jaxRsApplication = new JaxRsApplication(lsss);
      httpServer = GrizzlyHttpServerFactory.createHttpServer(uri, jaxRsApplication.getResourceConfig(), false);
      httpServer.getServerConfiguration().setName("LsssServer-" + port);
      httpServer.getListeners().forEach(listener -> {
         TCPNIOTransport transport = listener.getTransport();
         transport.setSelectorRunnersCount(1);
         transport.setKernelThreadPoolConfig(ThreadPoolConfig.defaultConfig()
               .setPoolName("LsssServer-kernel-" + port)
               .setCorePoolSize(1)
               .setMaxPoolSize(1));
         transport.setWorkerThreadPool(Exec.CACHED_THREAD_POOL);
      });
      try {
         httpServer.start();
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error starting LSSS server", e);
      }
   }

   void close() {
      httpServer.shutdownNow();
      jaxRsApplication.close();
   }
}
