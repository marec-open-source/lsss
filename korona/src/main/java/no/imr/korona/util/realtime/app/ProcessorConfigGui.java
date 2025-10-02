package no.imr.korona.util.realtime.app;

import no.imr.korona.Korona;
import no.imr.tools.io.DirectoryWatcher;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.VerticalScrollablePanel;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.HierarchyEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;

public final class ProcessorConfigGui {
   private final Path configFile;
   private final ProcessorConfig processorConfig;
   private final JPanel panel = new JPanel(new BorderLayout());
   private @Nullable DirectoryWatcher directoryWatcher;

   public ProcessorConfigGui(Path configFile, Korona korona) {
      this.configFile = configFile;
      processorConfig = new ProcessorConfig(korona);

      ParameterEditor parameterEditor = new ParameterEditor(processorConfig.getParameters());
      VerticalScrollablePanel parameterPanel = VerticalScrollablePanel.wrap(parameterEditor.getEditorComponent());
      parameterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      panel.add(new JScrollPane(parameterPanel));

      panel.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            update();
         }
      });
   }

   public Component getComponent() {
      return panel;
   }

   public void save() {
      try {
         processorConfig.save(configFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error saving " + configFile, e);
      }
   }

   private void update() {
      if (panel.isShowing()) {
         startWatching();
      } else {
         stopWatching();
      }
   }

   private void startWatching() {
      stopWatching();

      loadConfig();
      directoryWatcher = DirectoryWatcher.forFile(configFile, () -> {
         SwingUtilities.invokeLater(this::loadConfig);
      });
   }

   private void stopWatching() {
      if (directoryWatcher != null) {
         directoryWatcher.close();
      }
   }

   private void loadConfig() {
      Log.global.info("Loading " + configFile);
      try {
         processorConfig.load(configFile);
      } catch (IOException e) {
         Log.global.log(Level.WARNING, "Error loading " + configFile, e);
      }
   }
}
