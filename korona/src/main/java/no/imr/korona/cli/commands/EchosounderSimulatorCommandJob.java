package no.imr.korona.cli.commands;

import no.imr.korona.cli.CliCommandJob;
import no.imr.korona.cli.CliException;
import no.imr.korona.util.simulator.EchoSounderSimulator;
import no.imr.korona.util.simulator.EchoSounderSimulatorGUI;
import no.imr.tools.listening.Listener;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

final class EchosounderSimulatorCommandJob extends CliCommandJob {
   private final boolean gui;
   private final @Nullable Path configFile;

   EchosounderSimulatorCommandJob(boolean gui, @Nullable Path configFile) {
      this.gui = gui;
      this.configFile = configFile != null ? configFile.toAbsolutePath().normalize() : null;
   }

   @Override
   public void run(InputStream in, PrintStream out) throws Exception {
      CountDownLatch countDownLatch = new CountDownLatch(1);
      if (gui) {
         SwingUtilities.invokeAndWait(() -> {
            EchoSounderSimulatorGUI echoSounderSimulatorGUI = new EchoSounderSimulatorGUI(EchosounderSimulatorCommand.COMMAND_NAME, null);
            if (configFile != null) {
               echoSounderSimulatorGUI.open(configFile);
            }
            echoSounderSimulatorGUI.getFrame().addWindowListener(new WindowAdapter() {
               @Override
               public void windowClosed(WindowEvent e) {
                  countDownLatch.countDown();
               }
            });
         });
      } else {
         if (configFile == null) {
            throw new CliException("No config file specified");
         }
         EchoSounderSimulator echoSounderSimulator = new EchoSounderSimulator();
         Element element = XmlUtils.readDocument(configFile).getRootElement();
         echoSounderSimulator.fromXml(element);
         echoSounderSimulator.getChangeManager().addListener(new Listener() {
            private @Nullable Path previousOutputFile;

            @Override
            public void listen() {
               Path outputFile = echoSounderSimulator.getCurrentOutputFile();
               if (outputFile != null && !outputFile.equals(previousOutputFile)) {
                  previousOutputFile = outputFile;
                  out.println("Writing to " + outputFile);
               }
            }
         });
         echoSounderSimulator.getRunningChangeManager().addListener(running -> {
            if (!running) {
               countDownLatch.countDown();
            }
         });
         echoSounderSimulator.setRunning(true);
      }
      countDownLatch.await();
   }
}
