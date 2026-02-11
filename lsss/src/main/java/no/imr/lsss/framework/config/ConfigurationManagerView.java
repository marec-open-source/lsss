package no.imr.lsss.framework.config;

import no.imr.lsss.LSSS;
import no.imr.lsss.util.ApiMenuBuilder;
import no.imr.lsss.viewer.Shortcuts;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpID;
import no.imr.tools.misc.TextFilter;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.PreferredSizeLayout;
import no.imr.tools.swing.SimpleDocumentListener;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.icons.AbstractIcon;
import no.imr.tools.swing.svg.SvgIcon;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import java.util.stream.Stream;

final class ConfigurationManagerView implements ViewHolder.View {
   private static final Dimension DEFAULT_DIALOG_SIZE = new Dimension(950, 620);
   private static final int DEFAULT_TREE_PANEL_WIDTH = 200;
   private static final String PREFERENCE_DIALOG_SIZE = "dialogSize";
   private static final String PREFERENCE_TREE_WIDTH = "treeWidth";

   private final ConfigurationManager configurationManager;

   private final JDialog dialog;
   private final JSplitPane splitPane;
   private final JTree tree;
   private final ConfigurationNode rootNode;
   private final JPanel configurePanel = new JPanel(new BorderLayout());
   private final JComboBox<UserProfile> userProfileComboBox = new JComboBox<>(UserProfile.values());
   private final JTextField filterTextField = new JTextField("", 20);
   private final FilterGlassPane filterGlassPane = new FilterGlassPane();
   private final Map<ConfigurationNode, Component> filterComponentCache = new HashMap<>();
   private final JButton helpButton = new JButton("Help");
   private @Nullable DeepInputListener configurationPanelDeepInputListener;
   private boolean isSettingTreePath;

   private @Nullable ConfigurationUnit currentConfigurationUnit;
   private @Nullable Element backupConfiguration;

   ConfigurationManagerView(ConfigurationManager configurationManager) {
      this.configurationManager = configurationManager;
      LSSS lsss = configurationManager.getLsssConfiguration().getLSSS();
      dialog = new JDialog(lsss.getFrame(), "Configuration", Dialog.ModalityType.DOCUMENT_MODAL);

      rootNode = new ConfigurationNode(configurationManager.getLsssConfiguration());
      currentConfigurationUnit = configurationManager.getDataConf();

      DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
      tree = new JTree(treeModel);
      tree.setBorder(GuiUtils.DEFAULT_MARGIN);
      tree.setExpandsSelectedPaths(true);
      tree.setScrollsOnExpand(true);
      ToolTipManager.sharedInstance().registerComponent(tree);
      tree.addTreeSelectionListener(_ -> {
         if (!isSettingTreePath) {
            updateConfigurationPanel();
         }
      });

      tree.setCellRenderer(new DefaultTreeCellRenderer() {
         @Override
         public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            if (value instanceof ConfigurationNode node) {
               setToolTipText(node.getConfigurationUnit().getDescription());
               SvgIcon confUnitIcon = node.getConfigurationUnit().getIcon();
               if (confUnitIcon != null) {
                  Image image = confUnitIcon.getImage();
                  Icon icon = getIcon();
                  setIcon(new AbstractIcon(icon.getIconWidth(), icon.getIconHeight()) {
                     @Override
                     protected void paintIcon(Component c, Graphics2D g, int x, int y) {
                        x += (getIconWidth() - image.getWidth(null)) / 2;
                        y += (getIconHeight() - image.getHeight(null)) / 2;
                        g.drawImage(image, x, y, null);
                     }
                  });
               }
            }
            return this;
         }
      });

