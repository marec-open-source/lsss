package no.imr.korona.util.simulator;

import no.imr.korona.util.KoronaPreferences;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GeometryListener;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.prefs.Preferences;

/**
 * GUI for {@link EchoSounderSimulator}.
 */
public final class EchoSounderSimulatorGUI {
   private static final String PREFERENCE_SETUP = "setup";
   private static final String PREFERENCE_CONFIG_DIR = "configDir";

   private final JFrame frame = new JFrame();
   private final Preferences preferences;
   private final EchoSounderSimulator echoSounderSimulator = new EchoSounderSimulator();

   private @Nullable Path configFile;

   public EchoSounderSimulatorGUI(String preferencesKey, @Nullable Path defaultOutputDir) {
      preferences = KoronaPreferences.node("simulator").node(preferencesKey);
      String setup = preferences.get(PREFERENCE_SETUP, null);
      if (setup != null) {
         try {
            new ParameterCollection(echoSounderSimulator).fromXml(XmlUtils.readDocument(setup).getRootElement());
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error loading saved setup", e);
         }
      }
      if (echoSounderSimulator.output.getFile() == null) {
         echoSounderSimulator.output.setFile(defaultOutputDir);
      }

      ParameterEditor parameterEditor = new ParameterEditor(echoSounderSimulator.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);

      Listener.of(this::savePreferences).addTo(echoSounderSimulator.getParameters());

      JTextArea textArea = new JTextArea(3, 0);
      textArea.setEditable(false);
      textArea.setBorder(BorderFactory.createEtchedBorder());
      echoSounderSimulator.getChangeManager().addListener(GuiListeners.coalescingLater(() -> {
         Path inputFile = echoSounderSimulator.getCurrentInputFile();
         Path outputFile = echoSounderSimulator.getCurrentOutputFile();
         if (inputFile == null || outputFile == null) {
            textArea.setText("");
            return;
         }
         String text = "Input: " + inputFile
               + "\nOutput: " + outputFile.getFileName() + " (" + echoSounderSimulator.getFileCounter() + ")"
               + "\n" + Utils.getByteSizeString(FileUtils.sizeOr0(outputFile)) + " ; " + echoSounderSimulator.getPingCounter() + " pings"
               + " ; Ping number: " + echoSounderSimulator.getPingNumber();
         textArea.setText(text);
      }));

      JPanel playControlPanel = new JPanel(new FlowLayout());
      JButton playButton = MiscIcons.PLAY.on(new JButton());
      playControlPanel.add(playButton);
      playButton.addActionListener(_ -> echoSounderSimulator.setRunning(!echoSounderSimulator.isRunning()));
      echoSounderSimulator.getRunningChangeManager().addListener(GuiListeners.coalescingLater(() -> {
         SvgIcon icon = echoSounderSimulator.isRunning() ? MiscIcons.PAUSE : MiscIcons.PLAY;
         icon.on(playButton);
      }));

      JPanel upperPanel = new JPanel(new BorderLayout());
      VerticalScrollablePanel parameterPanel = VerticalScrollablePanel.wrap(parameterEditor.getEditorComponent());
      parameterPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
      upperPanel.add(new JScrollPane(parameterPanel));
      upperPanel.add(playControlPanel, BorderLayout.SOUTH);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(upperPanel);
      mainPanel.add(textArea, BorderLayout.SOUTH);

      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      frame.setJMenuBar(createMenuBar());
      frame.getContentPane().add(mainPanel);
      frame.pack();
      GeometryListener.startPreferenceSyncing(frame, null, new Point(0, 0), preferences, "windowGeometry");
      frame.setVisible(true);

      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            close();
         }
      });

      updateFrameTitle();
   }

   private void close() {
      echoSounderSimulator.setRunning(false);
      savePreferences();
      frame.dispose();
   }

   public JFrame getFrame() {
      return frame;
   }

   private JMenuBar createMenuBar() {
      JMenuBar menuBar = new JMenuBar();

      JMenu fileMenu = menuBar.add(new JMenu("File"));

      GuiUtils.autoCreateContentMenu(fileMenu, () -> {
         JMenuItem openItem = MiscIcons.OPEN.on(fileMenu.add("Open config file..."));
         openItem.setToolTipText("Open an echosounder simulator config file");
         openItem.setMnemonic(KeyEvent.VK_O);
         openItem.addActionListener(_ -> open());

         JMenuItem saveItem = MiscIcons.SAVE.on(fileMenu.add("Save config file"));
         saveItem.setToolTipText(configFile != null
               ? "Save current configuration to " + configFile
               : "Save current configuration");
         saveItem.setMnemonic(KeyEvent.VK_S);
         saveItem.addActionListener(_ -> save());

         JMenuItem closeItem = fileMenu.add("Close config file");
         if (configFile != null) {
            closeItem.setToolTipText("Close config file " + configFile);
         } else {
            closeItem.setEnabled(false);
         }
         closeItem.setMnemonic(KeyEvent.VK_C);
         closeItem.addActionListener(_ -> setConfigFile(null));

         fileMenu.addSeparator();

         JMenuItem exitItem = MiscIcons.POWER.on(fileMenu.add("Exit"));
         exitItem.setMnemonic(KeyEvent.VK_X);
         exitItem.addActionListener(_ -> close());
      });

      return menuBar;
   }

   private void setConfigFile(@Nullable Path file) {
      configFile = file != null ? file.toAbsolutePath().normalize() : null;
      if (configFile != null) {
         Path dir = configFile.getParent();
         if (dir != null) {
            preferences.put(PREFERENCE_CONFIG_DIR, dir.toString());
         }
      }
      updateFrameTitle();
   }

   private void updateFrameTitle() {
      String title = "EchoSounderSimulator";
      if (configFile != null) {
         title = title + " - [" + configFile + "]";
      }
      frame.setTitle(title);
   }

   private JFileChooser createFileChooser() {
      JFileChooser fileChooser = new JFileChooser();
      String configDirPref = preferences.get(PREFERENCE_CONFIG_DIR, null);
      if (configDirPref != null) {
         fileChooser.setCurrentDirectory(Path.of(configDirPref).toFile());
      }
      fileChooser.setFileFilter(new SuffixFileFilter("Echosounder config file", ".xml"));
      return fileChooser;
   }

   private void open() {
      JFileChooser fileChooser = createFileChooser();
      fileChooser.setSelectedFile(configFile != null ? configFile.toFile() : null);
      int returnVal = fileChooser.showOpenDialog(frame);
      if (returnVal != JFileChooser.APPROVE_OPTION) {
         return;
      }
      open(fileChooser.getSelectedFile().toPath());
   }

   public void open(Path file) {
      setConfigFile(file);
      Element element;
      try {
         element = XmlUtils.readDocument(file).getRootElement();
      } catch (IOException e) {
         GuiUtils.showErrorDialog(frame, "Error reading " + file, e);
         return;
      }
      echoSounderSimulator.fromXml(element);
   }

   private void save() {
      if (configFile == null) {
         JFileChooser fileChooser = createFileChooser();
         int returnVal = fileChooser.showSaveDialog(frame);
         if (returnVal != JFileChooser.APPROVE_OPTION) {
            return;
         }
         Path selectedFile = fileChooser.getSelectedFile().toPath();
         selectedFile = FileUtils.ensureSuffix(selectedFile, ".xml");
         setConfigFile(selectedFile);
      }
      try {
         XmlUtils.writeDocument(echoSounderSimulator.toXml(), configFile);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(frame, "Error writing " + configFile, e);
      }
   }

   private void savePreferences() {
      preferences.put(PREFERENCE_SETUP, XmlUtils.toCompactString(new ParameterCollection(echoSounderSimulator).toXml()));
   }

   public static void start(String preferencesKey, Path defaultOutputDir) {
      SwingUtilities.invokeLater(() -> {
         new EchoSounderSimulatorGUI(preferencesKey, defaultOutputDir);
      });
   }
}
