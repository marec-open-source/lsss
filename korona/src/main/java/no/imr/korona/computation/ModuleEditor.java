package no.imr.korona.computation;

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
import no.imr.tools.Utils;
import no.imr.tools.help.HelpID;
import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
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
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
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
   private long lastGroupButtonClickTime;
   private boolean ok;

   private final boolean editable;
   private ModulePredicate modulePredicate = ModulePredicate.alwaysTrue();
   private Consumer<Boolean> onClose = __ -> {
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

   public ModuleEditor(ModuleContainer moduleContainer, boolean editable, @Nullable Component referenceComponent) {
      this.moduleContainer = moduleContainer;
      this.editable = editable;

      backupCfs = moduleContainer.getConfigFileSettings().toXml();
      backupCds = moduleContainer.toXml();

      ModuleUtils.fixGroupEndModules(moduleContainer);

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
      allActiveButton.addActionListener(e -> modulesActive(true));
      JButton noneActiveButton = new JButton("Set none active");
      noneActiveButton.addActionListener(e -> modulesActive(false));
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
      splitPane.setDividerLocation(height + insets.top + insets.bottom + splitPaneDividerSize);
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
         getModuleManager().getModuleInfos(modulePlugin).forEach(moduleInfo -> {
            if (moduleInfo.moduleClass() == GroupEndModule.class) {
               return;
            }
            node.add(moduleInfo);
         });

         JMenu newMenu = node.makeNewMenuHierarchy(modulePlugin.getName().displayName());
         newMenu.setEnabled(editable);

         if (modulePlugin instanceof KoronaModulePlugin && getModuleManager().getModulePlugins().size() == 1) {
            newMenu.setText("New");
            newMenu.setMnemonic(KeyEvent.VK_N);
         }

         menuBar.add(newMenu);
      }

      menuBar.add(Box.createVerticalStrut(BUTTON_DIMENSION.height + 2 * 3)); // Also acts as horizontal glue

      menuBar.add(Box.createHorizontalGlue());

      forwardButton.setPreferredSize(BUTTON_DIMENSION);
      forwardButton.setToolTipText("Move selected modules one position forward");
      forwardButton.addActionListener(e -> moveSelectedModules(-1));
      GuiUtils.setAccelerator(forwardButton, KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, KeyEvent.ALT_DOWN_MASK));
      menuBar.add(forwardButton);

      menuBar.add(Box.createHorizontalStrut(5));

      backwardButton.setPreferredSize(BUTTON_DIMENSION);
      backwardButton.setToolTipText("Move selected modules one position backward");
      backwardButton.addActionListener(e -> moveSelectedModules(1));
      GuiUtils.setAccelerator(backwardButton, KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK));
      menuBar.add(backwardButton);

      menuBar.add(Box.createHorizontalStrut(25));

      JButton menuBarHelpButton = MiscIcons.HELP.on(new JButton());
      menuBarHelpButton.setPreferredSize(BUTTON_DIMENSION);
      menuBarHelpButton.setToolTipText("Show help for module editor");
      menuBarHelpButton.addActionListener(e -> getModuleEditorHelpID().show());
      menuBar.add(menuBarHelpButton);

      menuBar.add(Box.createHorizontalStrut(25));

      deleteButton.setPreferredSize(BUTTON_DIMENSION);
      deleteButton.setToolTipText("Delete selected modules");
      deleteButton.addActionListener(e -> deleteSelectedModules());
      menuBar.add(deleteButton);

      menuBar.add(Box.createHorizontalGlue());
   }

   private JPanel createBottomPanel() {
      okButton.setToolTipText("Accept changes and close window");
      okButton.addActionListener(e -> accept());

      cancelButton.setToolTipText("Revert changes and close window");
      GuiUtils.setAccelerator(cancelButton, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
      cancelButton.addActionListener(e -> cancel());

      GuiUtils.setAccelerator(helpButton, KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
      helpButton.addActionListener(e -> getCurrentHelpID().show());

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
      if (!parameterEditor.commitEdits()) {
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
         case TemporaryComputationsBeginModule beginModule when beginModule.active.getBooleanValue() -> {
            TemporaryComputationsEndModule endModule = ModuleUtils.getEndModule(moduleContainer, beginModule);
            return endModule != null ? Set.of(beginModule, endModule) : Set.of();
         }
         case TemporaryComputationsEndModule endModule when endModule.active.getBooleanValue() -> {
            TemporaryComputationsBeginModule beginModule = ModuleUtils.getBeginModule(moduleContainer, endModule);
            return beginModule != null ? Set.of(endModule, beginModule) : Set.of();
         }
         case CommentModule commentModule when commentModule.groupStart.getBooleanValue() && !commentModule.groupCollapsed.getBooleanValue() -> {
            return Set.of(commentModule, getEndGroupModule(commentModule));
         }
         case GroupEndModule groupEndModule -> {
            return Set.of(groupEndModule, getBeginGroupModule(groupEndModule));
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
      if (!module.active.getBooleanValue()) {
         g.setColor(new Color(0, 0, 0, 26));
         g.fillRect(0, 0, button.getWidth(), button.getHeight());
         g.setColor(new Color(0, 0, 0, 51));
         g.setStroke(GuiUtils.STROKE_1);
         int height = button.getHeight();
         for (int x = -height; x <= button.getWidth(); x += 10) {
            g.drawLine(x, 0, x + height, height);
         }
      }
      EnumSet<ModuleCategory> categories = module.getModuleInfo().categories();
      if (!categories.equals(EnumSet.of(ModuleCategory.NO_MODIFICATION))) {
         int width = 4;
         int verticalMargin = 2;
         int x = 2;
         for (ModuleCategory category : categories) {
            g.setColor(category.getColor());
            g.fillRect(x, verticalMargin, width, button.getHeight() - 2 * verticalMargin);
            x += width;
         }
      }
   }

   private boolean hasProblems(BaseModule module) {
      if (!module.active.getBooleanValue()) {
         return false;
      }
      return hasUnspecifiedConfigFiles(module)
            || isUnmatchedTemporaryComputationsBeginModule(module)
            || isUnmatchedTemporaryComputationsEndModule(module)
            || module.getModuleInfo().isDeprecated();
   }

   private boolean hasUnspecifiedConfigFiles(BaseModule module) {
      return module.getRequiredConfigFileServiceNames().stream()
            .map(moduleContainer.getConfigFileSettings()::getFile)
            .anyMatch(Objects::isNull);
   }

   private boolean isUnmatchedTemporaryComputationsBeginModule(BaseModule module) {
      return module instanceof TemporaryComputationsBeginModule beginModule
            && ModuleUtils.getEndModule(moduleContainer, beginModule) == null;
   }

   private boolean isUnmatchedTemporaryComputationsEndModule(BaseModule module) {
      return module instanceof TemporaryComputationsEndModule endModule
            && ModuleUtils.getBeginModule(moduleContainer, endModule) == null;
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
               if (lastGroupButtonClickModule != commentModule || System.currentTimeMillis() - lastGroupButtonClickTime > 500) {
                  // This is not the second click in a double click => Toggle the group.
                  lastGroupButtonClickModule = commentModule;
                  lastGroupButtonClickTime = System.currentTimeMillis();
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
            boolean groupCollapsed = commentModule.groupCollapsed.getBooleanValue();
            if (groupCollapsed) {
               collapsedGroupCount++;
            }
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
            String text = groupStart
                  ? (label.isEmpty() ? "" : label + "   ") + (groupCollapsed ? "(...)" : "(")
                  : label;
            button.setText(text);
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
                     .map(HtmlEscapers.htmlEscaper()::escape)
                     .collect(Collectors.joining("<br>"));
               button.setToolTipText("<html>" + p + "Comment:<br>" + commentAsHtml);
            });
         } else {
            if (module instanceof GroupEndModule) {
               button.setText(")");
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

   private void createModule(ModuleInfo moduleInfo) {
      try {
         BaseModule module = getModuleManager().createModule(moduleInfo.getPersistentName());

         Set<Name> set = new LinkedHashSet<>(module.getRequiredConfigFileServiceNames());
         set.removeAll(moduleContainer.getConfigFileSettings().getFileServiceNames());
         if (!set.isEmpty()) {
            StringBuilder message = new StringBuilder("<html>" + moduleInfo.getDisplayName() + " requires config files not available in current context:<br>");
            for (Name name : set) {
               message.append(name.displayName()).append("<br>");
            }
            GuiUtils.showErrorDialog(dialog, message.toString());
            return;
         }

         int index = getModuleInsertionIndex();
         moduleContainer.addModule(index, module);
         selectedModules.clear();
         selectedModules.add(module);
         rewind();
      } catch (ModuleCreationException e) {
         GuiUtils.showErrorDialog(dialog, "Could not create module " + moduleInfo.getDisplayName(), e);
      }
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
      int insertionIndex = getModuleInsertionIndex();
      ModuleList moduleList = new ModuleList(moduleContainer);
      moduleList.appendXml(modulesClipboard);
      List<BaseModule> modules = moduleList.getModules();
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
         if (module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue()) {
            deleteModules.add(getEndGroupModule(commentModule));
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
      int maxTargetIndex = moduleButton.module instanceof CommentModule commentModule && commentModule.groupStart.getBooleanValue() && !commentModule.groupCollapsed.getBooleanValue()
            ? moduleToDisplayedModuleButton.get(getEndGroupModule(commentModule)).buttonIndex - 1
            : moduleButtons.size() - 1;
      int minTargetIndex = moduleButton.module instanceof GroupEndModule groupEndModule
            ? moduleToDisplayedModuleButton.get(getBeginGroupModule(groupEndModule)).buttonIndex + 1
            : 0;
      targetIndex = Math.clamp(targetIndex, minTargetIndex, maxTargetIndex);
      ModuleButton target = moduleButtons.get(targetIndex);

      int shift = target.moduleIndex - moduleButton.moduleIndex;
      if (shift == 0) {
         return;
      }
      if (shift > 0) {
         shift += getEffectiveModuleCount(target) - getEffectiveModuleCount(moduleButton);
      }
      moveModules(shift, getEffectivelySelectedModules(Set.of(moduleButton.module)));
   }

   private void moveModules(int shift, Collection<BaseModule> modules) {
      if (shift == 0) {
         return;
      }
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
      ParameterEditor editor = new ParameterEditor(module.getParameters());
      module.customizeGUIConfig(editor.getGUIConfig());

      boolean activeInitially = module.active.getBooleanValue();
      AtomicBoolean onlyOnce = new AtomicBoolean();
      editor.getParameterChangeManager().addListener(() -> {
         if (activeInitially != module.active.getBooleanValue()) {
            if (onlyOnce.compareAndSet(false, true)) {
               rewind();
            }
         }
      });

      editor.getGUIConfig().setParameterEnabledDecider(parameter -> {
         return editable && (parameter == module.active || module.active.getBooleanValue());
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
         } else if (currentModule.active.getBooleanValue() && isUnmatchedTemporaryComputationsBeginModule(currentModule)) {
            errorText = "Missing end of temporary computations";
         } else if (currentModule.active.getBooleanValue() && isUnmatchedTemporaryComputationsEndModule(currentModule)) {
            errorText = "Missing begin of temporary computations";
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
         scrollPane.addMouseListener(new PopupMenuMouseListener(e -> parameterEditor.makePopupMenu()));
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
            node = node.subNodes.computeIfAbsent(group, k -> new Node());
         }
         node.moduleInfos.add(moduleInfo);
      }

      private JMenu makeNewMenuHierarchy(String menuText) {
         JMenu menu = new JMenu(menuText);
         subNodes.forEach((group, node) -> {
            menu.add(node.makeNewMenuHierarchy(group));
         });
         moduleInfos.stream()
               .sorted(Utils.comparingIgnoringCase(ModuleInfo::getDisplayName))
               .forEach(moduleInfo -> {
                  JMenuItem createModuleItem = menu.add(moduleInfo.getDisplayName());
                  createModuleItem.setIcon(new AbstractIcon(16, 16) {
                     @Override
                     protected void paintIcon(Component c, Graphics2D g, int x, int y) {
                        EnumSet<ModuleCategory> categories = moduleInfo.categories();
                        if (!categories.equals(EnumSet.of(ModuleCategory.NO_MODIFICATION))) {
                           int width = 4;
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
                  });
                  createModuleItem.setEnabled(modulePredicate.test(moduleInfo));
                  createModuleItem.addActionListener(e -> createModule(moduleInfo));
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
}
