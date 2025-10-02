package no.imr.korona.apps.fiskview;

import no.imr.korona.Korona;
import no.imr.korona.computation.ModuleContainer;
import no.imr.korona.config.ConfigFileService;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.KoronaSettingsUtils;
import no.imr.korona.resources.KoronaResource;
import no.imr.korona.util.KoronaPreferences;
import no.imr.korona.util.KoronaUtils;
import no.imr.korona.viewer.KoronaHelpSystem;
import no.imr.korona.viewer.KoronaPlaybox;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpSystem;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.swing.GeometryListener;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Main class for display.
 */
public final class FiskView {
   private static final Dimension FRAME_SIZE = new Dimension(900, 600);

   private final Korona korona = new Korona();
   private final HelpSystem koronaHelpSystem = KoronaHelpSystem.createHelpSystem(korona);
   private final KoronaPlaybox koronaPlaybox = new KoronaPlaybox(korona);
   private final ModuleContainer moduleContainer = koronaPlaybox.getModuleContainer();
   private final ConfigFileSettings configFileSettings = moduleContainer.getConfigFileSettings();

   private final JFrame frame = new JFrame();

   private FiskView() {
      koronaPlaybox.getUpdateChangeManager().addListener(this::updateFrameTitle);
      updateFrameTitle();

      FiskViewMenu fiskViewMenu = new FiskViewMenu(this);
      JMenuBar menuBar = fiskViewMenu.getMenuBar();
      menuBar.add(Box.createHorizontalStrut(6));
      menuBar.add(koronaPlaybox.createButtonsGridBag().getPanel());

      frame.setJMenuBar(menuBar);
      frame.getContentPane().add(koronaPlaybox.getComponent());
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent we) {
            shutDown();
         }
      });
      GeometryListener.startPreferenceSyncing(frame, FRAME_SIZE, null, KoronaPreferences.node("fiskView"), "windowGeometry");
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.setIconImage(KoronaResource.KORONA_32);
      frame.setVisible(true);
   }

   private void loadSettings() {
      Path lastCfsFile = KoronaPlaybox.getLastCfsFile();
      if (lastCfsFile != null && Files.exists(lastCfsFile)) {
         koronaPlaybox.loadCfsFile(lastCfsFile);
      }
   }

   public static void main(String[] args) {
      long t0 = System.currentTimeMillis();

      Utils.init(args, KoronaResource.KORONA_64);
      if (Utils.isTestRun()) {
         throw new UnsupportedOperationException();
      }

      SwingUtilities.invokeLater(() -> {
         FiskView fiskView = new FiskView();
         boolean newDir = KoronaSettingsUtils.showMainConfigDirDialogIfNecessary(fiskView.korona, fiskView.frame, Path.of(""));
         Path configDir = fiskView.korona.getKoronaSettings().getKoronaConfigDir().getFile();
         if (newDir && configDir != null) {
            ConfigFileSettings configFileSettings = fiskView.korona.createConfigFileSettings();
            for (ConfigFileService configFileService : configFileSettings.getFileServices()) {
               FileParameter fileParameter = configFileSettings.getFileParameter(configFileService.getName());
               fileParameter.setFile(configFileService.getDefaultInConfigDirectory(configDir));
            }

            Path cfsFile = configDir.resolve(ConfigFileSettings.DEFAULT_FILE_NAME);
            try {
               configFileSettings.save(cfsFile);
            } catch (IOException e) {
               GuiUtils.showErrorDialog(fiskView.frame, "Error saving " + cfsFile, e);
            }
            fiskView.koronaPlaybox.loadCfsFile(cfsFile);
         } else {
            fiskView.loadSettings();
         }
         fiskView.parseArgs(args);

         long t1 = System.currentTimeMillis();
         Log.global.info("Startup time: " + (t1 - t0) / 1000f + " sec");
      });
   }

   private void parseArgs(String[] args) {
      for (String arg : args) {
         Path file = Path.of(arg);

         if (korona.getDataFormatManager().createSegmentHandle(file) != null) {
            koronaPlaybox.loadRawFile(file);
         } else if (arg.equals("--open-last")) {
            Path lastRawFile = KoronaPlaybox.getLastRawFile();
            if (lastRawFile != null) {
               koronaPlaybox.loadRawFile(lastRawFile);
            }
            koronaPlaybox.start();
         } else {
            Log.global.info("Unused command line argument: " + arg);
         }
      }
   }

   Korona getKorona() {
      return korona;
   }

   HelpSystem getKoronaHelpSystem() {
      return koronaHelpSystem;
   }

   private @Nullable Path getCfsFile() {
      return configFileSettings.getFile();
   }

   private FileParameter getCdsFileParameter() {
      return configFileSettings.getModuleConfigurationFileParameter();
   }

   private @Nullable Path getCdsFile() {
      return getCdsFileParameter().getFile();
   }

   private void setCdsFile(Path cdsFile) {
      getCdsFileParameter().setFile(cdsFile);
   }

   ModuleContainer getModuleContainer() {
      return moduleContainer;
   }

   JFrame getFrame() {
      return frame;
   }

   private void updateFrameTitle() {
      String title = "KORONA " + Korona.VERSION;
      Path cfsFile = getCfsFile();
      if (cfsFile != null) {
         String cfsInfo = cfsFile.getFileName().toString();
         Path cdsFile = getCdsFile();
         if (cdsFile != null) {
            cfsInfo += " [" + cdsFile.getFileName() + "]";
         }
         title = cfsInfo + " - " + title;
      }
      Path rawFile = koronaPlaybox.getRawFile();
      if (rawFile != null) {
         title = rawFile + " - " + title;
      }
      frame.setTitle(title);
   }

   void removeConfiguration() {
      koronaPlaybox.stop();

      moduleContainer.clear();

      koronaPlaybox.reset();
   }

   private JFileChooser createRawFileChooser() {
      JFileChooser fileChooser = new JFileChooser();
      Path rawFile = koronaPlaybox.getRawFile();
      fileChooser.setSelectedFile(FileUtils.toFile(rawFile != null ? rawFile : KoronaPlaybox.getLastRawFile()));
      korona.getDataFormatManager().installFileFilters(fileChooser);
      return fileChooser;
   }

   private JFileChooser createCdsFileChooser() {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setSelectedFile(FileUtils.toFile(getCdsFile() != null ? getCdsFile() : KoronaPlaybox.getLastCdsFile()));
      fileChooser.setFileFilter(new SuffixFileFilter(KoronaUtils.CDS_FILE_TYPE));
      return fileChooser;
   }

   private JFileChooser createCfsFileChooser() {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setSelectedFile(FileUtils.toFile(getCfsFile() != null ? getCfsFile() : KoronaPlaybox.getLastCfsFile()));
      fileChooser.setFileFilter(new SuffixFileFilter(ConfigFileSettings.FILE_TYPE));
      return fileChooser;
   }

   void shutDown() {
      if (isUnmodifiedOrUserApproved()) {
         koronaHelpSystem.close();
         koronaPlaybox.close();
         frame.dispose();
         System.exit(0);
      }
   }

   void loadRawFile() {
      boolean running = koronaPlaybox.isRunning();
      koronaPlaybox.stop();

      JFileChooser fileChooser = createRawFileChooser();
      int returnVal = fileChooser.showOpenDialog(frame);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         koronaPlaybox.loadRawFile(fileChooser.getSelectedFile().toPath());
      }

      if (running) {
         koronaPlaybox.start();
      }
   }

   void loadCdsFile() {
      koronaPlaybox.stop();

      JFileChooser fileChooser = createCdsFileChooser();
      int returnVal = fileChooser.showOpenDialog(frame);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         koronaPlaybox.loadCdsFile(fileChooser.getSelectedFile().toPath());
      }
   }

   void loadCfsFile() {
      koronaPlaybox.stop();

      JFileChooser fileChooser = createCfsFileChooser();
      int returnVal = fileChooser.showOpenDialog(frame);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         koronaPlaybox.loadCfsFile(fileChooser.getSelectedFile().toPath());
      }
   }

   boolean saveCdsFile() {
      Path cdsFile = getCdsFile();
      if (cdsFile == null) {
         return saveCdsFileAs();
      }
      try {
         moduleContainer.writeConfiguration(cdsFile);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(frame, "Error saving " + cdsFile);
         return false;
      }
      return true;
   }

   boolean saveCdsFileAs() {
      JFileChooser fileChooser = createCdsFileChooser();
      int returnVal = fileChooser.showSaveDialog(frame);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         setCdsFile(KoronaUtils.CDS_FILE_TYPE.ensureSuffix(fileChooser.getSelectedFile().toPath()));
         return saveCdsFile();
      } else {
         return false;
      }
   }

   boolean saveCfsFile() {
      Path cfsFile = getCfsFile();
      if (cfsFile == null) {
         return saveCfsFileAs();
      }
      try {
         configFileSettings.save(cfsFile);
      } catch (IOException e) {
         GuiUtils.showErrorDialog(frame, "Error saving " + cfsFile);
         return false;
      }
      KoronaPlaybox.setLastCfsFile(cfsFile);
      return true;
   }

   boolean saveCfsFileAs() {
      JFileChooser fileChooser = createCfsFileChooser();
      int returnVal = fileChooser.showSaveDialog(frame);

      if (returnVal == JFileChooser.APPROVE_OPTION) {
         configFileSettings.setFile(ConfigFileSettings.FILE_TYPE.ensureSuffix(fileChooser.getSelectedFile().toPath()));
         return saveCfsFile();
      } else {
         return false;
      }
   }

   void saveAll() {
      saveCdsFile();
      saveCfsFile();
   }

   void editCurrentConfiguration() {
      koronaPlaybox.editCurrentConfiguration();
   }

   void createNewConfiguration() {
      koronaPlaybox.stop();
      removeConfiguration();
      editCurrentConfiguration();
   }

   private boolean isUnmodifiedOrUserApproved() {
      return isCdsUnmodifiedOrUserApproved() && isCfsUnmodifiedOrUserApproved();
   }

   private boolean isCfsUnmodifiedOrUserApproved() {
      return isXmlUnmodifiedOrUserApproved(getCfsFile(), configFileSettings.toXml(), korona.createConfigFileSettings().toXml(),
            frame, "Config file settings", this::saveCfsFile);
   }

   boolean isCdsUnmodifiedOrUserApproved() {
      return isXmlUnmodifiedOrUserApproved(getCdsFile(), moduleContainer.toXml(), new ModuleContainer(korona).toXml(),
            frame, "Module configuration", this::saveCdsFile);
   }

   private static boolean isXmlUnmodifiedOrUserApproved(@Nullable Path file, Element currentXml, Element defaultXml,
                                                        Component referenceComponent, String whatIsChanged, Supplier<Boolean> save) {
      if (file == null && XmlUtils.equalContent(currentXml, defaultXml)) {
         return true;
      }
      if (file != null && XmlUtils.equalContent(currentXml, file)) {
         return true;
      }

      StringBuilder message = new StringBuilder()
            .append(whatIsChanged).append(" is modified");
      if (file != null) {
         message.append(":\n").append(file.getFileName());
      }
      message.append("\n\nSave changes?");

      int answer = JOptionPane.showConfirmDialog(referenceComponent, message,
            "Save changes?", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
      return switch (answer) {
         case JOptionPane.YES_OPTION -> save.get();
         case JOptionPane.NO_OPTION -> true;
         default -> false;
      };
   }
}