      tree.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE && e.isControlDown() && e.isAltDown()) {
               configurationManager.setUserProfile(Utils.shift(configurationManager.getUserProfile(), 1));
            }
         }
      });

      tree.addMouseListener(new PopupMenuMouseListener(mouseEvent -> {
         int row = tree.getRowForLocation(mouseEvent.getX(), mouseEvent.getY());
         if (row < 0) {
            return null;
         }
         ConfigurationUnit configurationUnit = ((ConfigurationNode) tree.getPathForRow(row).getLastPathComponent()).configurationUnit;
         if (configurationUnit == configurationManager.getLsssConfiguration()) {
            return null;
         }
         JPopupMenu popupMenu = new JPopupMenu();
         new ApiMenuBuilder(configurationUnit.getLSSS(), dialog)
               .configurationUnitMenu(configurationUnit)
               .addTo(popupMenu);
         return popupMenu;
      }));

      setTreePath(currentConfigurationUnit);

      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), configurePanel);
      splitPane.setBorder(BorderFactory.createEmptyBorder());
      splitPane.setResizeWeight(0);

      JPanel bottomButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

      JButton okButton = new JButton("OK");
      bottomButtonsPanel.add(okButton);
      okButton.setToolTipText("Accept changes to configuration");
      okButton.addActionListener(_ -> configurationManager.ok());

      JButton cancelButton = new JButton("Cancel");
      cancelButton.addActionListener(_ -> cancel());
      bottomButtonsPanel.add(cancelButton);
      cancelButton.setToolTipText("Undo changes to configuration");
      GuiUtils.setAccelerator(cancelButton, Shortcuts.ESCAPE, () -> {
         if (filterTextField.getText().isEmpty()) {
            cancel();
         } else {
            filterTextField.setText("");
            filterTextField.requestFocusInWindow();
         }
      });

      helpButton.addActionListener(_ -> {
         for (ConfigurationNode node = (ConfigurationNode) tree.getLastSelectedPathComponent(); node != null; node = (ConfigurationNode) node.getParent()) {
            HelpID helpID = node.getConfigurationUnit().getHelpID();
            if (helpID.isValid()) {
               helpID.show();
               return;
            }
         }
         configurationManager.getLsssConfiguration().getHelpID().show();
      });
      bottomButtonsPanel.add(helpButton);
      GuiUtils.setAccelerator(helpButton, Shortcuts.HELP);

      userProfileComboBox.setSelectedItem(configurationManager.getUserProfile());
      userProfileComboBox.addItemListener(e -> {
         if (e.getStateChange() == ItemEvent.SELECTED) {
            configurationManager.setUserProfile((UserProfile) e.getItem());
         }
      });

      JPanel userProfilePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
      userProfilePanel.add(new JLabel("Access level: "));
      userProfilePanel.add(userProfileComboBox);

      filterTextField.getDocument().addDocumentListener(new SimpleDocumentListener(_ -> {
         TreePath selectionPath = tree.getSelectionPath();
         String filterText = filterTextField.getText();
         TextFilter filter = new TextFilter(filterText);
         filterGlassPane.setVisible(!filterText.isEmpty());
         rootNode.filter(filter, filterComponentCache);
         treeModel.reload();
         if (filterText.isEmpty()) {
            filterComponentCache.clear();
            tree.expandRow(2);
            tree.expandRow(1);
            if (configurationPanelDeepInputListener != null) {
               configurationPanelDeepInputListener.stop();
               configurationPanelDeepInputListener = null;
            }
         } else {
            for (int i = 0; i < tree.getRowCount(); i++) {
               tree.expandRow(i);
            }
            if (configurationPanelDeepInputListener == null) {
               // Only deep input listener when filtering. Otherwise, parameter copy/paste popup menu does not work.
               configurationPanelDeepInputListener = new DeepInputListener(configurePanel, new ConfigurationPanelDeepInputListener());
            }
         }
         if (filterText.isEmpty() || selectionPath != null && ((ConfigurationNode) selectionPath.getLastPathComponent()).getParent() != null) {
            tree.setSelectionPath(selectionPath);
         } else {
            rootNode.getAttachedNodesRecursively()
                  .filter(ConfigurationNode::isLeaf)
                  .findFirst()
                  .map(node -> new TreePath(node.getPath()))
                  .ifPresent(tree::setSelectionPath);
         }
         updateConfigurationPanel();
      }));

      JLabel filterLabel = new JLabel("Filter: ");
      filterLabel.setToolTipText("<html>Displays only matching configuration<br>Prefix a word with - to exclude");
      filterLabel.setDisplayedMnemonic(KeyEvent.VK_F);
      GuiUtils.setAccelerator(filterLabel, KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.ALT_DOWN_MASK), filterTextField::requestFocusInWindow);

      JPanel filterPanel = new JPanel(new GridBagLayout());
      GridBagConstraints filterPanelConstraints = new GridBagConstraints();
      filterPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
      filterPanelConstraints.weightx = 1;
      filterPanel.add(filterLabel);
      filterPanel.add(filterTextField, filterPanelConstraints);
      JPanel wrappedFilterPanel = PreferredSizeLayout.wrap(filterPanel, PreferredSizeLayout.HorizontalAlignment.CENTER);

      JPanel bottomPanel = new JPanel(new BorderLayout());
      bottomPanel.add(userProfilePanel, BorderLayout.WEST);
      bottomPanel.add(wrappedFilterPanel);
      bottomPanel.add(bottomButtonsPanel, BorderLayout.EAST);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(splitPane);
      mainPanel.add(bottomPanel, BorderLayout.SOUTH);

      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      dialog.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            cancel();
         }
      });
      dialog.getContentPane().add(mainPanel);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.setSize(DEFAULT_DIALOG_SIZE);
      GuiUtils.syncSize(dialog, configurationManager.getPreferences(), PREFERENCE_DIALOG_SIZE);
      dialog.setLocationRelativeTo(lsss.getFrame());
      filterGlassPane.setVisible(false);
      dialog.setGlassPane(filterGlassPane);

      splitPane.setDividerLocation(configurationManager.getPreferences().getInt(PREFERENCE_TREE_WIDTH, DEFAULT_TREE_PANEL_WIDTH));
   }

   private void updatePreferences() {
      configurationManager.getPreferences().putInt(PREFERENCE_TREE_WIDTH, splitPane.getDividerLocation());
   }

   @Override
   public JComponent getComponent() {
      throw new UnsupportedOperationException();
   }

   void updateUserProfile() {
      userProfileComboBox.setSelectedItem(configurationManager.getUserProfile());
      updateConfigurationPanel();
   }

   private void setTreePath(ConfigurationUnit configurationUnit) {
      isSettingTreePath = true;
      rootNode.getAttachedNodesRecursively()
            .filter(node -> node.getConfigurationUnit() == configurationUnit)
            .findFirst()
            .ifPresentOrElse(node -> {
               TreePath path = new TreePath(node.getPath());
               tree.setSelectionPath(path);
               tree.scrollPathToVisible(path);
            }, tree::clearSelection);
      isSettingTreePath = false;
   }

   JDialog getDialog() {
      return dialog;
   }

   void showDialog(ConfigurationUnit configurationUnit) {
      setTreePath(configurationUnit);
      showDialog();
   }

   void showDialog() {
      updateConfigurationPanel();
      openDialog();
   }

   void updateConfigurationPanel() {
      if (currentConfigurationUnit != null && !currentConfigurationUnit.stopEditing()) {
         setTreePath(currentConfigurationUnit);
         return;
      }

      configurePanel.removeAll();

      if (tree.getLastSelectedPathComponent() instanceof ConfigurationNode configurationNode) {
         currentConfigurationUnit = configurationNode.getConfigurationUnit();
         helpButton.setToolTipText("Show help for " + currentConfigurationUnit.getDisplayName());
         JComponent component = currentConfigurationUnit.getComponent();
         configurePanel.add(component);
         if (!filterComponentCache.isEmpty()) {
            filterComponentCache.put(configurationNode, component);
         }
      } else {
         currentConfigurationUnit = null;
         helpButton.setToolTipText("Show help");
         configurePanel.add(new JLabel("Select configuration element in the tree to edit its settings", SwingConstants.CENTER));
      }

      configurePanel.validate();
      configurePanel.repaint();

      filterGlassPane.setVisible(!filterTextField.getText().isEmpty());
   }

   private void cancel() {
      if (!dialog.isVisible()) {
         // See ticket #635. Can happen if e.g. user presses escape and click cancel at the same time.
         return;
      }

      configurationManager.getLsssConfiguration().applyRecursively(ConfigurationUnit::cancelled);
      if (backupConfiguration != null) {
         configurationManager.getLsssConfiguration().fromXml(backupConfiguration);
      }
      closeDialog();
   }

   void closeDialog() {
      ToolTipManager.sharedInstance().unregisterComponent(tree);
      dialog.setVisible(false);
      backupConfiguration = null;
      filterTextField.setText("");
      updatePreferences();
      configurationManager.getLsssConfiguration().applyRecursively(ConfigurationUnit::removeView);
   }

   private void openDialog() {
      if (dialog.isVisible()) {
         return;
      }
      configurationManager.getLsssConfiguration().applyRecursively(ConfigurationUnit::opened);
      backupConfiguration = configurationManager.getLsssConfiguration().toXml();
      dialog.setVisible(true);
   }

   private static boolean filterAcceptsComponentHierarchy(Component component, TextFilter filter) {
      return GuiUtils.hierarchyStream(component)
            .filter(Component::isVisible)
            .anyMatch(c -> filterAcceptsComponent(c, filter));
   }

   private static boolean filterAcceptsComponent(Component component, TextFilter filter) {
      if (component instanceof JTabbedPane tabbedPane) {
         for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (filterAcceptsTabTitle(tabbedPane, i, filter)) {
               return true;
            }
         }
      }
      String text = GuiUtils.getComponentPlainText(component);
      return filter.test(text);
   }

   private static boolean filterAcceptsTabTitle(JTabbedPane tabbedPane, int tabIndex, TextFilter filter) {
      if (filter.test(tabbedPane.getTitleAt(tabIndex))) {
         return true;
      }
      Component tabComponent = tabbedPane.getTabComponentAt(tabIndex);
      return tabComponent != null && filterAcceptsComponentHierarchy(tabComponent, filter);
   }

   private final class ConfigurationPanelDeepInputListener extends MouseAndKeyAdapter {
      private ConfigurationPanelDeepInputListener() {
      }

      @Override
      public void focusGained(FocusEvent e) {
         filterGlassPane.setVisible(false);
         filterComponentCache.clear();
      }

      @Override
      public void focusLost(FocusEvent e) {
         filterGlassPane.setVisible(!filterTextField.getText().isEmpty());
      }
   }

   private final class FilterGlassPane extends JComponent {
      private FilterGlassPane() {
      }

      @Override
      protected void paintComponent(Graphics g) {
         super.paintComponent(g);

         drawFilter((Graphics2D) g);
      }

      private void drawFilter(Graphics2D g) {
         TextFilter filterText = new TextFilter(filterTextField.getText());
         List<Rectangle> rectangles = new ArrayList<>();
         addRectangles(configurePanel, filterText, rectangles);
         if (rectangles.isEmpty()) {
            return;
         }
         List<RoundRectangle2D.Float> roundedRectangles = rectangles.stream()
               .map(rect -> new RoundRectangle2D.Float(rect.x, rect.y, rect.width, rect.height, 6, 6))
               .toList();
         Area area = new Area(SwingUtilities.convertRectangle(configurePanel.getParent(), configurePanel.getBounds(), getParent()));
         roundedRectangles.forEach(rect -> area.subtract(new Area(rect)));
         g.setColor(new Color(0, 0, 0, 63));
         g.fill(area);
         g.setColor(ColorUtils.GOLD);
         g.setStroke(GuiUtils.STROKE_2);
         Shape originalClip = g.getClip();
         g.setClip(configurePanel.getBounds());
         roundedRectangles.forEach(g::draw);
         g.setClip(originalClip);
      }

      private void addRectangles(Component component, TextFilter filter, List<Rectangle> rectangles) {
         if (!component.isVisible()) {
            return;
         }
         if (component instanceof JTabbedPane tabbedPane) {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
               boolean contentsAccepted = filterAcceptsComponentHierarchy(tabbedPane.getComponent(i), filter);
               if (contentsAccepted && tabbedPane.getSelectedIndex() == i) {
                  addRectangles(tabbedPane.getComponentAt(i), filter, rectangles);
               }
               if (contentsAccepted || filterAcceptsTabTitle(tabbedPane, i, filter)) {
                  Rectangle rect = SwingUtilities.convertRectangle(tabbedPane.getParent(), tabbedPane.getBoundsAt(i), getParent());
                  rectangles.add(new Rectangle(rect.x + 4, rect.y + 2, rect.width - 8, rect.height - 4));
               }
            }
            return;
         }
         if (filterAcceptsComponent(component, filter)) {
            Rectangle rect = SwingUtilities.convertRectangle(component.getParent(), component.getBounds(), getParent());
            rectangles.add(new Rectangle(rect.x - 2, rect.y - 1, rect.width + 4, rect.height + 2));
         }
         if (component instanceof Container container) {
            for (int i = 0; i < container.getComponentCount(); i++) {
               addRectangles(container.getComponent(i), filter, rectangles);
            }
         }
      }
   }

   private static final class ConfigurationNode extends DefaultMutableTreeNode {
      private final ConfigurationUnit configurationUnit;
      private final List<ConfigurationNode> allChildren;

      private ConfigurationNode(ConfigurationUnit configurationUnit) {
         super(configurationUnit.getDisplayName());

         this.configurationUnit = configurationUnit;
         allChildren = configurationUnit.getSubUnits().stream()
               .filter(ConfigurationUnit::hasConfigurationComponent)
               .map(ConfigurationNode::new)
               .toList();
         allChildren.forEach(this::add);
      }

      private ConfigurationUnit getConfigurationUnit() {
         return configurationUnit;
      }

      private Stream<ConfigurationNode> getAttachedNodesRecursively() {
         return Stream.concat(
               Stream.of(this),
               IntStream.range(0, getChildCount())
                     .mapToObj(index -> (ConfigurationNode) getChildAt(index))
                     .flatMap(ConfigurationNode::getAttachedNodesRecursively));
      }

      private boolean filter(TextFilter filter, Map<ConfigurationNode, Component> filterComponentCache) {
         removeAllChildren();
         allChildren.forEach(child -> {
            if (child.filter(filter, filterComponentCache)) {
               add(child);
            }
         });
         return getChildCount() > 0 || filterAcceptsNode(filter, filterComponentCache);
      }

      private boolean filterAcceptsNode(TextFilter filter, Map<ConfigurationNode, Component> filterComponentCache) {
         return filter.test(configurationUnit.getDisplayName())
               || filterAcceptsComponentHierarchy(filterComponentCache.computeIfAbsent(this, _ -> configurationUnit.getComponent()), filter);
      }
   }
}
