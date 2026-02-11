package no.imr.lsss.viewer;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.html.HtmlEscapers;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.SurveyIndexFile;
import no.imr.lsss.framework.SurveyManager;
import no.imr.lsss.framework.config.application.DirectoryConf;
import no.imr.lsss.framework.config.application.SurveyDirStructure;
import no.imr.lsss.framework.config.survey.SurveyConfigurationXml;
import no.imr.lsss.framework.config.survey.data.DataConfLSSS;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.TextFilter;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.ListListModel;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.PreferredSizeLayout;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.StatusView;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * Helper class for {@link SurveyManager}.
 */
final class SurveyList {
   private final LSSS lsss;
   private final Consumer<Path> openAction;
   private final SurveyListModel listModel = new SurveyListModel();
   private final JList<Path> list = new JList<>(listModel);
   private final JPanel panel = new JPanel(new BorderLayout());
   private final JLabel infoLabel = new JLabel("Searching...");
   private final @Nullable Path lastSurveyFile;
   private volatile boolean cancelled;
   private boolean selectionInited;
   private int longestFileNameLength;
   private @Nullable Path mouseOverDir;
   private final JTextField filterTextField = new JTextField("", 20);
   private TextFilter filter = new TextFilter("");
   private final LoadingCache<Path, SurveyConfigurationXml> surveyConfigurationXmlCache = CacheBuilder.newBuilder()
         .build(CacheLoader.from(this::createSurveyConfigurationXml));

