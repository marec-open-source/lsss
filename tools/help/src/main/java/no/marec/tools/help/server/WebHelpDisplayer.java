package no.marec.tools.help.server;

import no.imr.tools.concurrent.Exec;
import no.imr.tools.help.HelpDisplayer;
import no.imr.tools.help.HelpID;
import no.imr.tools.help.HelpSystemInfo;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.GuiUtils;
import no.marec.tools.help.server.jaxrs.JaxRsApplication;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.nio.transport.TCPNIOTransport;
import org.glassfish.grizzly.threadpool.ThreadPoolConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.List;

public final class WebHelpDisplayer implements HelpDisplayer {
   private final String url;
   private final HttpServer httpServer;

   public WebHelpDisplayer(List<String> helpDirs, List<ClassLoader> classLoaders, HelpSystemInfo helpSystemInfo) throws IOException {
      int port = getActualPort(helpSystemInfo.port());
      String serverUrl = "http://localhost:" + port + "/";
      JaxRsApplication jaxRsApplication = new JaxRsApplication(helpDirs, classLoaders, helpSystemInfo);
      httpServer = GrizzlyHttpServerFactory.createHttpServer(URI.create(serverUrl), jaxRsApplication.getResourceConfig(), false);
      httpServer.getServerConfiguration().setName("HelpServer-" + port);
      httpServer.getListeners().forEach(listener -> {
         TCPNIOTransport transport = listener.getTransport();
         transport.setSelectorRunnersCount(1);
         transport.setKernelThreadPoolConfig(ThreadPoolConfig.defaultConfig()
               .setPoolName("HelpServer-kernel-" + port)
               .setCorePoolSize(1)
               .setMaxPoolSize(1));
         transport.setWorkerThreadPool(Exec.CACHED_THREAD_POOL);
      });
      httpServer.start();
      url = serverUrl + "help/" + helpSystemInfo.app() + "/" + helpSystemInfo.version() + "/";
      Log.global.info("Web help server started at " + url);
   }

   private static int getActualPort(int preferredPort) throws IOException {
      if (preferredPort > 0) {
         for (int port = preferredPort; port <= 65535; port++) {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
               return serverSocket.getLocalPort();
            } catch (IOException e) {
               // Try again.
            }
         }
      }
      try (ServerSocket serverSocket = new ServerSocket(0)) {
         return serverSocket.getLocalPort();
      }
   }

   @Override
   public void close() {
      httpServer.shutdownNow();
   }

   public String getUrl() {
      return url;
   }

   @Override
   public void display(HelpID helpID) {
      StringBuilder pageUrl = new StringBuilder(url)
            .append("#/page/").append(helpID.helpSet().getId());
      if (!helpID.id().isEmpty()) {
         pageUrl.append('/').append(helpID.id());
      }
      GuiUtils.desktopBrowse(URI.create(pageUrl.toString()), null);
   }
}
