package no.imr.lsss.framework.config.application.packages;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.application.packages.pojo.CallbackInfo;
import no.imr.lsss.framework.config.application.packages.pojo.KeyStrokeInfo;
import no.imr.lsss.framework.config.application.packages.pojo.MenuItemInfo;
import no.imr.lsss.framework.config.application.packages.pojo.ToolbarButtonInfo;
import no.imr.lsss.framework.config.application.packages.pojo.UiItemInfo;
import no.imr.lsss.framework.packages.ActionArgument;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.parameter.gui.input.GUIConfig;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.GridBag;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.swing.SuffixFileFilter;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WorkerDialog;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import no.marec.lsss.api.util.observing.Subscription;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

final class PackagesConfView implements ViewHolder.View {
   private final PackagesConf packagesConf;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JScrollPane scrollPane = GuiUtils.createScrollPane(mainPanel);
   private @Nullable Subscription subscription;
   private final Insets margin = new Insets(0, 0, 0, 0);

   private final Map<ToolbarButtonInfo, JComponent> toolbarButtonInfoToComponent = new HashMap<>();
   private final Map<JComponent, Integer> menuItemInfoComponentToRow = new HashMap<>();
   private @Nullable Object dragItem;

   PackagesConfView(PackagesConf packagesConf) {
      this.packagesConf = packagesConf;

      scrollPane.addHierarchyListener(e -> {
         if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (subscription != null) {
               subscription.unsubscribe();
               subscription = null;
            }
            if (scrollPane.isShowing()) {
               subscription = packagesConf.getChangeManager().subscribe(GuiListeners.coalescingLater(this::update));
               update();
            } else {
               mainPanel.removeAll();
            }
         }
      });
   }

   @Override
   public JComponent getComponent() {
      return scrollPane;
   }

   private void update() {
      toolbarButtonInfoToComponent.clear();
      menuItemInfoComponentToRow.clear();

      GridBag gridBag = new GridBag()
            .configureVerticalBox();

      String warning;
      if (!packagesConf.getLsssServerConf().getLsssServerPluginEnabled()) {
         warning = "The LSSS server plugin is not enabled.";
      } else if (!packagesConf.getLsssServerConf().serverActive.getBooleanValue()) {
         warning = "The LSSS server is not started.";
      } else {
         warning = null;
      }
      if (warning != null) {
         gridBag.addWithLineBreak(new JLabel("<html><h2 style='color: #ff6347;'>" + warning + "<h2>"));
         gridBag.addWithLineBreak(Box.createVerticalStrut(10));
         gridBag.addWithLineBreak(new JSeparator());
         gridBag.addWithLineBreak(Box.createVerticalStrut(10));
      }

      gridBag.addWithLineBreak(packagesConf.createParameterEditor().getEditorComponent());

      gridBag.addWithLineBreak(GuiUtils.labelLikeHtmlTextPane("Make sure that the "
            + "<a href=\"https://pypi.org/project/requests/\">requests</a> Python library is available", href -> {
         GuiUtils.desktopBrowse(URI.create(href), mainPanel);
      }));

      UserDefinedPackage userDefinedPackage = packagesConf.getCurrentUserDefinedPackage();

      addPackages(gridBag, userDefinedPackage);

      if (userDefinedPackage != null) {
         addActions(gridBag, userDefinedPackage);
         addCallbacks(gridBag, userDefinedPackage);
         addKeyStrokes(gridBag, userDefinedPackage);
         addToolbarButtons(gridBag, userDefinedPackage);
         addMenus(gridBag, userDefinedPackage);
      }

      GuiUtils.replaceContent(mainPanel, gridBag.getPanel());
   }

   private void addPackages(GridBag gridBag, @Nullable UserDefinedPackage userDefinedPackage) {
      JButton addButton = newButton(MiscIcons.ADD, "Add new package", _ -> {
         UserDefinedPackage definedPackage = new UserDefinedPackage(packagesConf);
         boolean ok = editPackage(definedPackage, "New package");
         if (ok) {
            packagesConf.addUserDefinedPackage(definedPackage);
         }
      });

      JButton importButton = newButton(MiscIcons.IMPORT, "Import package from zip file", _ -> importPackage());

      JButton reloadButton = newButton(MiscIcons.REFRESH, "Reload packages", _ -> packagesConf.reloadPackages());

      JButton exampleButton = newButton(MiscIcons.MENU, "Menu");
      GuiUtils.addPopupMenuToButton(exampleButton, popupMenu -> {
         JMenuItem exampleItem = MiscIcons.ADD.on(popupMenu.add("Create example package"));
         exampleItem.addActionListener(_ -> createExamplePackage());
      });

      addHeader(gridBag, "Packages", addButton, importButton, reloadButton, exampleButton);

      JButton deleteButton = newButton(MiscIcons.DELETE, "Delete");
      if (userDefinedPackage != null) {
         deleteButton.addActionListener(_ -> {
            int deleteAnswer = GuiUtils.showOptionDialog(deleteButton, "Question", "Delete package and associated files?", new String[]{"Delete", "Cancel"});
            if (deleteAnswer != 0) {
               return;
            }
            packagesConf.deleteUserDefinedPackage(userDefinedPackage);
         });
      } else {
         deleteButton.setEnabled(false);
      }

      JButton editButton = newButton(MiscIcons.EDIT, "Edit");
      if (userDefinedPackage != null) {
         editButton.addActionListener(_ -> {
            boolean ok = editPackage(userDefinedPackage, "Edit package");
            if (ok) {
               packagesConf.sortPackages();
               packagesConf.getChangeManager().notifyListeners();
            }
         });
      } else {
         editButton.setEnabled(false);
      }

      JButton showDocButton = newButton(MiscIcons.HELP, "Show package documentation");
      if (userDefinedPackage != null) {
         showDocButton.addActionListener(_ -> {
            Path indexHtml = userDefinedPackage.getDir().resolve("doc").resolve("index.html");
            if (Files.exists(indexHtml)) {
               URI uri = URI.create(packagesConf.getLsssServerConf().getServerBaseUri() + "/lsss/package/" + userDefinedPackage.id + "/file/doc/index.html");
               GuiUtils.desktopBrowse(uri, mainPanel);
            } else {
               JOptionPane.showMessageDialog(showDocButton, "Documentation not available:\n" + indexHtml);
            }
         });
      } else {
         showDocButton.setEnabled(false);
      }

      JButton browseButton = newButton(MiscIcons.OPEN, "Browse package directory");
      if (userDefinedPackage != null) {
         browseButton.addActionListener(_ -> GuiUtils.desktopBrowse(userDefinedPackage.getDir().toUri(), mainPanel));
      } else {
         browseButton.setEnabled(false);
      }

      JButton exportButton = newButton(MiscIcons.EXPORT, "Export package to zip file");
      if (userDefinedPackage != null) {
         exportButton.addActionListener(_ -> exportPackage(userDefinedPackage));
      } else {
         exportButton.setEnabled(false);
      }

      JComboBox<Object> comboBox = new JComboBox<>(packagesConf.getUserDefinedPackages().toArray());
      comboBox.setSelectedItem(userDefinedPackage);
      comboBox.setRenderer(new DefaultListCellRenderer() {
         @Override
         public Component getListCellRendererComponent(JList<?> list, @Nullable Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String text;
            String toolTipText;
            UserDefinedPackage aPackage = (UserDefinedPackage) value;
            if (aPackage != null) {
               text = aPackage.getEffectiveLabel();
               toolTipText = UserDefinedUtils.combinedText("Package id: " + aPackage.id, aPackage.info.description);
            } else {
               text = "";
               toolTipText = null;
            }
            setToolTipText(toolTipText);
            return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
         }
      });
      comboBox.addActionListener(_ -> {
         packagesConf.setCurrentUserDefinedPackage((UserDefinedPackage) comboBox.getSelectedItem());
         update();
      });

      JLabel packagesLabel = MiscIcons.EMPTY.on(new JLabel());
      packagesLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, packagesLabel.getIconTextGap()));

      JPanel packagesPanel = new JPanel(new BorderLayout());
      packagesPanel.add(comboBox);
      packagesPanel.add(packagesLabel, BorderLayout.WEST);

      addRow(gridBag, packagesPanel, editButton, showDocButton, browseButton, exportButton, deleteButton);

      if (userDefinedPackage != null) {
         String description = userDefinedPackage.info.description;
         if (!description.isEmpty()) {
            gridBag.addWithLineBreak(new JLabel(UserDefinedUtils.combinedText("", description)));
         }
      }
   }

   private void addActions(GridBag gridBag, UserDefinedPackage userDefinedPackage) {
      JButton addButton = newButton(MiscIcons.ADD, "Add new action", _ -> {
         UserDefinedAction userDefinedAction = new UserDefinedAction(userDefinedPackage);
         boolean ok = editAction(userDefinedAction, "New action");
         if (ok) {
            userDefinedPackage.addUserDefinedAction(userDefinedAction);
         }
      });
      addHeader(gridBag, "Actions", addButton);

      List<UserDefinedAction> actions = userDefinedPackage.getActions().stream()
            .sorted(Utils.comparingIgnoringCase(UserDefinedAction::getEffectiveLabel))
            .toList();
      for (UserDefinedAction userDefinedAction : actions) {
         JButton runButton = newButton(MiscIcons.PLAY, "Run");
         runButton.addActionListener(e -> {
            Optional<Map<String, Object>> input = UserDefinedUtils.showInputDialog(userDefinedAction, userDefinedAction.info.inputParameters, runButton);
            if (input.isEmpty()) {
               return;
            }
            userDefinedAction.execute(new ActionArgument(e, input.get()));
         });

         JButton browseActionButton = newButton(MiscIcons.OPEN, "Browse action directory", _ -> GuiUtils.desktopBrowse(userDefinedAction.getDir().toUri(), mainPanel));

         JButton deleteButton = newButton(MiscIcons.DELETE, "Delete");
         deleteButton.addActionListener(_ -> {
            int deleteAnswer = GuiUtils.showOptionDialog(deleteButton, "Question", "Delete action and associated files?", new String[]{"Delete", "Cancel"});
            if (deleteAnswer != 0) {
               return;
            }
            if (hasUiItems(userDefinedPackage, userDefinedAction.id)) {
               int answer = JOptionPane.showConfirmDialog(deleteButton, "Also delete UI integrations?", "Question", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
               if (answer != JOptionPane.OK_OPTION) {
                  return;
               }
               deleteUiItems(userDefinedPackage, userDefinedAction.id);
            }
            userDefinedPackage.deleteUserDefinedAction(userDefinedAction);
         });

         JButton editButton = newButton(MiscIcons.EDIT, "Edit", _ -> {
            String oldActionId = userDefinedAction.id;
            boolean ok = editAction(userDefinedAction, "Edit action");
            if (ok) {
               String newActionId = userDefinedAction.id;
               if (!newActionId.equals(oldActionId)) {
                  updateUiItems(userDefinedPackage, oldActionId, newActionId);
                  userDefinedPackage.savePackageInfo();
               }
               userDefinedAction.install();
               packagesConf.getChangeManager().notifyListeners();
            }
         });

         JLabel label = userDefinedAction.getIcon().orElse(MiscIcons.EMPTY).on(new JLabel(userDefinedAction.getEffectiveLabel()));
         label.setToolTipText(userDefinedAction.getEffectiveTooltip());

         addRow(gridBag, label, editButton, runButton, browseActionButton, deleteButton);
      }
   }

   private void addCallbacks(GridBag gridBag, UserDefinedPackage userDefinedPackage) {
      JButton addButton = newButton(MiscIcons.ADD, "Add new callback", _ -> {
         CallbackInfo callbackInfo = new CallbackInfo();
         boolean ok = editCallback(userDefinedPackage, callbackInfo, "Add callback");
         if (ok) {
            userDefinedPackage.info.callbacks.add(callbackInfo);
            userDefinedPackage.savePackageInfo();
            packagesConf.getChangeManager().notifyListeners();
         }
      });
      addHeader(gridBag, "Callbacks", addButton);

      GridBag localGridBag = new GridBag()
            .configureVerticalBox();
      gridBag.addWithLineBreak(localGridBag.getPanel());

      List<CallbackInfo> callbackInfos = userDefinedPackage.info.callbacks.stream()
            .sorted(Comparator.<CallbackInfo, String>comparing(c -> c.event).thenComparing(c -> c.actionId))
            .toList();
      for (CallbackInfo callbackInfo : callbackInfos) {
         JButton deleteButton = newButton(MiscIcons.DELETE, "Delete", _ -> {
            userDefinedPackage.info.callbacks.remove(callbackInfo);
            userDefinedPackage.savePackageInfo();
            packagesConf.getChangeManager().notifyListeners();
         });

         JButton editButton = newButton(MiscIcons.EDIT, "Edit", _ -> {
            boolean ok = editCallback(userDefinedPackage, callbackInfo, "Edit callback");
            if (ok) {
               userDefinedPackage.savePackageInfo();
               packagesConf.getChangeManager().notifyListeners();
            }
         });

         JLabel eventLabel = new JLabel(callbackInfo.event);
         eventLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
         JLabel actionLabel = userDefinedPackage.uiInfoToIcon(callbackInfo).orElse(MiscIcons.EMPTY).on(new JLabel(callbackInfo.actionId));
         actionLabel.setToolTipText(userDefinedPackage.uiInfoToToolTip(callbackInfo));

         addRow(localGridBag, List.of(eventLabel, actionLabel), editButton, deleteButton);
      }
   }

   private void addKeyStrokes(GridBag gridBag, UserDefinedPackage userDefinedPackage) {
      JButton addButton = newButton(MiscIcons.ADD, "Add new keystroke", _ -> {
         KeyStrokeInfo keyStrokeInfo = new KeyStrokeInfo();
         boolean ok = editKeyStrokeInfo(userDefinedPackage, keyStrokeInfo, "Add keystroke");
         if (ok) {
            userDefinedPackage.info.keyStrokes.addFirst(keyStrokeInfo);
            userDefinedPackage.savePackageInfo();
            userDefinedPackage.update();
            packagesConf.getChangeManager().notifyListeners();
         }
      });
      addHeader(gridBag, "Keystrokes", addButton);

      GridBag localGridBag = new GridBag()
            .configureVerticalBox();
      gridBag.addWithLineBreak(localGridBag.getPanel());

      List<KeyStrokeInfo> keyStrokeInfos = userDefinedPackage.info.keyStrokes.stream()
            .sorted(Utils.<KeyStrokeInfo>comparingIgnoringCase(c -> c.context)
                  .thenComparing(c -> c.keyStroke)
                  .thenComparing(c -> c.actionId))
            .toList();
      for (KeyStrokeInfo keyStrokeInfo : keyStrokeInfos) {
         JButton deleteButton = newButton(MiscIcons.DELETE, "Delete", _ -> {
            userDefinedPackage.info.keyStrokes.remove(keyStrokeInfo);
            userDefinedPackage.savePackageInfo();
            userDefinedPackage.update();
            packagesConf.getChangeManager().notifyListeners();
         });

         JButton editButton = newButton(MiscIcons.EDIT, "Edit", _ -> {
            boolean ok = editKeyStrokeInfo(userDefinedPackage, keyStrokeInfo, "Edit keystroke");
            if (ok) {
               userDefinedPackage.savePackageInfo();
               userDefinedPackage.update();
               packagesConf.getChangeManager().notifyListeners();
            }
         });

         JLabel contextLabel = new JLabel(keyStrokeInfo.context);
         JLabel keyStrokeLLabel = new JLabel(keyStrokeInfo.keyStroke);
         keyStrokeLLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
         JLabel actionLabel = userDefinedPackage.uiInfoToIcon(keyStrokeInfo).orElse(MiscIcons.EMPTY).on(new JLabel(keyStrokeInfo.actionId));
         actionLabel.setToolTipText(userDefinedPackage.uiInfoToToolTip(keyStrokeInfo));

         addRow(localGridBag, List.of(contextLabel, keyStrokeLLabel, actionLabel), editButton, deleteButton);
      }
   }

   private void addToolbarButtons(GridBag gridBag, UserDefinedPackage userDefinedPackage) {
      JButton addButton = newButton(MiscIcons.ADD, "Add new toolbar button", _ -> {
         ToolbarButtonInfo toolbarButtonInfo = new ToolbarButtonInfo();
         boolean ok = editToolbarButtonInfo(userDefinedPackage, toolbarButtonInfo, "Add toolbar button");
         if (ok) {
            userDefinedPackage.info.toolbarButtons.addFirst(toolbarButtonInfo);
            userDefinedPackage.savePackageInfo();
            packagesConf.getChangeManager().notifyListeners();
         }
      });
      addHeader(gridBag, "Toolbar buttons", addButton);

      for (ToolbarButtonInfo toolbarButtonInfo : userDefinedPackage.info.toolbarButtons) {
         JButton deleteButton = newButton(MiscIcons.DELETE, "Delete", _ -> {
            userDefinedPackage.info.toolbarButtons.remove(toolbarButtonInfo);
            userDefinedPackage.savePackageInfo();
            packagesConf.getChangeManager().notifyListeners();
         });

         JButton editButton = newButton(MiscIcons.EDIT, "Edit", _ -> {
            boolean ok = editToolbarButtonInfo(userDefinedPackage, toolbarButtonInfo, "Edit toolbar button");
            if (ok) {
               userDefinedPackage.savePackageInfo();
               packagesConf.getChangeManager().notifyListeners();
            }
         });

         JLabel label = newLabel(userDefinedPackage, toolbarButtonInfo, toolbarButtonInfo == dragItem);
         label.setToolTipText(userDefinedPackage.uiInfoToToolTip(toolbarButtonInfo));
         label.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
               label.removeMouseMotionListener(this);
               new Dragger(userDefinedPackage, toolbarButtonInfo, label, label, e);
            }
         });
         toolbarButtonInfoToComponent.put(toolbarButtonInfo, label);

         addRow(gridBag, label, editButton, deleteButton);
      }
   }

   private void addMenus(GridBag gridBag, UserDefinedPackage userDefinedPackage) {
      JButton addButton = newButton(MiscIcons.ADD, "Add new menu", _ -> {
         MenuItemInfo menuItemInfo = new MenuItemInfo();
         boolean ok = editMenuItemInfo(userDefinedPackage, menuItemInfo, "Add menu", true);
         if (ok) {
            userDefinedPackage.info.menus.addFirst(menuItemInfo);
            userDefinedPackage.savePackageInfo();
            packagesConf.getChangeManager().notifyListeners();
         }
      });
      addHeader(gridBag, "Menus", addButton);

      AtomicInteger row = new AtomicInteger();
      addMenuItemInfos(gridBag, 0, userDefinedPackage.info.menus, userDefinedPackage, false, row);
   }

   private void addMenuItemInfos(GridBag gridBag, int indent, List<MenuItemInfo> menuItemInfos, UserDefinedPackage userDefinedPackage,
                                 boolean parentIsDragged, AtomicInteger row) {
      for (int i = 0; i < menuItemInfos.size(); i++) {
         MenuItemInfo menuItemInfo = menuItemInfos.get(i);
         JButton deleteButton = newButton(MiscIcons.DELETE, "Delete", _ -> {
            menuItemInfos.remove(menuItemInfo);
            userDefinedPackage.savePackageInfo();
            packagesConf.getChangeManager().notifyListeners();
         });

         JButton editButton = newButton(MiscIcons.EDIT, "Edit", _ -> {
            String title = menuItemInfo.isMenu() ? "Edit menu" : "Edit menu item";
            boolean ok = editMenuItemInfo(userDefinedPackage, menuItemInfo, title, menuItemInfo.isMenu());
            if (ok) {
               userDefinedPackage.savePackageInfo();
               packagesConf.getChangeManager().notifyListeners();
            }
         });

         if (indent == 0 && i > 0) {
            gridBag.addWithLineBreak(Box.createVerticalStrut(5));
            gridBag.addWithLineBreak(new JSeparator());
            gridBag.addWithLineBreak(Box.createVerticalStrut(5));
         }

         GridBag subGridBag = new GridBag()
               .configureVerticalBox();
         gridBag.addWithLineBreak(subGridBag.getPanel());

         boolean isDragged = parentIsDragged || menuItemInfo == dragItem;

         JLabel label = newLabel(userDefinedPackage, menuItemInfo, isDragged);
         if (!menuItemInfo.isMenu()) {
            label.setToolTipText(userDefinedPackage.uiInfoToToolTip(menuItemInfo));
         }
         label.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
               label.removeMouseMotionListener(this);
               new Dragger(userDefinedPackage, menuItemInfo, label, subGridBag.getPanel(), e);
            }
         });
         menuItemInfoComponentToRow.put(label, row.getAndIncrement());

         JPanel panel = new JPanel(new BorderLayout());
         panel.add(label);
         if (indent > 0) {
            JLabel indentLabel = new JLabel("–");
            int labelWidth = 12;
            int leftBorder = indent * (16 + 4 + 5 + labelWidth) - labelWidth;
            indentLabel.setBorder(BorderFactory.createEmptyBorder(0, leftBorder, 0, 5));
            indentLabel.putClientProperty("InvisibleDuringDrag", true);
            panel.add(indentLabel, BorderLayout.WEST);
         }

         if (menuItemInfo.isMenu()) {
            JLabel menuLabel = MiscIcons.NAVIGATE_NEXT.on(new JLabel());
            menuLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
            menuLabel.putClientProperty("InvisibleDuringDrag", true);
            panel.add(menuLabel, BorderLayout.EAST);

            JButton addActionButton = newButton(MiscIcons.ADD, "Add action item", _ -> {
               MenuItemInfo newMenuItemInfo = new MenuItemInfo();
               boolean ok = editMenuItemInfo(userDefinedPackage, newMenuItemInfo, "Add action item", false);
               if (ok) {
                  menuItemInfo.items.addFirst(newMenuItemInfo);
                  userDefinedPackage.savePackageInfo();
                  packagesConf.getChangeManager().notifyListeners();
               }
            });
            addActionButton.setText("Action");

            JButton addSubmenuButton = newButton(MiscIcons.ADD, "Add submenu", _ -> {
               MenuItemInfo newMenuItemInfo = new MenuItemInfo();
               boolean ok = editMenuItemInfo(userDefinedPackage, newMenuItemInfo, "Add submenu", true);
               if (ok) {
                  menuItemInfo.items.addFirst(newMenuItemInfo);
                  userDefinedPackage.savePackageInfo();
                  packagesConf.getChangeManager().notifyListeners();
               }
            });
            addSubmenuButton.setText("Submenu");

            addRow(subGridBag, panel, addActionButton, addSubmenuButton, editButton, deleteButton);
         } else {
            addRow(subGridBag, panel, editButton, deleteButton);
         }

         if (!menuItemInfo.items.isEmpty()) {
            addMenuItemInfos(subGridBag, indent + 1, menuItemInfo.items, userDefinedPackage, isDragged, row);
         }
      }
   }

   private JButton newButton(SvgIcon icon, String toolTipText) {
      JButton button = icon.on(new JButton());
      button.setToolTipText(toolTipText);
      button.setMargin(margin);
      return button;
   }

   private JButton newButton(SvgIcon icon, String toolTipText, ActionListener actionListener) {
      JButton button = newButton(icon, toolTipText);
      button.addActionListener(actionListener);
      return button;
   }

   private static JLabel newLabel(UserDefinedPackage userDefinedPackage, UiItemInfo uiItemInfo, boolean isDragged) {
      JLabel label = new JLabel(userDefinedPackage.uiInfoToEffectiveText(uiItemInfo)) {
         @Override
         protected void paintComponent(Graphics g) {
            if (isDragged) {
               g.setColor(Color.GRAY);
               g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
               return;
            }
            super.paintComponent(g);
         }
      };
      label.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      return userDefinedPackage.uiInfoToIcon(uiItemInfo).orElse(MiscIcons.EMPTY).on(label);
   }

   private static void addHeader(GridBag gridBag, String title, JComponent... components) {
      gridBag.addWithLineBreak(Box.createVerticalStrut(10));
      gridBag.addWithLineBreak(new JSeparator());

      JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
      header.add(new JLabel("<html><h2>" + title + "</h2>"));
      header.add(Box.createHorizontalStrut(10));
      for (int i = 0; i < components.length; i++) {
         if (i > 0) {
            header.add(Box.createHorizontalStrut(5));
         }
         header.add(components[i]);
      }
      gridBag.addWithLineBreak(header);
   }

   private void addRow(GridBag gridBag, JComponent mainComponent, JButton... buttons) {
      addRow(gridBag, List.of(mainComponent), buttons);
   }

   private void addRow(GridBag gridBag, List<JComponent> mainComponents, JButton... buttons) {
      AtomicBoolean buttonsVisible = new AtomicBoolean();
      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0)) {
         @Override
         protected void paintChildren(Graphics g) {
            if (buttonsVisible.get() && dragItem == null) {
               super.paintChildren(g);
            }
         }
      };
      Consumer<Component> deepInputListener = component -> new DeepInputListener(component, new MouseAndKeyAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            setInside(true);
         }

         @Override
         public void mouseExited(MouseEvent e) {
            setInside(false);
         }

         private void setInside(boolean inside) {
            buttonsVisible.set(inside);
            buttonPanel.repaint();
         }
      });
      mainComponents.forEach(deepInputListener);
      deepInputListener.accept(buttonPanel);
      buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
      for (JButton button : buttons) {
         button.setFocusable(false);
         buttonPanel.add(button);
      }
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(mainComponents.getLast(), BorderLayout.WEST);
      panel.add(buttonPanel);
      gridBag.getConstraints().gridwidth = 1;
      gridBag.getConstraints().weightx = 0;
      for (int i = 0; i < mainComponents.size() - 1; i++) {
         gridBag.add(mainComponents.get(i));
      }
      gridBag.getConstraints().weightx = 1;
      gridBag.addWithLineBreak(panel);
   }

   private static boolean hasUiItems(UserDefinedPackage userDefinedPackage, String actionId) {
      return Stream.concat(userDefinedPackage.info.toolbarButtons.stream(), UserDefinedPackage.uiMenuItemInfos(userDefinedPackage.info.menus))
            .anyMatch(menuItemInfo -> menuItemInfo.actionId.equals(actionId));
   }

   private static void deleteUiItems(UserDefinedPackage userDefinedPackage, String actionId) {
      userDefinedPackage.info.toolbarButtons.removeIf(menuItemInfo -> menuItemInfo.actionId.equals(actionId));
      deleteUiItems(userDefinedPackage.info.menus, actionId);
   }

   private static void deleteUiItems(List<MenuItemInfo> menuItemInfos, String actionId) {
      menuItemInfos.removeIf(menuItemInfo -> menuItemInfo.actionId.equals(actionId));
      menuItemInfos.forEach(menuItemInfo -> deleteUiItems(menuItemInfo.items, actionId));
   }

   private static void updateUiItems(UserDefinedPackage userDefinedPackage, String oldActionId, String newActionId) {
      Stream.concat(userDefinedPackage.info.toolbarButtons.stream(), UserDefinedPackage.uiMenuItemInfos(userDefinedPackage.info.menus))
            .filter(menuItemInfo -> menuItemInfo.actionId.equals(oldActionId))
            .forEach(menuItemInfo -> menuItemInfo.actionId = newActionId);
   }

   private void createExamplePackage() {
      String id = "lsssExample";
      Path packageDir = packagesConf.getPackagesDir().resolve(id);
      if (Files.exists(packageDir)) {
         int answer = JOptionPane.showConfirmDialog(scrollPane,
               "The package \"" + id + "\" already exists:"
                     + "\n" + packageDir
                     + "\n"
                     + "\nOverwrite existing package?"
                     + "\n"
                     + "\n",
               "Overwrite existing package?", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
         if (answer != JOptionPane.OK_OPTION) {
            return;
         }
      }
      Path sourceDir = LSSS.getInstallationDir().resolve("data").resolve("packages").resolve(id);
      new WorkerDialog(scrollPane, "Copying files to\n" + sourceDir)
            .start(asyncHandle -> {
               FileUtils.copyRecursively(sourceDir, packageDir, Utils.emptyConsumer(), asyncHandle);
               packagesConf.reloadPackages();
               UserDefinedPackage examplePackage = packagesConf.getUserDefinedPackage(id);
               packagesConf.setCurrentUserDefinedPackage(examplePackage);
            });
   }

   private void importPackage() {
      Path importDir = getDefaultExportImportDir();
      JFileChooser fileChooser = new JFileChooser(importDir.toFile());
      GuiUtils.setFileListTransferHandler(fileChooser);
      fileChooser.setFileFilter(new SuffixFileFilter("Zip files", ".zip"));
      int returnValue = fileChooser.showOpenDialog(scrollPane);
      if (returnValue != JFileChooser.APPROVE_OPTION) {
         return;
      }
      Path zipFile = fileChooser.getSelectedFile().toPath();
      setDefaultExportImportDir(zipFile.getParent());
      String packageId = FileUtils.baseName(zipFile);
      Path packageDir = packagesConf.getPackagesDir().resolve(packageId);
      if (Files.exists(packageDir)) {
         int answer = GuiUtils.showOptionDialog(scrollPane, "Question", "Replace existing package?\n" + packageDir, new String[]{"Replace", "Cancel"});
         if (answer != 0) {
            return;
         }
      }
      WorkerDialog.Result result = new WorkerDialog(scrollPane, "Importing package from zip file\n" + zipFile)
            .start(_ -> {
               FileUtils.deleteRecursively(packageDir);
               try (InputStream in = FileUtils.newBufferedInputStream(zipFile);
                    ZipInputStream zipInputStream = new ZipInputStream(in)) {
                  FileUtils.unzip(zipInputStream, packageDir);
                  packagesConf.reloadPackages();
                  UserDefinedPackage importedPackage = packagesConf.getUserDefinedPackage(packageId);
                  packagesConf.setCurrentUserDefinedPackage(importedPackage);
               }
            });
      if (result.success()) {
         GuiUtils.showOptionDialog(scrollPane, "Message", "Package import done.", new String[]{"OK"});
      }
   }

   private void exportPackage(UserDefinedPackage userDefinedPackage) {
      Path packageDir = userDefinedPackage.getDir();
      Path exportDir = getDefaultExportImportDir();
      JFileChooser fileChooser = new JFileChooser();
      GuiUtils.setFileListTransferHandler(fileChooser);
      fileChooser.setSelectedFile(exportDir.resolve(packageDir.getFileName() + ".zip").toFile());
      int returnValue = fileChooser.showSaveDialog(scrollPane);
      if (returnValue != JFileChooser.APPROVE_OPTION) {
         return;
      }
      Path zipFile = fileChooser.getSelectedFile().toPath();
      setDefaultExportImportDir(zipFile.getParent());
      if (Files.exists(zipFile)) {
         int answer = GuiUtils.showOptionDialog(scrollPane, "Question", "Overwrite existing file?\n" + zipFile, new String[]{"Overwrite", "Cancel"});
         if (answer != 0) {
            return;
         }
      }
      WorkerDialog.Result result = new WorkerDialog(scrollPane, "Exporting package to zip file\n" + zipFile)
            .start(asyncHandle -> {
               try (OutputStream out = Files.newOutputStream(zipFile);
                    ZipOutputStream zipOutputStream = new ZipOutputStream(out)) {
                  FileUtils.zip("", zipOutputStream, packageDir, asyncHandle, _ -> true);
               }
            });
      if (result.success()) {
         int answer = GuiUtils.showOptionDialog(scrollPane, "Message", "Package export done. Open folder?", new String[]{"Yes", "No"});
         if (answer == 0) {
            GuiUtils.desktopOpen(zipFile.getParent(), scrollPane);
         }
      }
   }

   private Path getDefaultExportImportDir() {
      return Path.of(packagesConf.getConfigurationManager().getPreferences().get("packageExportImportDir", Utils.getUserHome().toString()));
   }

   private void setDefaultExportImportDir(Path dir) {
      packagesConf.getConfigurationManager().getPreferences().put("packageExportImportDir", dir.toString());
   }

   private boolean editPackage(UserDefinedPackage userDefinedPackage, String title) {
      UserDefinedPackageEditor editor = new UserDefinedPackageEditor(userDefinedPackage);
      ParameterEditor parameterEditor = new ParameterEditor(editor.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
      parameterEditor.getGUIConfig().setCombineInputAndDescription(true);

      BaseParameter<?> focusParameter = userDefinedPackage.id.isEmpty() ? editor.id : editor.label;
      JComponent focusComponent = parameterEditor.getInputComponent(focusParameter);
      SwingUtilities.invokeLater(focusComponent::requestFocusInWindow);

      boolean ok = new ConfigurableGUIDialog(scrollPane, title, new ParameterCollection(editor))
            .setCloseOnOk(() -> editor.isOK(parameterEditor))
            .setGUI(parameterEditor.getEditorComponent())
            .setMinimumSize(500, 0)
            .show();
      if (ok) {
         editor.apply();
      }
      return ok;
   }

   private boolean editAction(UserDefinedAction userDefinedAction, String title) {
      UserDefinedActionEditor editor = new UserDefinedActionEditor(userDefinedAction);
      ParameterEditor parameterEditor = new ParameterEditor(editor.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
      parameterEditor.getGUIConfig().setCombineInputAndDescription(true);
      editor.init(parameterEditor);

      JButton runButton = MiscIcons.PLAY.on(new JButton("Run"));
      runButton.setToolTipText("Save and run " + UserDefinedAction.MAIN_PY);
      runButton.setVisible(!userDefinedAction.id.isEmpty());

      BaseParameter<?> focusParameter = userDefinedAction.id.isEmpty() ? editor.id : editor.code;
      JComponent focusComponent = parameterEditor.getInputComponent(focusParameter);
      SwingUtilities.invokeLater(focusComponent::requestFocusInWindow);

      String codeBackup = editor.code.getValue();

      boolean ok = new ConfigurableGUIDialog(scrollPane, title, new ParameterCollection(editor))
            .setMinimumSize(800, 600)
            .setCloseOnOk(() -> editor.isOK(parameterEditor))
            .accessDialog(dialog -> {
               runButton.addActionListener(ae -> {
                  Optional<Map<String, Object>> input = UserDefinedUtils.showInputDialog(userDefinedAction, editor.getInputParameters(), runButton);
                  if (input.isEmpty()) {
                     return;
                  }
                  ActionArgument argument = new ActionArgument(ae, input.get());
                  Path mainFile = userDefinedAction.getMainFile();
                  try {
                     FileUtils.replaceFileSafely(mainFile, editor.code.getValue(), Utils.UTF_8);
                     packagesConf.executeActionScript(() -> dialog, userDefinedAction, argument, editor.showDialog.getBooleanValue());
                  } catch (IOException e) {
                     Log.global.log(Level.WARNING, "Error writing to " + mainFile, e);
                  }
               });
            })
            .extraButton(runButton)
            .setNoScrollGUI(parameterEditor.getEditorComponent())
            .show();
      if (ok) {
         editor.apply();
      } else {
         if (!userDefinedAction.id.isEmpty()) {
            Path mainFile = userDefinedAction.getMainFile();
            try {
               FileUtils.replaceFileSafely(mainFile, codeBackup, Utils.UTF_8);
            } catch (IOException e) {
               Log.global.log(Level.WARNING, "Error writing to " + mainFile, e);
            }
         }
      }
      return ok;
   }

   private boolean editCallback(UserDefinedPackage userDefinedPackage, CallbackInfo callbackInfo, String title) {
      CallbackInfoEditor editor = new CallbackInfoEditor(userDefinedPackage, callbackInfo);
      ParameterEditor parameterEditor = new ParameterEditor(editor.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
      editor.init(parameterEditor);
      boolean ok = new ConfigurableGUIDialog(scrollPane, title, new ParameterCollection(editor))
            .setCloseOnOk(() -> editor.isOK(parameterEditor))
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (ok) {
         editor.apply();
      }
      return ok;
   }

   private boolean editKeyStrokeInfo(UserDefinedPackage userDefinedPackage, KeyStrokeInfo keyStrokeInfo, String title) {
      KeyStrokeInfoEditor editor = new KeyStrokeInfoEditor(userDefinedPackage, keyStrokeInfo);
      ParameterEditor parameterEditor = new ParameterEditor(editor.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
      editor.init(parameterEditor);
      boolean ok = new ConfigurableGUIDialog(scrollPane, title, new ParameterCollection(editor))
            .setCloseOnOk(() -> editor.isOK(parameterEditor))
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (ok) {
         editor.apply();
      }
      return ok;
   }

   private boolean editToolbarButtonInfo(UserDefinedPackage userDefinedPackage, ToolbarButtonInfo toolbarButtonInfo, String title) {
      ToolbarButtonInfoEditor editor = new ToolbarButtonInfoEditor(userDefinedPackage, toolbarButtonInfo);
      ParameterEditor parameterEditor = new ParameterEditor(editor.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
      editor.init(parameterEditor);
      boolean ok = new ConfigurableGUIDialog(scrollPane, title, new ParameterCollection(editor))
            .setCloseOnOk(() -> editor.isOK(parameterEditor))
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (ok) {
         editor.apply();
      }
      return ok;
   }

   private boolean editMenuItemInfo(UserDefinedPackage userDefinedPackage, MenuItemInfo menuItemInfo, String title, boolean isMenu) {
      MenuItemInfoEditor editor = new MenuItemInfoEditor(userDefinedPackage, menuItemInfo, isMenu);
      ParameterEditor parameterEditor = new ParameterEditor(editor.getParameters());
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setTextAlignment(GUIConfig.Alignment.LEFT);
      editor.init(parameterEditor);
      boolean ok = new ConfigurableGUIDialog(scrollPane, title, new ParameterCollection(editor))
            .setCloseOnOk(() -> editor.isOK(parameterEditor))
            .setGUI(parameterEditor.getEditorComponent())
            .show();
      if (ok) {
         editor.apply();
      }
      return ok;
   }

   private final class Dragger {
      private final UserDefinedPackage userDefinedPackage;
      private final JLabel label;
      private final JComponent dragComponent;
      private final Point grabOffset;
      private final JPanel glassPane = new JPanel(null);

      private Dragger(UserDefinedPackage userDefinedPackage, UiItemInfo uiItemInfo, JLabel label, JComponent dragComponent, MouseEvent mouseEvent) {
         this.userDefinedPackage = userDefinedPackage;
         this.label = label;
         this.dragComponent = dragComponent;
         grabOffset = SwingUtilities.convertPoint(label, mouseEvent.getPoint(), dragComponent);

         if (dragItem != null) {
            return;
         }
         dragItem = uiItemInfo;

         GuiUtils.hierarchyStream(dragComponent)
               .gather(Utils.allOfType(JComponent.class))
               .forEach(component -> {
                  component.setOpaque(false);
                  if (component.getClientProperty("InvisibleDuringDrag") != null) {
                     component.setForeground(new Color(0, true));
                     ((JLabel) component).setIcon(null);
                  }
               });

         JDialog dialog = packagesConf.getConfigurationManager().getDialog();
         Component originalGlassPane = dialog.getGlassPane();
         boolean originalGlassPaneVisible = originalGlassPane.isVisible();

         dragComponent.getParent().remove(dragComponent);
         glassPane.add(dragComponent);
         glassPane.setOpaque(false);
         dialog.setGlassPane(glassPane);
         glassPane.setVisible(true);
         glassPane.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));

         drag();

         MouseAdapter listener = new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
               drag();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
               dialog.setGlassPane(originalGlassPane);
               originalGlassPane.setVisible(originalGlassPaneVisible);
               dragItem = null;
               userDefinedPackage.savePackageInfo();
               packagesConf.getChangeManager().notifyListeners();
            }
         };
         label.addMouseMotionListener(listener);
         label.addMouseListener(listener);
         update();
      }

      private void drag() {
         Point glassPanePoint = MouseInfo.getPointerInfo().getLocation();
         SwingUtilities.convertPointFromScreen(glassPanePoint, glassPane);
         dragComponent.setLocation(glassPanePoint.x - grabOffset.x, glassPanePoint.y - grabOffset.y);
         glassPane.repaint();

         if (dragItem instanceof ToolbarButtonInfo toolbarButtonInfo) {
            for (Map.Entry<ToolbarButtonInfo, JComponent> entry : toolbarButtonInfoToComponent.entrySet()) {
               JComponent component = entry.getValue();
               Point p = SwingUtilities.convertPoint(component, new Point(), glassPane);
               if (glassPanePoint.y >= p.y && glassPanePoint.y <= p.y + component.getHeight()) {
                  List<ToolbarButtonInfo> toolbarButtons = userDefinedPackage.info.toolbarButtons;
                  int i = toolbarButtons.indexOf(entry.getKey());
                  int j = toolbarButtons.indexOf(toolbarButtonInfo);
                  if (i != j) {
                     Collections.swap(toolbarButtons, i, j);
                     packagesConf.getChangeManager().notifyListeners();
                  }
                  break;
               }
            }
         }
         if (dragItem instanceof MenuItemInfo menuItemInfo) {
            for (Map.Entry<JComponent, Integer> entry : menuItemInfoComponentToRow.entrySet()) {
               JComponent component = entry.getKey();
               Point p = SwingUtilities.convertPoint(component, new Point(), glassPane);
               if (glassPanePoint.y >= p.y && glassPanePoint.y <= p.y + component.getHeight()) {
                  Point labelPoint = SwingUtilities.convertPoint(label, new Point(), mainPanel);
                  int minIndent = menuItemInfo.isMenu() ? 0 : 1;
                  int targetIndent = Math.max(minIndent, labelPoint.x / 25);

                  List<MenuItemInfo> parentItems = getParentItems(userDefinedPackage.info.menus, menuItemInfo);
                  if (parentItems != null) {
                     int index = parentItems.indexOf(menuItemInfo);
                     parentItems.remove(menuItemInfo);
                     boolean didAdd = tryAdd(userDefinedPackage.info.menus, menuItemInfo, new AtomicInteger(), entry.getValue(), 0, targetIndent);
                     if (!didAdd) {
                        userDefinedPackage.info.menus.add(menuItemInfo);
                     }
                     if (parentItems.indexOf(menuItemInfo) != index) {
                        packagesConf.getChangeManager().notifyListeners();
                     }
                  }
                  break;
               }
            }
         }
      }

      private static @Nullable List<MenuItemInfo> getParentItems(List<MenuItemInfo> menuItemInfos, MenuItemInfo child) {
         for (MenuItemInfo menuItemInfo : menuItemInfos) {
            if (menuItemInfo == child) {
               return menuItemInfos;
            }
            if (menuItemInfo.isMenu()) {
               List<MenuItemInfo> parentItems = getParentItems(menuItemInfo.items, child);
               if (parentItems != null) {
                  return parentItems;
               }
            }
         }
         return null;
      }

      private static boolean tryAdd(List<MenuItemInfo> menuItemInfos, MenuItemInfo toBeAdded, AtomicInteger row, int targetRow, int indent, int targetIndent) {
         for (int i = 0; i < menuItemInfos.size(); i++) {
            MenuItemInfo menuItemInfo = menuItemInfos.get(i);
            if (targetRow == row.getAndIncrement()) {
               if (indent == 0 && !toBeAdded.isMenu()) {
                  menuItemInfo.items.addFirst(toBeAdded);
               } else {
                  menuItemInfos.add(i, toBeAdded);
               }
               return true;
            }
            if (menuItemInfo.isMenu()) {
               boolean didAdd = tryAdd(menuItemInfo.items, toBeAdded, row, targetRow, indent + 1, targetIndent);
               if (didAdd) {
                  return true;
               }
            }
         }
         if (indent <= targetIndent && targetRow == row.get()) {
            menuItemInfos.add(toBeAdded);
            return true;
         }
         return false;
      }
   }
}