   SurveyList(LSSS lsss, @Nullable Path lastSurveyFile, Consumer<Path> openAction, Runnable cancelAction) {
      this.lsss = lsss;
      this.lastSurveyFile = lastSurveyFile;
      this.openAction = openAction;

      infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

      JButton openButton = new JButton("Open");
      openButton.setEnabled(false);
      GuiUtils.setAccelerator(openButton, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
      openButton.addActionListener(_ -> openSelectedFile());

      JButton scanButton = new JButton("Scan...");
      scanButton.setToolTipText("Scan a directory for survey files");
      scanButton.addActionListener(_ -> scan());

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> cancelAction.run());

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      buttonPanel.add(openButton);
      buttonPanel.add(scanButton);
      buttonPanel.add(cancelButton);

      filterTextField.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> {
         filter = new TextFilter(filterTextField.getText());
         listModel.updateContent();
      }));
      filterTextField.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && filterTextField.isShowing()) {
            filterTextField.requestFocusInWindow();
         }
      });
      filterTextField.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_UP,
                    KeyEvent.VK_DOWN,
                    KeyEvent.VK_PAGE_UP,
                    KeyEvent.VK_PAGE_DOWN -> {
                  list.dispatchEvent(e);
               }
               default -> {
               }
            }
         }
      });
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE, () -> {
         if (filterTextField.getText().isEmpty()) {
            cancelAction.run();
         } else {
            filterTextField.setText("");
            filterTextField.requestFocusInWindow();
         }
      });

      JLabel filterLabel = new JLabel("Filter: ");
      filterLabel.setToolTipText("<html>Displays only matching surveys<br>Prefix a word with - to exclude");
      filterLabel.setDisplayedMnemonic(KeyEvent.VK_F);
      GuiUtils.setAccelerator(filterLabel, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.ALT_DOWN_MASK), filterTextField::requestFocusInWindow);

      JPanel filterPanel = new JPanel(new GridBagLayout());
      GridBagConstraints filterPanelConstraints = new GridBagConstraints();
      filterPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
      filterPanelConstraints.weightx = 1;
      filterPanel.add(filterLabel);
      filterPanel.add(filterTextField, filterPanelConstraints);

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.add(infoLabel, BorderLayout.WEST);
      bottomPanel.add(PreferredSizeLayout.wrap(filterPanel, PreferredSizeLayout.HorizontalAlignment.CENTER));
      bottomPanel.add(buttonPanel, BorderLayout.EAST);

      list.setFocusable(false);
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      list.setCellRenderer(new SurveyListCellRenderer());
      list.addListSelectionListener(_ -> {
         selectionInited = true;
         Path selectedFile = getSelectedFile();
         openButton.setEnabled(selectedFile != null);
      });
      list.addMouseListener(new MouseAdapter() {
         @Override
         public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() > 1) {
               openSelectedFile();
            } else {
               filterTextField.requestFocusInWindow();
            }
         }

         @Override
         public void mouseExited(MouseEvent e) {
            setMouseOverDir(null);
         }
      });
      list.addMouseListener(new PopupMenuMouseListener(e -> {
         Path file = GuiUtils.pointToListItem(list, e.getPoint());
         if (file == null) {
            return null;
         }
         JPopupMenu menu = new JPopupMenu();
         menu.add(MenuItems.showInFileExplorer(file));
         return menu;
      }));
      list.addMouseMotionListener(new MouseMotionAdapter() {
         @Override
         public void mouseMoved(MouseEvent e) {
            Path file = GuiUtils.pointToListItem(list, e.getPoint());
            String toolTipText = file != null ? createToolTipText(file) : null;
            list.setToolTipText(toolTipText);
            if (file != null) {
               setMouseOverDir(file.getParent());
            }
         }
      });

      panel.add(new JScrollPane(list));
      panel.add(bottomPanel, BorderLayout.SOUTH);
   }

   private String createToolTipText(Path file) {
      SurveyConfigurationXml xml = surveyConfigurationXmlCache.getUnchecked(file);
      return createToolTipText(file, xml);
   }

   static String createToolTipText(Path file, SurveyConfigurationXml xml) {
      return "<html>"
            + "<table border=1 cellspacing=0>"
            + "<tr><td>File</td><td>" + htmlEscape(file.toString()) + "</td></tr>"
            + "<tr><td>Nation</td><td>" + htmlEscape(xml.getNation()) + "</td></tr>"
            + "<tr><td>Platform</td><td>" + htmlEscape(xml.getPlatform()) + "</td></tr>"
            + "<tr><td>Survey</td><td>" + htmlEscape(xml.getSurveyTitle()) + "</td></tr>"
            + "<tr><td>Start</td><td>" + htmlEscape(xml.getStartDate()) + "&nbsp;&nbsp;" + htmlEscape(xml.getStartTime()) + "</td></tr>"
            + "<tr><td>Stop</td><td>" + htmlEscape(xml.getStopDate()) + "&nbsp;&nbsp;" + htmlEscape(xml.getStopTime()) + "</td></tr>"
            + "<tr><td>Boundaries</td><td>"
            /**/ + "North: " + htmlEscape(xml.getBoundaryNorth()) + "&nbsp;, &nbsp;"
            /**/ + "South: " + htmlEscape(xml.getBoundarySouth()) + "&nbsp;, &nbsp;"
            /**/ + "West: " + htmlEscape(xml.getBoundaryWest()) + "&nbsp;, &nbsp;"
            /**/ + "East: " + htmlEscape(xml.getBoundaryEast()) + "</td></tr>"
            + "<tr><td>Comment</td><td>" + htmlEscape(xml.getSurveyDescription()).replaceAll("\\n", "<br>") + "</td></tr>"
            + "</table>";
   }

   private void setMouseOverDir(@Nullable Path dir) {
      mouseOverDir = dir;
      list.repaint();
   }

   private @Nullable Path getSelectedFile() {
      return list.getSelectedValue();
   }

   private void openSelectedFile() {
      Path selectedFile = getSelectedFile();
      if (selectedFile != null) {
         openAction.accept(selectedFile);
      }
   }

   JComponent getComponent() {
      return panel;
   }

   void execute() {
      Exec.CACHED_THREAD_POOL.execute(this::run);
   }

   void cancel() {
      cancelled = true;
   }

   private void run() {
      listAllSurveys();
      SwingUtilities.invokeLater(() -> infoLabel.setText("Found " + listModel.getSize() + " surveys"));
   }

   private void listAllSurveys() {
      DirectoryConf directoryConf = lsss.getConfigurationManager().getApplicationConfiguration().getDirectoryConf();
      List<SurveyDirStructure> surveyDirStructures = directoryConf.getAllSurveyDirStructures();
      Set<Path> lsssDataDirs = new HashSet<>();
      Path mainDir = directoryConf.mainDir.getFile();
      if (mainDir != null) {
         lsssDataDirs.add(mainDir);
      }
      for (Path root : SurveyIndexFile.getRoots()) {
         try {
            for (Path dir : FileUtils.listFiles(root)) {
               if (dir.getFileName().toString().startsWith(SurveyManager.LSSS_DATA_DIR_NAME)) {
                  lsssDataDirs.add(dir);
               }
            }
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error listing files in " + root, e);
         }
      }
      for (Path lsssDataDir : lsssDataDirs) {
         try {
            listLsssDataDirSurveys(lsssDataDir, surveyDirStructures);
         } catch (IOException e) {
            Log.global.log(Level.WARNING, "Error listing surveys in " + lsssDataDir, e);
         }
      }
   }

   private void listSurveyFilesInDirectory(Path dir, SurveyIndexFile surveyIndexFile) throws IOException {
      for (Path file : FileUtils.listFiles(dir)) {
         if (cancelled) {
            return;
         }
         if (file.toString().endsWith(SurveyManager.SURVEY_FILE_SUFFIX)) {
            publish(file);
            surveyIndexFile.add(file);
         }
      }
   }

   private void listLsssDataDirSurveys(Path dir, List<SurveyDirStructure> surveyDirStructures) throws IOException {
      SurveyIndexFile surveyIndexFile = new SurveyIndexFile(dir);
      List<Path> nonExisting = new ArrayList<>();
      for (Path file : surveyIndexFile.getSurveyFiles()) {
         if (cancelled) {
            return;
         }
         if (Files.exists(file)) {
            publish(file);
         } else {
            // Cannot remove while iterating
            nonExisting.add(file);
         }
      }
      nonExisting.forEach(surveyIndexFile::remove);

      for (Path file : FileUtils.listFiles(dir)) {
         for (SurveyDirStructure surveyDirStructure : surveyDirStructures) {
            if (cancelled) {
               return;
            }
            listSurveyFilesInDirectory(file.resolve(surveyDirStructure.getRelativePath(DataConfLSSS.LSSS_SUB_DIR)), surveyIndexFile);
         }
      }

      surveyIndexFile.saveIfChanged();
   }

   private void scan() {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Select directory to scan");
      fileChooser.setCurrentDirectory(FileUtils.listExistingRoots().getFirst().toFile());
      fileChooser.setApproveButtonText("Scan");
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      int returnValue = fileChooser.showOpenDialog(panel);
      if (returnValue != JFileChooser.APPROVE_OPTION) {
         return;
      }
      Path dir = fileChooser.getSelectedFile().toPath();

      Map<Path, SurveyIndexFile> rootToSurveyIndexFile = new HashMap<>();
      try {
         List<Path> roots = SurveyIndexFile.getRoots();
         Path mainRoot = SurveyIndexFile.findMostSpecificRootForFile(dir, roots);
         if (mainRoot == null) {
            lsss.showError(panel, "Cannot find which directory root corresponds to\n" + dir);
            return;
         }
         for (Path root : roots) {
            if (root.equals(mainRoot) || FileUtils.isInDir(root, dir)) {
               rootToSurveyIndexFile.put(root, new SurveyIndexFile(root.resolve(SurveyManager.LSSS_DATA_DIR_NAME)));
            }
         }
      } catch (IOException e) {
         lsss.showError(panel, "Error preparing scan of " + dir, e);
         return;
      }

      StringBuilder message = new StringBuilder();
      StatusView statusView = new StatusView("Scanning " + dir);
      new WorkerDialog(panel, statusView.getComponent())
            .setCancelText("Stop")
            .setMinimumSize(new Dimension(400, 0))
            .start(asyncHandle -> {
               AtomicInteger foundCount = new AtomicInteger();
               Files.walkFileTree(dir, new SimpleFileVisitor<>() {
                  private @Nullable SurveyIndexFile currentSurveyIndexFile;

                  @Override
                  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                     if (asyncHandle.isCancelled()) {
                        return FileVisitResult.TERMINATE;
                     }
                     statusView.setSecondaryText(dir.toString());
                     currentSurveyIndexFile = rootToSurveyIndexFile.get(SurveyIndexFile.findMostSpecificRootForFile(dir, rootToSurveyIndexFile.keySet()));
                     return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                     if (asyncHandle.isCancelled()) {
                        return FileVisitResult.TERMINATE;
                     }
                     if (currentSurveyIndexFile == null) {
                        return FileVisitResult.CONTINUE;
                     }
                     if (file.toString().endsWith(SurveyManager.SURVEY_FILE_SUFFIX)) {
                        publish(file);
                        currentSurveyIndexFile.add(file);
                        foundCount.incrementAndGet();
                     }
                     return FileVisitResult.CONTINUE;
                  }

                  @Override
                  public FileVisitResult visitFileFailed(Path file, IOException exc) {
                     return FileVisitResult.CONTINUE;
                  }
               });

               statusView.setSecondaryText("Saving changes");
               int changeCount = 0;
               List<Path> updatedFiles = new ArrayList<>();
               for (SurveyIndexFile surveyIndexFile : rootToSurveyIndexFile.values()) {
                  if (surveyIndexFile.getChangeCount() > 0) {
                     changeCount += surveyIndexFile.getChangeCount();
                     updatedFiles.add(surveyIndexFile.getIndexFile());
                     surveyIndexFile.saveIfChanged();
                  }
               }
               message.append("Found " + foundCount.get() + " survey file" + (changeCount == 1 ? "" : "s") + " (" + changeCount + " new)");
               for (Path updatedFile : updatedFiles) {
                  message.append("\nUpdated survey index file: ").append(updatedFile);
               }
            });
      JOptionPane.showMessageDialog(panel, message.toString());
   }

   private void publish(Path file) {
      longestFileNameLength = Math.max(longestFileNameLength, file.getFileName().toString().length());
      SwingUtilities.invokeLater(() -> listModel.add(file));
   }

   private static String htmlEscape(@Nullable String string) {
      return HtmlEscapers.htmlEscaper().escape(string != null ? string : "<Not available>");
   }

   private SurveyConfigurationXml createSurveyConfigurationXml(Path file) {
      try {
         Document document = XmlUtils.readDocument(file);
         listModel.updateContentLater();
         return new SurveyConfigurationXml(document);
      } catch (IOException _) {
         return new SurveyConfigurationXml(DocumentHelper.createDocument());
      }
   }

   private final class SurveyListModel extends ListListModel<Path> {
      private final NavigableSet<Path> filesAsSet = new TreeSet<>(Comparator.comparing(Path::getFileName).thenComparing(Function.identity()));

      private SurveyListModel() {
         super(new ArrayList<>());
      }

      private void add(Path file) {
         if (!filesAsSet.add(file)) {
            return;
         }
         updateContentLater();
      }

      private void updateContentLater() {
         SwingDelayer.invokeLater(this, this::updateContent);
      }

      private void updateContent() {
         Path selectedFile = getSelectedFile();
         getItems().clear();
         filesAsSet.stream()
               .filter(this::passesFilter)
               .forEach(getItems()::add);
         list.clearSelection();
         fireContentsChanged(this, 0, getSize());
         if (!selectionInited) {
            int i = getItems().indexOf(lastSurveyFile);
            if (i != -1) {
               list.setSelectedIndex(i);
            }
         } else if (selectedFile != null) {
            list.setSelectedIndex(getItems().indexOf(selectedFile));
         }
      }

      private boolean passesFilter(Path file) {
         List<String> texts = new ArrayList<>();
         texts.add(file.toString());

         SurveyConfigurationXml surveyConfigurationXml = surveyConfigurationXmlCache.getIfPresent(file);
         if (surveyConfigurationXml != null) {
            String surveyTitle = surveyConfigurationXml.getSurveyTitle();
            if (surveyTitle != null) {
               texts.add(surveyTitle);
            }
            String surveyDescription = surveyConfigurationXml.getSurveyDescription();
            if (surveyDescription != null) {
               texts.add(surveyDescription);
            }
         }

         return filter.test(texts);
      }
   }

   private final class SurveyListCellRenderer extends DefaultListCellRenderer {
      private SurveyListCellRenderer() {
      }

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
         Path file = (Path) value;

         String surveyTitle;
         String surveyDescription;
         SurveyConfigurationXml surveyConfigurationXml = surveyConfigurationXmlCache.getIfPresent(file);
         if (surveyConfigurationXml == null) {
            Exec.FORK_JOIN_POOL.execute(() -> surveyConfigurationXmlCache.getUnchecked(file));
            surveyTitle = "";
            surveyDescription = "<Loading...>";
         } else {
            surveyTitle = surveyConfigurationXml.getSurveyTitle();
            surveyDescription = surveyConfigurationXml.getSurveyDescription();
         }

         String fileName = file.getFileName().toString();
         String fileNamePrefix = "&nbsp;".repeat(longestFileNameLength - fileName.length());

         String highlightStyle = mouseOverDir != null && file.startsWith(mouseOverDir)
               ? "color: red;"
               : "";

         String text = "<html><span style='font-family:monospace;" + highlightStyle + "'>" + fileNamePrefix + htmlEscape(fileName) + "</span>"
               + " &nbsp; &nbsp; <span style='color:black;'>" + htmlEscape(surveyTitle) + "</span>"
               + " &nbsp; &nbsp; <span style='color:gray;'>" + htmlEscape(surveyDescription) + "</span>";
         return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
      }
   }
}
