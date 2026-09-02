package no.imr.korona.computation;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.html.HtmlEscapers;
import no.imr.korona.computation.misc.CommentModule;
import no.imr.korona.computation.misc.GroupEndModule;
import no.imr.korona.computation.misc.TemporaryComputationsBeginModule;
import no.imr.korona.computation.misc.TemporaryComputationsEndModule;
import no.imr.korona.config.ConfigFileSettings;
import no.imr.korona.config.gui.ConfigFileSettingsEditor;
import no.imr.korona.config.gui.ContextVisibility;
import no.imr.korona.plugins.ModulePlugin;
import no.imr.korona.resources.KoronaHelp;
import no.imr.korona.util.KoronaPreferences;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.Exec;
import no.imr.tools.help.HelpID;
import no.imr.tools.io.FileInfo;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.CurrentInputComponent;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MenuItems;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.swing.WrappingLines;
import no.imr.tools.swing.icons.AbstractIcon;
import no.imr.tools.swing.icons.MiscIcons;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implements a window for editing the contents of a ModuleContainer.
 */
public final class ModuleEditor {
   private static final Dimension BUTTON_DIMENSION = new Dimension(24, 24);

   private static String lastModuleName = "";
   private static int lastModuleIndex;

   private final Element backupCfs;
   private final Element backupCds;
   private final ModuleContainer moduleContainer;
   private final Set<BaseModule> selectedModules = new HashSet<>();
   private final List<ModuleButton> moduleButtons = new ArrayList<>();
   private final Map<BaseModule, ModuleButton> moduleToDisplayedModuleButton = new HashMap<>();
   private @Nullable Drag drag;
   private @Nullable Element modulesClipboard;
   private Set<BaseModule> highlightModules = Set.of();
   private @Nullable CommentModule lastGroupButtonClickModule;
   private Instant lastGroupButtonClickTime = Instant.EPOCH;
   private boolean ok;

   private final boolean editable;
   private ModulePredicate modulePredicate = ModulePredicate.alwaysTrue();
   private Consumer<Boolean> onClose = _ -> {
   };

   private final JDialog dialog;
   private final JPanel glassPane;
   private final WrappingLines wrappingLines = new WrappingLines(3, 3, 0);
   private final JPanel modulePanel = new VerticalScrollablePanel(new BorderLayout());
   private final JPanel lowerPanel = new JPanel(new CardLayout());
   private final JPanel emptySelectionPanel = new JPanel(new BorderLayout());
   private final JPanel singleSelectionPanel = new JPanel(new BorderLayout());
   private final JPanel parameterEditorPanel = new VerticalScrollablePanel(new BorderLayout());
   private final JPanel multiSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
   private final JLabel errorLabel = new JLabel();
   private final JLabel deprecatedLabel = new JLabel("NB: This module is deprecated and will be removed in a future version.");
   private final JLabel betaLabel = new JLabel("NB: This module is in beta and can be incompatibly changed or removed in future versions.");
   private final JLabel descriptionLabel = new JLabel();
   private final JPanel configFileSettingsPanel = new JPanel(new BorderLayout());
   private final JMenuBar menuBar = new JMenuBar();

   private final JButton forwardButton = MiscIcons.STEP_BACK.on(new JButton());
   private final JButton backwardButton = MiscIcons.STEP_FORWARD.on(new JButton());
   private final JButton deleteButton = MiscIcons.DELETE.on(new JButton());

   private final JButton okButton = new JButton("OK");
   private final JButton cancelButton = new JButton("Cancel");
   private final JButton helpButton = new JButton("Help");

   private ParameterEditor parameterEditor = new ParameterEditor(List.of());
   private final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(modulePanel), lowerPanel);
   private List<?> lastDividerInfo = List.of();

   public ModuleEditor(ModuleContainer moduleContainer, boolean editable, @Nullable Component referenceComponent) {
      this.moduleContainer = moduleContainer;
      this.editable = editable;

      backupCfs = moduleContainer.getConfigFileSettings().toXml();
      backupCds = moduleContainer.toXml();

      ModuleUtils.fixAll(moduleContainer.getModuleList());

      List<BaseModule> modules = moduleContainer.getModules();
      if (lastModuleIndex < modules.size() && modules.get(lastModuleIndex).getPersistentName().equals(lastModuleName)) {
         selectedModules.add(modules.get(lastModuleIndex));
      }
      if (selectedModules.isEmpty() && !modules.isEmpty()) {
         selectedModules.add(modules.getFirst());
      }

      Listener rewindListener = this::rewind;
      WhenShowingListening.connect(configFileSettingsPanel, moduleContainer.getConfigFileSettings().getChangeManager(), rewindListener);

      Window window = GuiUtils.windowForComponent(referenceComponent);
      dialog = new JDialog(window, makeTitle(), Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.setJMenuBar(menuBar);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            cancel();
         }
      });
      glassPane = (JPanel) dialog.getGlassPane();
   }

   private ModuleManager getModuleManager() {
      return moduleContainer.getKorona().getModuleManager();
   }

   public ModuleEditor setModulePredicate(ModulePredicate modulePredicate) {
      this.modulePredicate = modulePredicate;
      return this;
   }

   public ModuleEditor setOnClose(Consumer<Boolean> onClose) {
      this.onClose = onClose;
      return this;
   }

   public ModuleEditor setModeless() {
      dialog.setModalityType(Dialog.ModalityType.MODELESS);
      return this;
   }

   public ModuleEditor setSelectedModule(BaseModule module) {
      selectedModules.clear();
      selectedModules.add(module);
      return this;
   }

   public ModuleEditor show() {
      makeMenus();
      dialog.getContentPane().add(makeContent());
      rewind();
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.setSize(900, 600);
      GuiUtils.syncSize(dialog, getPreferences(), "dialogSize");
      dialog.setLocationRelativeTo(dialog.getParent());
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);
      return this;
   }

   public boolean getOK() {
      return ok;
   }

   private static Preferences getPreferences() {
      return KoronaPreferences.node("moduleEditor");
   }

   private String makeTitle() {
      String title = "Module editor";
      Path configFile = moduleContainer.getConfigFile();
      if (configFile != null) {
         title = configFile.getFileName() + " - [" + configFile.getParent() + "] - " + title;
      }
      return title;
   }

   private JComponent makeContent() {
      JButton allActiveButton = new JButton("Set all active");
      allActiveButton.addActionListener(_ -> modulesActive(true));
      JButton noneActiveButton = new JButton("Set none active");
      noneActiveButton.addActionListener(_ -> modulesActive(false));
      multiSelectionPanel.add(allActiveButton);
      multiSelectionPanel.add(noneActiveButton);

      Border marginBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
      errorLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 5), marginBorder));
      deprecatedLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.RED, 5), marginBorder));
      betaLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(ColorUtils.GOLD, 5), marginBorder));
      descriptionLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEtchedBorder(), marginBorder));

      GridBag gridBag = new GridBag()
            .configureVerticalBox();
      gridBag.add(errorLabel);
      gridBag.add(deprecatedLabel);
      gridBag.add(betaLabel);
      gridBag.add(configFileSettingsPanel);
      gridBag.add(descriptionLabel);

      singleSelectionPanel.add(gridBag.getPanel(), BorderLayout.NORTH);
      singleSelectionPanel.add(parameterEditorPanel);

      lowerPanel.add(emptySelectionPanel);
      lowerPanel.add(singleSelectionPanel);
      lowerPanel.add(multiSelectionPanel);

      glassPane.setLayout(null);
      glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      new DeepInputListener(glassPane, new MouseAndKeyAdapter() {
         @Override
         public void mouseReleased(MouseEvent e) {
            if (drag != null) {
               drag = null;
               glassPane.setVisible(false);
               glassPane.removeAll();
               rewind();
            }
         }
      });
      modulePanel.setFocusable(true);
      MouseAndKeyAdapter modulePanelListener = new MouseAndKeyAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            modulePanel.requestFocusInWindow();
         }

         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_HOME -> {
                  if (e.getModifiersEx() == 0 && !moduleContainer.getModules().isEmpty()) {
                     selectModules(moduleContainer.getModules().getFirst(), false, false);
                  }
               }
               case KeyEvent.VK_END -> {
                  if (e.getModifiersEx() == 0 && !moduleContainer.getModules().isEmpty()) {
                     selectModules(moduleContainer.getModules().getLast(), false, false);
                  }
               }
               case KeyEvent.VK_LEFT -> {
                  if (e.getModifiersEx() == 0) {
                     stepCurrentModuleHorizontally(-1);
                  }
               }
               case KeyEvent.VK_RIGHT -> {
                  if (e.getModifiersEx() == 0) {
                     stepCurrentModuleHorizontally(1);
                  }
               }
               case KeyEvent.VK_UP -> {
                  if (e.getModifiersEx() == 0) {
                     stepCurrentModuleVertically(true);
                  }
               }
               case KeyEvent.VK_DOWN -> {
                  if (e.getModifiersEx() == 0) {
                     stepCurrentModuleVertically(false);
                  }
               }
               case KeyEvent.VK_DELETE -> {
                  if (e.getModifiersEx() == 0) {
                     deleteSelectedModules();
                  }
               }
               case KeyEvent.VK_C -> {
                  if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                     copySelectedModules();
                  }
               }
               case KeyEvent.VK_V -> {
                  if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                     pasteModulesClipboard();
                  }
               }
               case KeyEvent.VK_X -> {
                  if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                     cutSelectedModules();
                  }
               }
               default -> {
               }
            }
         }
      };
      modulePanel.addMouseListener(modulePanelListener);
      modulePanel.addKeyListener(modulePanelListener);
      modulePanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      modulePanel.add(wrappingLines.getPanel());

      dialog.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            updateDividerLocation();
         }
      });

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(splitPane);
      mainPanel.add(createBottomPanel(), BorderLayout.SOUTH);
      return mainPanel;
   }

   private void updateDividerLocation() {
      Insets insets = modulePanel.getInsets();
      int maxWidth = modulePanel.getParent().getWidth() - (insets.left + insets.right);
      int height = wrappingLines.relayout(maxWidth).height;
      int splitPaneDividerSize = UiUtils.splitPaneDividerSize();
      int dividerLocation = height + insets.top + insets.bottom + splitPaneDividerSize;
      List<?> dividerInfo = List.of(dividerLocation, moduleButtons.size(), dialog.getWidth());
      if (!dividerInfo.equals(lastDividerInfo) || dividerLocation < splitPane.getDividerLocation()) {
         splitPane.setDividerLocation(dividerLocation);
         lastDividerInfo = dividerInfo;
      }
   }

   private void modulesActive(boolean active) {
      for (BaseModule module : selectedModules) {
         module.active.setBooleanValue(active);
      }
      rewind();
   }

   private HelpID getCurrentHelpID() {
      BaseModule currentModule = getCurrentModule();
      HelpID helpID = currentModule != null ? currentModule.getHelpID() : null;
      if (helpID == null || !helpID.isValid()) {
         helpID = getModuleEditorHelpID();
      }
      return helpID;
   }

   private static HelpID getModuleEditorHelpID() {
      return KoronaHelp.MODULE_CONFIGURATION;
   }

   private void makeMenus() {
      for (ModulePlugin modulePlugin : getModuleManager().getModulePlugins()) {
         Node node = new Node();
         for (ModuleInfo moduleInfo : getModuleManager().getModuleInfos(modulePlugin)) {
            if (moduleInfo.moduleClass() == GroupEndModule.class) {
               continue;
            }
            if (moduleInfo.moduleClass() == TemporaryComputationsEndModule.class) {
               continue;
            }
            node.add(moduleInfo);
         }

         JMenu newMenu = node.makeNewMenuHierarchy(modulePlugin.getName().displayName());
         newMenu.setEnabled(editable);

         if (modulePlugin instanceof KoronaModulePlugin && getModuleManager().getModulePlugins().size() == 1) {
            newMenu.setText("New");
            newMenu.setMnemonic(KeyEvent.VK_N);
         }

         JMenu partialSetupsMenu = createPartialSetupsMenu(modulePlugin);
         if (partialSetupsMenu != null) {
            newMenu.add(MenuItems.label(MiscIcons.EMPTY, modulePlugin.getName().displayName() + " modules:", true), 0);
            newMenu.insertSeparator(0);
            newMenu.insert(partialSetupsMenu, 0);
         }

         menuBar.add(newMenu);
      }

      menuBar.add(Box.createVerticalStrut(BUTTON_DIMENSION.height + 2 * 3)); // Also acts as horizontal glue

      menuBar.add(Box.createHorizontalGlue());

      forwardButton.setPreferredSize(BUTTON_DIMENSION);
      forwardButton.setToolTipText("Move selected modules one position forward");
      forwardButton.addActionListener(_ -> moveSelectedModules(-1));
      GuiUtils.setAccelerator(forwardButton, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK));
      menuBar.add(forwardButton);

      menuBar.add(Box.createHorizontalStrut(5));

      backwardButton.setPreferredSize(BUTTON_DIMENSION);
      backwardButton.setToolTipText("Move selected modules one position backward");
      backwardButton.addActionListener(_ -> moveSelectedModules(1));
      GuiUtils.setAccelerator(backwardButton, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK));
      menuBar.add(backwardButton);

      menuBar.add(Box.createHorizontalStrut(25));

      JButton menuBarHelpButton = MiscIcons.HELP.on(new JButton());
      menuBarHelpButton.setPreferredSize(BUTTON_DIMENSION);
      menuBarHelpButton.setToolTipText("Show help for module editor");
      menuBarHelpButton.addActionListener(_ -> getModuleEditorHelpID().show());
      menuBar.add(menuBarHelpButton);

      menuBar.add(Box.createHorizontalStrut(25));

      deleteButton.setPreferredSize(BUTTON_DIMENSION);
      deleteButton.setToolTipText("Delete selected modules");
      deleteButton.addActionListener(_ -> deleteSelectedModules());
      menuBar.add(deleteButton);

      menuBar.add(Box.createHorizontalGlue());
   }

   private @Nullable JMenu createPartialSetupsMenu(ModulePlugin modulePlugin) {
      String moduleConfigurationSubDirName = modulePlugin.getModuleConfigurationSubDirName();
      if (moduleConfigurationSubDirName == null) {
         return null;
      }
      Path configDir = moduleContainer.getKorona().getKoronaSettings().getKoronaConfigDir().getFile();
      if (configDir == null) {
         return null;
      }
      JMenu partialSetupsMenu = MiscIcons.EMPTY.on(new JMenu("Partial setups"));
      partialSetupsMenu.setToolTipText("""
            <html>
            A partial setup is a sequence of modules that can be reused.<br>
            Partial setup with low leading number to be used before one with higher leading number.""");
      Path partialSetupsDir = configDir.resolve(moduleConfigurationSubDirName, "PartialSetups");
      GuiUtils.autoCreateContentMenu(partialSetupsMenu, () -> {
         populatePartialSetupsMenu(partialSetupsMenu, partialSetupsDir);
         if (partialSetupsMenu.getMenuComponentCount() == 0) {
            JMenuItem noneAvailableItem = partialSetupsMenu.add("<No partial setups available>");
            noneAvailableItem.setEnabled(false);
         }
         partialSetupsMenu.addSeparator();
         JMenuItem saveItem = MiscIcons.SAVE.on(partialSetupsMenu.add("Save selected modules as a partial setup..."));
         saveItem.setToolTipText(new HtmlStringBuilder()
               .html("Partial setups directory:<br>")
               .text(partialSetupsDir.toString())
               .build());
         saveItem.setEnabled(!selectedModules.isEmpty());
         saveItem.addActionListener(_ -> {
            try {
               FileUtils.createDirectories(partialSetupsDir);
            } catch (IOException e) {
               GuiUtils.showErrorDialog(dialog, "Error creating directory " + partialSetupsDir, e);
               return;
            }
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(partialSetupsDir.toFile());
            fileChooser.setFileFilter(new SuffixFileFilter(KoronaUtils.CDS_FILE_TYPE));
            int returnVal = fileChooser.showSaveDialog(dialog);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
               Path selectedFile = KoronaUtils.CDS_FILE_TYPE.ensureSuffix(fileChooser.getSelectedFile().toPath());
               ModuleContainer tmpModuleContainer = new ModuleContainer(moduleContainer.getKorona());
               tmpModuleContainer.getModuleList().appendXml(ModuleList.toXml(getSortedModuleMap().values()));
               ModuleUtils.fixAll(tmpModuleContainer.getModuleList());
               try {
                  tmpModuleContainer.writeConfiguration(selectedFile);
               } catch (IOException e) {
                  GuiUtils.showErrorDialog(dialog, "Error saving " + selectedFile, e);
               }
            }
         });

         JMenuItem openDirItem = MenuItems.showInFileExplorer(partialSetupsDir);
         partialSetupsMenu.add(openDirItem);
         openDirItem.setText("Open partial setups directory");
         openDirItem.setToolTipText(partialSetupsDir.toString());
      });
      return partialSetupsMenu;
   }

   private void populatePartialSetupsMenu(JMenu menu, Path dir) {
      try {
         List<FileInfo> fileInfos = FileUtils.listFilesWithAttributes(dir).stream()
               .sorted(Utils.comparingIgnoringCase(FileInfo::getFileName))
               .toList();
         for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.isDirectory()) {
               JMenu submenu = MiscIcons.EMPTY.on(new JMenu(fileInfo.getFileName()));
               populatePartialSetupsMenu(submenu, fileInfo.file());
               if (submenu.getMenuComponentCount() > 0) {
                  menu.add(submenu);
               }
            }
         }
         for (FileInfo fileInfo : fileInfos) {
            if (fileInfo.file().toString().endsWith(KoronaUtils.CDS_FILE_SUFFIX)) {
               JMenuItem fileItem = MiscIcons.EMPTY.on(menu.add(fileInfo.getFileName()));
               fileItem.setToolTipText(fileInfo.file().toString());
               fileItem.addActionListener(_ -> {
                  ModuleContainer tmpModuleContainer = new ModuleContainer(moduleContainer.getKorona());
                  try {
                     tmpModuleContainer.readConfiguration(fileInfo.file());
                     ModuleUtils.fixAll(tmpModuleContainer.getModuleList());
                     insertModules(tmpModuleContainer.getModules());
                  } catch (IOException e) {
                     GuiUtils.showErrorDialog(dialog, "Error reading " + fileInfo.file(), e);
                  }
               });
               Exec.CACHED_THREAD_POOL.submit(() -> {
                  PartialSetupInfo partialSetupInfo = getPartialSetupInfo(fileInfo);
                  SwingUtilities.invokeLater(() -> {
                     fileItem.setIcon(new CategoriesIcon(partialSetupInfo.categories));
                     fileItem.setToolTipText(new HtmlStringBuilder()
                           .text(fileInfo.file().toString())
                           .html("<br><br>Modules:<br>").html(partialSetupInfo.tooltip)
                           .build());
                  });
               });
            } // else: Ignore this file.
         }
      } catch (IOException e) {
         GuiUtils.showErrorDialog(dialog, "Error accessing directory " + dir, e);
      }
   }

   private PartialSetupInfo getPartialSetupInfo(FileInfo fileInfo) {
      PartialSetupInfo partialSetupInfo = PartialSetupInfo.CACHE.getIfPresent(fileInfo.file());
      if (partialSetupInfo != null && partialSetupInfo.lastModified.equals(fileInfo.lastModified())) {
         return partialSetupInfo;
      }
      Set<ModuleCategory> categories;
      String tooltip;
      try {
         ModuleContainer tmpModuleContainer = new ModuleContainer(moduleContainer.getKorona());
         tmpModuleContainer.readConfiguration(fileInfo.file());
         categories = getAllCategories(tmpModuleContainer.getModules());
         tooltip = tmpModuleContainer.getModules().stream()
               .map(module -> {
                  String colors = module.getModuleInfo().categories().stream()
                        .filter(category -> category != ModuleCategory.NO_MODIFICATION)
                        .map(category -> {
                           return "<span style='color: " + ColorUtils.colorToHex(category.getColor()) + ";'>▍</span>";
                        })
                        .collect(Collectors.joining());
                  return "<span style='white-space: nowrap'>" + colors + (colors.isEmpty() ? "" : "&nbsp;")
                        + HtmlEscapers.htmlEscaper().escape(module.getDisplayName()) + "</span>";
               })
               .collect(Collectors.joining(" ➔ "));
         if (tmpModuleContainer.getModules().size() > 8) {
            tooltip = "<div style='width: 600px;'>" + tooltip + "</div>";
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
         categories = Set.of();
         tooltip = HtmlEscapers.htmlEscaper().escape(e.toString());
      }
      partialSetupInfo = new PartialSetupInfo(fileInfo.lastModified(), categories, tooltip);
      PartialSetupInfo.CACHE.put(fileInfo.file(), partialSetupInfo);
      return partialSetupInfo;
   }

   private JPanel createBottomPanel() {
      okButton.setToolTipText("Accept changes and close window");
      okButton.addActionListener(_ -> accept());

      cancelButton.setToolTipText("Revert changes and close window");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(_ -> cancel());

      GuiUtils.setAccelerator(helpButton, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
      helpButton.addActionListener(_ -> getCurrentHelpID().show());

      JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
      panel.add(okButton);
      panel.add(cancelButton);
      panel.add(helpButton);
      return panel;
   }

   private void cancel() {
      moduleContainer.getConfigFileSettings().fromXml(backupCfs);
      moduleContainer.fromXml(backupCds);
      ok = false;
      close();
   }

   private void accept() {
      if (!CurrentInputComponent.commitEdit()) {
         return;
      }
      ok = true;
      BaseModule currentModule = getCurrentModule();
      lastModuleIndex = Math.max(0, moduleContainer.getModules().indexOf(currentModule));
      lastModuleName = currentModule != null ? currentModule.getPersistentName() : "";
      close();
   }

   private void close() {
      onClose.accept(ok);
      dialog.dispose();
   }

   private void updateHighlightedModules(@Nullable BaseModule mouseoverModule) {
      Set<BaseModule> modules = drag != null ? toHighlightedModules(drag.dragModule) : Set.of();
      if (modules.isEmpty()) {
         modules = toHighlightedModules(mouseoverModule);
      }
      if (modules.isEmpty()) {
         modules = toHighlightedModules(getCurrentModule());
      }
      if (!highlightModules.equals(modules)) {
         highlightModules = modules;
         modulePanel.repaint();
      }
   }

   private Set<BaseModule> toHighlightedModules(@Nullable BaseModule module) {
      switch (module) {
         case TemporaryComputationsBeginModule beginModule -> {
            return Set.of(beginModule, getTemporaryComputationsEndModule(beginModule));
         }
         case TemporaryComputationsEndModule endModule -> {
            return Set.of(getTemporaryComputationsBeginModule(endModule), endModule);
         }
         case CommentModule commentModule when commentModule.groupStart.getBooleanValue() && !commentModule.groupCollapsed.getBooleanValue() -> {
            return Set.of(commentModule, getEndGroupModule(commentModule));
         }
         case GroupEndModule groupEndModule -> {
            return Set.of(getBeginGroupModule(groupEndModule), groupEndModule);
         }
         case null, default -> {
            return Set.of();
         }
      }
   }

   private void paintModuleButton(Graphics2D g, JToggleButton button, BaseModule module) {
      boolean problem = module instanceof CommentModule groupBeginModule && groupBeginModule.groupCollapsed.getBooleanValue()
            ? getGroupModules(groupBeginModule).stream().anyMatch(this::hasProblems)
            : hasProblems(module);
      if (problem) {
         g.setColor(Color.RED);
         g.setStroke(GuiUtils.STROKE_3);
         g.drawRect(3, 3, button.getWidth() - 7, button.getHeight() - 7);
      }
      if (highlightModules.contains(module)) {
         g.setColor(Color.GREEN);
         g.setStroke(GuiUtils.STROKE_3);
         g.drawRect(3, 3, button.getWidth() - 7, button.getHeight() - 7);
      }
      float inactiveFraction;
      if (module instanceof CommentModule commentModule && commentModule.groupCollapsed.getBooleanValue()) {
         List<BaseModule> groupModules = getGroupModules(commentModule);
         long inactiveCount = groupModules.stream()
               .filter(m -> !m.active.getBooleanValue())
               .count();
         inactiveFraction = (float) inactiveCount / groupModules.size();
      } else {
         inactiveFraction = module.active.getBooleanValue() ? 0 : 1;
      }
      if (inactiveFraction > 0) {
         Shape originalClip = g.getClip();
         int width = Math.round(button.getWidth() * inactiveFraction);
         int height = button.getHeight();
         g.setClip(0, 0, width, height);
         g.setColor(new Color(0, 0, 0, 26));
         g.fillRect(0, 0, width, height);
         g.setColor(new Color(0, 0, 0, 51));
         g.setStroke(GuiUtils.STROKE_1);
         for (int x = -height; x <= width; x += 10) {
            g.drawLine(x, 0, x + height, height);
         }
         g.setClip(originalClip);
      }
      Set<ModuleCategory> categories;
      if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
         categories = getAllCategories(getGroupModules(commentModule));
      } else {
         categories = module.getModuleInfo().categories();
      }
      if (!categories.isEmpty() && !categories.equals(Set.of(ModuleCategory.NO_MODIFICATION))) {
         int width = Math.clamp(16 / categories.size(), 1, 4);
         int verticalMargin = 2;
         int x = 2;
         for (ModuleCategory category : categories) {
            g.setColor(category.getColor());
            g.fillRect(x, verticalMargin, width, button.getHeight() - 2 * verticalMargin);
            x += width;
         }
      }
   }

   private static Set<ModuleCategory> getAllCategories(List<BaseModule> modules) {
      return modules.stream()
            .flatMap(module -> module.getModuleInfo().categories().stream())
            .filter(category -> category != ModuleCategory.NO_MODIFICATION)
            .collect(Collectors.toCollection(LinkedHashSet::new));
   }

   private boolean hasProblems(BaseModule module) {
      if (!module.active.getBooleanValue()) {
         return false;
      }
      return hasUnspecifiedConfigFiles(module)
            || module.getModuleInfo().isDeprecated();
   }

   private boolean hasUnspecifiedConfigFiles(BaseModule module) {
      return module.getRequiredConfigFileServiceNames().stream()
            .map(moduleContainer.getConfigFileSettings()::getFile)
            .anyMatch(Objects::isNull);
   }

   private void makeModuleList() {
      wrappingLines.clear();
      moduleButtons.clear();
      moduleToDisplayedModuleButton.clear();

      int collapsedGroupCount = 0;
      List<BaseModule> modules = moduleContainer.getModules();
      for (int moduleIndex = 0; moduleIndex < modules.size(); moduleIndex++) {
         BaseModule module = modules.get(moduleIndex);
         if (collapsedGroupCount > 0) {
            if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
               collapsedGroupCount++;
            } else if (module instanceof GroupEndModule) {
               collapsedGroupCount--;
            }
            moduleToDisplayedModuleButton.put(module, moduleButtons.getLast());
            continue;
         }
         JToggleButton button = new JToggleButton(module.getDisplayName(), selectedModules.contains(module)) {
            @Override
            protected void paintComponent(Graphics g) {
               if (drag != null && module == drag.dragModule) {
                  return;
               }
               super.paintComponent(g);
               paintModuleButton((Graphics2D) g, this, module);
            }
         };
         button.addActionListener(e -> {
            boolean ctrlDown = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
            boolean shiftDown = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
            if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()
                  && selectedModules.equals(Set.of(commentModule)) && !ctrlDown && !shiftDown) {
               // This is a click on a group start button.
               // Make sure a double click does not toggle twice.
               if (lastGroupButtonClickModule != commentModule || lastGroupButtonClickTime.until(Instant.now(), ChronoUnit.MILLIS) > 500) {
                  // This is not the second click in a double click => Toggle the group.
                  lastGroupButtonClickModule = commentModule;
                  lastGroupButtonClickTime = Instant.now();
                  commentModule.groupCollapsed.toggle();
               } else {
                  // This is the second click in a double click => Do nothing.
                  button.setSelected(true);
               }
               return;
            }
            selectModules(module, shiftDown, ctrlDown);
         });
         if (module instanceof CommentModule commentModule) {
            boolean groupStart = commentModule.groupStart.getBooleanValue();
            boolean groupCollapsed = commentModule.groupCollapsed.getBooleanValue();
            if (groupCollapsed) {
               collapsedGroupCount++;
            }
            if (groupStart && groupCollapsed) {
               boolean active = commentModule.active.getBooleanValue();
               WhenShowingListening.connect(button, commentModule.active, () -> {
                  if (active != commentModule.active.getBooleanValue()) {
                     for (BaseModule m : getGroupModules(commentModule)) {
                        m.active.setBooleanValue(commentModule.active.getBooleanValue());
                     }
                     rewind();
                  }
               });
            }
            WhenShowingListening.connect(button, commentModule.groupStart, () -> {
               if (groupStart != commentModule.groupStart.getBooleanValue()) {
                  if (commentModule.groupStart.getBooleanValue()) {
                     moduleContainer.addModule(moduleContainer.getModules().indexOf(commentModule) + 1, new GroupEndModule());
                  } else {
                     BaseModule endGroupModule = getEndGroupModule(commentModule);
                     moduleContainer.removeModule(endGroupModule);
                     selectedModules.remove(endGroupModule);
                  }
                  rewind();
               }
            });
            WhenShowingListening.connect(button, commentModule.groupCollapsed, () -> {
               if (groupCollapsed != commentModule.groupCollapsed.getBooleanValue()) {
                  rewind();
               }
            });

            boolean lineBreak = commentModule.lineBreak.getBooleanValue();
            int verticalSpace = commentModule.verticalSpace.getIntValue();
            if (lineBreak) {
               wrappingLines.addLineBreak();
               wrappingLines.add(Box.createVerticalStrut(verticalSpace));
               wrappingLines.addLineBreak();
            }

            WhenShowingListening.connect(button, commentModule.lineBreak, () -> {
               if (lineBreak != commentModule.lineBreak.getBooleanValue()) {
                  rewind();
               }
            });

            WhenShowingListening.connect(button, commentModule.verticalSpace, () -> {
               if (verticalSpace != commentModule.verticalSpace.getIntValue()) {
                  rewind();
               }
            });

            String label = commentModule.label.getValue();
            StringBuilder text = new StringBuilder(label);
            if (!text.isEmpty()) {
               text.append("   ");
            }
            if (commentModule.groupStart.getBooleanValue()) {
               text.append('(');
            }
            if (groupCollapsed) {
               List<BaseModule> groupModules = getGroupModules(commentModule);
               long activeCount = groupModules.stream()
                     .filter(m -> m.active.getBooleanValue())
                     .count();
               if (groupModules.size() == activeCount) {
                  text.append(activeCount);
               } else {
                  text.append(activeCount).append('/').append(groupModules.size());
               }
               text.append(')');
            }
            button.setText(text.toString());
            MiscIcons.COMMENT.on(button);
            WhenShowingListening.connect(button, commentModule.label, () -> {
               if (!label.equals(commentModule.label.getValue())) {
                  rewind();
               }
            });

            WhenShowingListening.connect(button, commentModule.textComment, () -> {
               List<String> lines = commentModule.textComment.getValue().lines().toList();
               int maxLineLength = lines.stream().mapToInt(String::length).max().orElse(0);
               String p = maxLineLength > 120 ? "<p width='500'>" : "";
               String commentAsHtml = lines.stream()
                     .map(HtmlEscapers.htmlEscaper().asFunction())
                     .collect(Collectors.joining("<br>"));
               button.setToolTipText("<html>" + p + "Comment:<br>" + commentAsHtml);
            });
         } else {
            switch (module) {
               case GroupEndModule _ -> {
                  button.setText(")");
               }
               case TemporaryComputationsBeginModule beginModule -> {
                  boolean active = beginModule.active.getBooleanValue();
                  WhenShowingListening.connect(button, beginModule.active, () -> {
                     if (active != beginModule.active.getBooleanValue()) {
                        getTemporaryComputationsEndModule(beginModule).active.setBooleanValue(beginModule.active.getBooleanValue());
                        rewind();
                     }
                  });
               }
               case TemporaryComputationsEndModule endModule -> {
                  boolean active = endModule.active.getBooleanValue();
                  WhenShowingListening.connect(button, endModule.active, () -> {
                     if (active != endModule.active.getBooleanValue()) {
                        getTemporaryComputationsBeginModule(endModule).active.setBooleanValue(endModule.active.getBooleanValue());
                        rewind();
                     }
                  });
               }
               default -> {
               }
            }
            WhenShowingListening.connect(button, module.comment, () -> {
               HtmlStringBuilder tooltip = new HtmlStringBuilder()
                     .html(module.getModuleInfo().description())
                     .html("<p>").html(categoriesToHtml(module.getModuleInfo().categories(), false));
               String comment = module.comment.getValue();
               if (!comment.isBlank()) {
                  tooltip.html("<br><br>").text(comment);
               }
               button.setToolTipText(tooltip.build());
            });
         }
         button.setFocusable(false);
         MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
               updateHighlightedModules(module);
            }

            @Override
            public void mouseExited(MouseEvent e) {
               updateHighlightedModules(null);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
               if (!editable) {
                  return;
               }
               Point mouseScreenLocation = e.getPoint();
               SwingUtilities.convertPointToScreen(mouseScreenLocation, button);
               if (drag == null) {
                  button.setVisible(false);

                  JToggleButton dragButton = new JToggleButton(button.getText(), button.getIcon(), button.isSelected()) {
                     @Override
                     protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        paintModuleButton((Graphics2D) g, this, module);
                     }
                  };
                  dragButton.setSize(button.getSize());

                  drag = new Drag(module, dragButton, e.getPoint());

                  glassPane.setVisible(true);
                  glassPane.add(button);
                  glassPane.add(dragButton);

                  rewind();
               }
               drag.dragModule(mouseScreenLocation);
            }
         };
         button.addMouseListener(mouseListener);
         button.addMouseMotionListener(mouseListener);
         wrappingLines.add(button);
         ModuleButton moduleButton = new ModuleButton(module, moduleIndex, button, moduleButtons.size());
         moduleButtons.add(moduleButton);
         moduleToDisplayedModuleButton.put(module, moduleButton);
      }

      enableMenuButtons();
   }

   private void enableMenuButtons() {
      boolean canEdit = editable && !selectedModules.isEmpty();
      deleteButton.setEnabled(canEdit);
      NavigableMap<Integer, BaseModule> moduleMap = getSortedModuleMap();
      forwardButton.setEnabled(canEdit && moduleMap.firstKey() > 0);
      backwardButton.setEnabled(canEdit && moduleMap.lastKey() < moduleContainer.getModules().size() - 1);
   }

   private @Nullable BaseModule getCurrentModule() {
      return selectedModules.size() == 1 ? selectedModules.iterator().next() : null;
   }

   private TemporaryComputationsBeginModule getTemporaryComputationsBeginModule(TemporaryComputationsEndModule endModule) {
      TemporaryComputationsBeginModule beginModule = ModuleUtils.getBeginModule(moduleContainer, endModule);
      if (beginModule == null) {
         Log.global.warning("No temporary computations begin module for end module at index " + moduleContainer.getModules().indexOf(endModule));
         beginModule = new TemporaryComputationsBeginModule();
         moduleContainer.addModule(0, beginModule);
      }
      return beginModule;
   }

   private TemporaryComputationsEndModule getTemporaryComputationsEndModule(TemporaryComputationsBeginModule beginModule) {
      TemporaryComputationsEndModule endModule = ModuleUtils.getEndModule(moduleContainer, beginModule);
      if (endModule == null) {
         Log.global.warning("No temporary computations end module for begin module at index " + moduleContainer.getModules().indexOf(beginModule));
         endModule = new TemporaryComputationsEndModule();
         moduleContainer.addModule(endModule);
      }
      return endModule;
   }

   private CommentModule getBeginGroupModule(GroupEndModule groupEndModule) {
      CommentModule commentModule = ModuleUtils.getBeginGroupModule(moduleContainer, groupEndModule);
      if (commentModule == null) {
         Log.global.warning("No begin group module for end group module at index " + moduleContainer.getModules().indexOf(groupEndModule));
         commentModule = new CommentModule();
         commentModule.groupStart.setBooleanValue(true);
         moduleContainer.addModule(0, commentModule);
      }
      return commentModule;
   }

   private GroupEndModule getEndGroupModule(CommentModule groupBeginModule) {
      GroupEndModule groupEndModule = ModuleUtils.getEndGroupModule(moduleContainer, groupBeginModule);
      if (groupEndModule == null) {
         Log.global.warning("No end group module for begin group module at index " + moduleContainer.getModules().indexOf(groupBeginModule));
         groupEndModule = new GroupEndModule();
         moduleContainer.addModule(groupEndModule);
      }
      return groupEndModule;
   }

   private List<BaseModule> getGroupModules(CommentModule commentModule) {
      List<BaseModule> modules = moduleContainer.getModules();
      int iBegin = modules.indexOf(commentModule);
      GroupEndModule endGroupModule = getEndGroupModule(commentModule);
      int iEnd = modules.indexOf(endGroupModule) + 1;
      return modules.subList(iBegin, iEnd);
   }

   private Set<BaseModule> getEffectivelySelectedModules(Set<BaseModule> modules) {
      Set<BaseModule> result = new HashSet<>(modules);
      for (BaseModule module : modules) {
         if (module instanceof CommentModule commentModule && commentModule.groupCollapsed.getBooleanValue()) {
            result.addAll(getGroupModules(commentModule));
         }
      }
      return result;
   }

   private int getEffectiveModuleCount(ModuleButton moduleButton) {
      int nextButtonIndex = moduleButton.buttonIndex + 1;
      return nextButtonIndex < moduleButtons.size()
            ? moduleButtons.get(nextButtonIndex).moduleIndex - moduleButton.moduleIndex
            : moduleContainer.getModules().size() - moduleButton.moduleIndex;
   }

   private NavigableMap<Integer, BaseModule> getSortedModuleMap() {
      return getSortedModuleMap(getEffectivelySelectedModules(selectedModules));
   }

   private NavigableMap<Integer, BaseModule> getSortedModuleMap(Collection<BaseModule> modules) {
      NavigableMap<Integer, BaseModule> map = new TreeMap<>();
      for (BaseModule module : modules) {
         map.put(moduleContainer.getModules().indexOf(module), module);
      }
      return map;
   }

   private void stepCurrentModuleHorizontally(int step) {
      BaseModule currentModule = getCurrentModule();
      if (currentModule == null) {
         return;
      }
      int i = moduleToDisplayedModuleButton.get(currentModule).buttonIndex + step;
      if (i >= 0 && i < moduleButtons.size()) {
         selectModules(moduleButtons.get(i).module, false, false);
      }
   }

   private void stepCurrentModuleVertically(boolean up) {
      BaseModule currentModule = getCurrentModule();
      if (currentModule == null) {
         return;
      }
      ModuleButton displayedModuleButton = moduleToDisplayedModuleButton.get(currentModule);
      if (displayedModuleButton == null) {
         return;
      }
      Rectangle currentBounds = GuiUtils.getScreenBounds(displayedModuleButton.button);
      BaseModule targetModule = null;
      Rectangle targetBounds = null;
      for (ModuleButton moduleButton : moduleButtons) {
         Rectangle bounds = GuiUtils.getScreenBounds(moduleButton.button);
         if (up ? bounds.getMaxY() > currentBounds.getMinY() : bounds.getMinY() < currentBounds.getMaxY()) {
            // Wrong direction
            continue;
         }
         if (targetBounds != null) {
            if (up ? bounds.y < targetBounds.y : bounds.y > targetBounds.y) {
               // Further away
               continue;
            }
            if (bounds.y == targetBounds.y) {
               // Same distance away
               if (currentBounds.createIntersection(bounds).getWidth() < currentBounds.createIntersection(targetBounds).getWidth()) {
                  // Less horizontal overlap
                  continue;
               }
            }
         }
         targetBounds = bounds;
         targetModule = moduleButton.module;
      }
      if (targetModule == null) {
         return;
      }
      selectModules(targetModule, false, false);
   }

   private void selectModules(BaseModule module, boolean shiftDown, boolean ctrlDown) {
      if (shiftDown) {
         NavigableMap<Integer, BaseModule> moduleMap = getSortedModuleMap();
         if (moduleMap.isEmpty()) {
            selectedModules.add(module);
         } else {
            int index = moduleContainer.getModules().indexOf(module);
            if (index < moduleMap.firstKey()) {
               selectedModules.addAll(moduleContainer.getModules().subList(index, moduleMap.firstKey()));
            } else if (index > moduleMap.lastKey()) {
               selectedModules.addAll(moduleContainer.getModules().subList(moduleMap.lastKey() + 1, index + 1));
            } else {
               selectedModules.add(module);
            }
         }
      } else if (ctrlDown) {
         Utils.toggle(selectedModules, module);
      } else {
         selectedModules.clear();
         selectedModules.add(module);
      }
      rewind();
   }

   private void addModule(ModuleInfo moduleInfo) {
      try {
         addModule(getModuleManager().createModule(moduleInfo.getPersistentName()));
      } catch (ModuleCreationException e) {
         GuiUtils.showErrorDialog(dialog, "Could not create module " + moduleInfo.getDisplayName(), e);
      }
   }

   private void addModule(BaseModule module) {
      Set<Name> set = new LinkedHashSet<>(module.getRequiredConfigFileServiceNames());
      set.removeAll(moduleContainer.getConfigFileSettings().getFileServiceNames());
      if (!set.isEmpty()) {
         HtmlStringBuilder message = new HtmlStringBuilder()
               .text(module.getDisplayName()).html(" requires config files not available in current context:<br>");
         for (Name name : set) {
            message.text(name.displayName()).html("<br>");
         }
         GuiUtils.showErrorDialog(dialog, message.toString());
         return;
      }

      int index = getModuleInsertionIndex();
      moduleContainer.addModule(index, module);
      if (module instanceof TemporaryComputationsBeginModule) {
         moduleContainer.addModule(index + 1, new TemporaryComputationsEndModule());
      }
      selectedModules.clear();
      selectedModules.add(module);
      rewind();
   }

   private int getModuleInsertionIndex() {
      NavigableMap<Integer, BaseModule> moduleMap = getSortedModuleMap();
      return moduleMap.isEmpty() ? moduleContainer.getModules().size() : moduleMap.lastKey() + 1;
   }

   private void copySelectedModules() {
      modulesClipboard = ModuleList.toXml(getSortedModuleMap().values());
   }

   private void cutSelectedModules() {
      copySelectedModules();
      deleteSelectedModules();
   }

   private void pasteModulesClipboard() {
      if (modulesClipboard == null) {
         return;
      }
      ModuleList moduleList = new ModuleList(moduleContainer);
      moduleList.appendXml(modulesClipboard);
      ModuleUtils.fixAll(moduleList);
      insertModules(moduleList.getModules());
   }

   private void insertModules(List<BaseModule> modules) {
      int insertionIndex = getModuleInsertionIndex();
      for (int i = modules.size() - 1; i >= 0; i--) {
         moduleContainer.addModule(insertionIndex, modules.get(i));
      }
      selectedModules.clear();
      selectedModules.addAll(modules);
      rewind();
   }

   private void deleteSelectedModules() {
      List<CommentModule> notDeletedGroupBeginModules = Utils.getAllOfType(selectedModules, GroupEndModule.class)
            .flatMap(groupEndModule -> {
               CommentModule commentModule = getBeginGroupModule(groupEndModule);
               return selectedModules.contains(commentModule) ? Stream.empty() : Stream.of(commentModule);
            })
            .toList();
      for (CommentModule commentModule : notDeletedGroupBeginModules) {
         commentModule.groupStart.setValue(false);
      }
      Set<BaseModule> deleteModules = getEffectivelySelectedModules(selectedModules);
      for (BaseModule module : selectedModules) {
         switch (module) {
            case CommentModule commentModule when commentModule.groupStart.getBooleanValue() -> {
               deleteModules.add(getEndGroupModule(commentModule));
            }
            case TemporaryComputationsBeginModule beginModule -> {
               deleteModules.add(getTemporaryComputationsEndModule(beginModule));
            }
            case TemporaryComputationsEndModule endModule -> {
               deleteModules.add(getTemporaryComputationsBeginModule(endModule));
            }
            default -> {
            }
         }
      }
      if (deleteModules.isEmpty()) {
         return;
      }
      NavigableMap<Integer, BaseModule> moduleMap = getSortedModuleMap(deleteModules);
      moduleMap.values().forEach(moduleContainer::removeModule);
      selectedModules.clear();

      int index = Math.min(moduleMap.firstKey(), moduleContainer.getModules().size() - 1);
      if (index >= 0) {
         selectedModules.add(moduleContainer.getModules().get(index));
      }
      rewind();
   }

   private void moveSelectedModules(int shift) {
      Comparator<ModuleButton> comparator = Comparator.comparingInt(ModuleButton::buttonIndex);
      List<ModuleButton> toBeMoved = selectedModules.stream()
            .filter(module -> moduleToDisplayedModuleButton.get(module).module == module)
            .map(moduleToDisplayedModuleButton::get)
            .sorted(shift > 0 ? comparator.reversed() : comparator)
            .toList();
      for (ModuleButton moduleButton : toBeMoved) {
         moveModuleButton(moduleButton, moduleButton.buttonIndex + shift);
      }
   }

   private void moveModuleButton(ModuleButton moduleButton, int targetIndex) {
      int maxTargetIndex = switch (moduleButton.module) {
         case CommentModule commentModule when commentModule.groupStart.getBooleanValue() && !commentModule.groupCollapsed.getBooleanValue() -> {
            yield moduleToDisplayedModuleButton.get(getEndGroupModule(commentModule)).buttonIndex - 1;
         }
         case TemporaryComputationsBeginModule beginModule -> {
            yield moduleToDisplayedModuleButton.get(getTemporaryComputationsEndModule(beginModule)).buttonIndex - 1;
         }
         default -> moduleButtons.size() - 1;
      };
      int minTargetIndex = switch (moduleButton.module) {
         case GroupEndModule groupEndModule -> moduleToDisplayedModuleButton.get(getBeginGroupModule(groupEndModule)).buttonIndex + 1;
         case TemporaryComputationsEndModule endModule -> moduleToDisplayedModuleButton.get(getTemporaryComputationsBeginModule(endModule)).buttonIndex + 1;
         default -> 0;
      };
      targetIndex = Math.clamp(targetIndex, minTargetIndex, maxTargetIndex);
      ModuleButton target = moduleButtons.get(targetIndex);

      int shift = target.moduleIndex - moduleButton.moduleIndex;
      if (shift == 0) {
         return;
      }
      if (shift > 0) {
         shift += getEffectiveModuleCount(target) - getEffectiveModuleCount(moduleButton);
      }
      if (shift == 0) {
         return;
      }
      Set<BaseModule> modules = getEffectivelySelectedModules(Set.of(moduleButton.module));
      List<BaseModule> sortedModules = new ArrayList<>(getSortedModuleMap(modules).values());
      if (shift > 0) {
         Collections.reverse(sortedModules);
      }
      for (BaseModule module : sortedModules) {
         moduleContainer.moveModule(module, shift);
      }
      rewind();
   }

   private static boolean isLineBreak(BaseModule module) {
      return module instanceof CommentModule commentModule
            && commentModule.lineBreak.getBooleanValue();
   }

   private void rewind() {
      moduleContainer.configureWithoutData();
      makeModuleList();
      editCurrentModule();
      updateDividerLocation();
      updateHighlightedModules(null);
   }

   private ParameterEditor createParameterEditor(BaseModule module) {
      GUIConfig guiConfig = new GUIConfig()
            .setParameterEnabledDecider(parameter -> {
               return editable && (parameter == module.active || module.active.getBooleanValue());
            });
      module.customizeGUIConfig(guiConfig);
      ParameterEditor editor = new ParameterEditor(module.getParameters(), guiConfig);

      boolean activeInitially = module.active.getBooleanValue();
      AtomicBoolean onlyOnce = new AtomicBoolean();
      editor.getParameterChangeManager().addListener(() -> {
         if (activeInitially != module.active.getBooleanValue()) {
            if (onlyOnce.compareAndSet(false, true)) {
               rewind();
            }
         }
      });

      return editor;
   }

   private void editCurrentModule() {
      emptySelectionPanel.setVisible(selectedModules.isEmpty());
      singleSelectionPanel.setVisible(selectedModules.size() == 1);
      multiSelectionPanel.setVisible(selectedModules.size() > 1);
      BaseModule currentModule = getCurrentModule();
      if (currentModule != null) {
         String errorText;
         if (hasUnspecifiedConfigFiles(currentModule)) {
            errorText = "Required config files not specified";
         } else {
            errorText = null;
         }
         errorLabel.setVisible(errorText != null);
         errorLabel.setText(errorText);
         deprecatedLabel.setVisible(currentModule.getModuleInfo().isDeprecated());
         betaLabel.setVisible(currentModule.getModuleInfo().isBeta());
         descriptionLabel.setText("<html>" + currentModule.getModuleInfo().description()
               + categoriesToHtml(currentModule.getModuleInfo().categories(), true));
         updateConfigFileSettingsPanel(currentModule);
         parameterEditor = createParameterEditor(currentModule);
         helpButton.setToolTipText("Show help for " + currentModule.getDisplayName());
      } else {
         parameterEditor = new ParameterEditor(List.of());
         helpButton.setToolTipText("Show help");
      }

      JPanel panel = new VerticalScrollablePanel(new BorderLayout());
      panel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
      panel.add(parameterEditor.getEditorComponent());
      if (parameterEditor.someParameterHasVerticalFill()) {
         GuiUtils.replaceContent(parameterEditorPanel, panel);
      } else {
         JScrollPane scrollPane = new JScrollPane(panel);
         scrollPane.addMouseListener(new PopupMenuMouseListener(_ -> parameterEditor.makePopupMenu()));
         GuiUtils.replaceContent(parameterEditorPanel, scrollPane);
      }

      dialog.validate();
      dialog.repaint();
   }

   private void updateConfigFileSettingsPanel(BaseModule module) {
      configFileSettingsPanel.removeAll();

      List<Name> configFileServiceNames = Utils.toList(module.getRequiredConfigFileServiceNames(), module.getOptionalConfigFileServiceNames());
      if (configFileServiceNames.isEmpty()) {
         configFileSettingsPanel.setBorder(BorderFactory.createEmptyBorder());
      } else {
         ConfigFileSettings configFileSettings = moduleContainer.getConfigFileSettings();
         ConfigFileSettingsEditor configFileSettingsEditor = new ConfigFileSettingsEditor(configFileSettings, configFileServiceNames, editable, ContextVisibility.HIDE, false);
         configFileSettingsPanel.add(configFileSettingsEditor.getComponent());
         String title = configFileSettings.getName().displayName();
         Path file = configFileSettings.getFile();
         if (file != null) {
            title = title + " [" + file + "]";
         }
         configFileSettingsPanel.setBorder(BorderFactory.createTitledBorder(title));
      }

      configFileSettingsPanel.validate();
      configFileSettingsPanel.repaint();
   }

   private static String categoryToHtml(ModuleCategory category) {
      return "<span style='background-color: " + ColorUtils.colorToHex(category.getColor()) + ";'>&nbsp;" +
            HtmlEscapers.htmlEscaper().escape(category.getLabel()) + "&nbsp;</span>";
   }

   private static String categoriesToHtml(Collection<ModuleCategory> categories, boolean padLeft) {
      String delimiter = "&nbsp;&nbsp;&nbsp;&nbsp;";
      return categories.stream()
            .map(ModuleEditor::categoryToHtml)
            .collect(Collectors.joining(delimiter, padLeft ? delimiter : "", ""));
   }

   private record ModuleButton(BaseModule module, int moduleIndex, JToggleButton button, int buttonIndex) {
      @Override
      public String toString() {
         return module.getPersistentName() + " (" + moduleIndex + "), button: " + buttonIndex;
      }
   }

   private final class Drag {
      private final BaseModule dragModule;
      private final JComponent dragButton;
      private final Point dragButtonOffset;

      private Drag(BaseModule dragModule, JComponent dragButton, Point dragButtonOffset) {
         this.dragModule = dragModule;
         this.dragButton = dragButton;
         this.dragButtonOffset = dragButtonOffset;
      }

      private void dragModule(Point mouseScreenLocation) {
         Point dragButtonLocation = new Point(mouseScreenLocation);
         SwingUtilities.convertPointFromScreen(dragButtonLocation, glassPane);
         dragButtonLocation.translate(-dragButtonOffset.x, -dragButtonOffset.y);
         dragButton.setLocation(dragButtonLocation);
         Rectangle dragBounds = GuiUtils.getScreenBounds(dragButton);
         Rectangle firstBounds = GuiUtils.getScreenBounds(moduleButtons.getFirst().button);
         Rectangle lastBounds = GuiUtils.getScreenBounds(moduleButtons.getLast().button);
         double dragY = Math.clamp(dragBounds.getCenterY(), firstBounds.getCenterY(), lastBounds.getCenterY());

         ModuleButton dragModuleButton = moduleToDisplayedModuleButton.get(dragModule);
         ModuleButton firstOverlap = null;
         ModuleButton lastOverlap = null;
         ModuleButton lastInRow = null;
         double rowMaxY = Double.NaN;
         for (ModuleButton moduleButton : moduleButtons) {
            Rectangle bounds = GuiUtils.getScreenBounds(moduleButton.button);
            if (bounds.getMaxY() < dragY) {
               // Too far up.
               continue;
            }
            if (bounds.getMinY() > rowMaxY) {
               // Too far down.
               break;
            }
            // In correct row.
            rowMaxY = bounds.getMaxY();
            lastInRow = moduleButton;
            boolean overlapsInX = bounds.getMinX() <= dragBounds.getMaxX() && dragBounds.getMinX() <= bounds.getMaxX();
            if (overlapsInX || moduleButton.module == dragModule) {
               if (firstOverlap == null) {
                  firstOverlap = moduleButton;
               }
               lastOverlap = moduleButton;
            }
         }

         int targetIndex;
         if (firstOverlap != null) {
            // Found row and overlap in x
            targetIndex = firstOverlap.buttonIndex;
            if (firstOverlap != lastOverlap) {
               Rectangle firstOverlapBounds = GuiUtils.getScreenBounds(firstOverlap.button);
               Rectangle lastOverlapBounds = GuiUtils.getScreenBounds(lastOverlap.button);
               double overlapCenterX = (firstOverlapBounds.getMinX() + lastOverlapBounds.getMaxX()) / 2;
               if (dragBounds.getCenterX() > overlapCenterX) {
                  targetIndex++;
               }
            }
            if (targetIndex > 0 && targetIndex < moduleButtons.size() - 1
                  && isLineBreak(moduleButtons.get(targetIndex).module) && !isLineBreak(dragModule)) {
               targetIndex++;
            }
         } else if (lastInRow != null) {
            // Found row, but no overlap in x
            targetIndex = lastInRow.buttonIndex;
            if (dragModuleButton.buttonIndex >= targetIndex) {
               targetIndex++;
            }
         } else {
            targetIndex = dragModuleButton.buttonIndex;
         }
         moveModuleButton(dragModuleButton, targetIndex);
      }
   }

   private static final class CategoriesIcon extends AbstractIcon {
      private final Set<ModuleCategory> categories;

      private CategoriesIcon(Set<ModuleCategory> categories) {
         super(16, 16);

         this.categories = categories;
      }

      @Override
      protected void paintIcon(Component c, Graphics2D g, int x, int y) {
         if (!categories.isEmpty() && !categories.equals(Set.of(ModuleCategory.NO_MODIFICATION))) {
            int width = Math.clamp(getIconWidth() / categories.size(), 1, 4);
            int height = 12;
            x += (getIconWidth() - categories.size() * width) / 2;
            y += (getIconHeight() - height) / 2;
            for (ModuleCategory category : categories) {
               g.setColor(category.getColor());
               g.fillRect(x, y, width, height);
               x += width;
            }
         }
      }
   }

   /**
    * Hierarchy of module classes.
    */
   private final class Node {
      private final NavigableMap<String, Node> subNodes = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      private final List<ModuleInfo> moduleInfos = new ArrayList<>();

      private Node() {
      }

      private void add(ModuleInfo moduleInfo) {
         Node node = this;
         for (String group : moduleInfo.grouping()) {
            node = node.subNodes.computeIfAbsent(group, _ -> new Node());
         }
         node.moduleInfos.add(moduleInfo);
      }

      private JMenu makeNewMenuHierarchy(String menuText) {
         JMenu menu = new JMenu(menuText);
         subNodes.forEach((group, node) -> {
            menu.add(MiscIcons.EMPTY.on(node.makeNewMenuHierarchy(group)));
         });
         moduleInfos.stream()
               .sorted(Utils.comparingIgnoringCase(ModuleInfo::getDisplayName))
               .forEach(moduleInfo -> {
                  String title = moduleInfo.moduleClass() == TemporaryComputationsBeginModule.class
                        ? "Temporary computations"
                        : moduleInfo.getDisplayName();
                  JMenuItem createModuleItem = menu.add(title);
                  createModuleItem.setIcon(new CategoriesIcon(moduleInfo.categories()));
                  createModuleItem.setEnabled(modulePredicate.test(moduleInfo));
                  createModuleItem.addActionListener(_ -> addModule(moduleInfo));
                  createModuleItem.setToolTipText("<html>" + moduleInfo.description()
                        + "<p>" + categoriesToHtml(moduleInfo.categories(), false));
                  if (moduleInfo.isDeprecated()) {
                     String text = new HtmlStringBuilder()
                           .html("<span style='color:gray;'>")
                           .text(createModuleItem.getText())
                           .html("</span>")
                           .build();
                     createModuleItem.setText(text);
                  }
               });
         if (menu.getMenuComponentCount() == 0) {
            JMenuItem emptyItem = MiscIcons.EMPTY.on(menu.add("<Empty>"));
            emptyItem.setEnabled(false);
         }
         return menu;
      }
   }

   private record PartialSetupInfo(
         Instant lastModified,
         Set<ModuleCategory> categories,
         String tooltip
   ) {
      private static final Cache<Path, PartialSetupInfo> CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
   }
}
