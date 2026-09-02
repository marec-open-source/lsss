package no.imr.tools.visualizer;

import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.swing.GroupingToolBar;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MultiColumnLayout;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public final class ItemVisualizer<T> {
   private final Preferences preferences;
   private final JPanel mainPanel = new JPanel(new BorderLayout());
   private final JPanel contentPanel = new JPanel(new BorderLayout());
   private final JButton hideSelectedButton = new JButton("Hide selected");
   private final JButton keepSelectedButton = new JButton("Keep selected");
   private final JButton showHiddenButton = new JButton("Show hidden");
   private final HidingItemContainer<T> hidingItemContainer;
   private final List<ItemView<T>> itemViews;
   private final List<JToggleButton> viewButtons;
   private @Nullable ItemView<T> selectedItemView;
   private final boolean[] needUpdate;

   public ItemVisualizer(List<ItemFeature<T>> features, ItemContainer<T> itemContainer, Preferences preferences) {
      this.preferences = preferences;

      ItemVisualizerConfig config = new ItemVisualizerConfig(preferences);

      hidingItemContainer = new HidingItemContainer<>(itemContainer);
      ItemScatter<T> itemScatter = new ItemScatter<>(config, features, hidingItemContainer);
      ItemHistogram<T> itemHistogram = new ItemHistogram<>(config, features, hidingItemContainer);
      ItemTable<T> itemTable = new ItemTable<>(features, hidingItemContainer);

      itemViews = List.of(itemScatter, itemHistogram, itemTable);

      needUpdate = new boolean[itemViews.size()];
      Arrays.fill(needUpdate, true);

      GroupingToolBar toolBar = new GroupingToolBar();

      mainPanel.add(toolBar.getToolBar(), BorderLayout.NORTH);
      mainPanel.add(contentPanel);

      JToggleButton scatterButton = new JToggleButton("Scatter");
      scatterButton.setMnemonic(KeyEvent.VK_S);
      scatterButton.addActionListener(_ -> setSelectedItemView(itemScatter));

      JToggleButton histogramButton = new JToggleButton("Histogram");
      histogramButton.setMnemonic(KeyEvent.VK_H);
      histogramButton.addActionListener(_ -> setSelectedItemView(itemHistogram));

      JToggleButton tableButton = new JToggleButton("Table");
      tableButton.setMnemonic(KeyEvent.VK_T);
      tableButton.addActionListener(_ -> setSelectedItemView(itemTable));

      JToggleButton settingsButton = MiscIcons.SETTINGS.on(new JToggleButton());
      settingsButton.setToolTipText("Settings");
      settingsButton.addActionListener(_ -> {
         selectedItemView = null;
         setContent(config.getComponent());
      });

      viewButtons = List.of(scatterButton, histogramButton, tableButton, settingsButton);

      GuiUtils.createButtonGroup(scatterButton, histogramButton, tableButton, settingsButton);
      toolBar.add(viewButtons);

      toolBar.addSeparator();

      hideSelectedButton.setToolTipText("Hides the current selection");
      hideSelectedButton.addActionListener(_ -> {
         hidingItemContainer.hideItems(hidingItemContainer.getSelectedItems());
         update();
      });

      keepSelectedButton.setToolTipText("Keeps the current selection");
      keepSelectedButton.addActionListener(_ -> {
         hidingItemContainer.hideItems(hidingItemContainer.getUnselectedItems());
         update();
      });

      showHiddenButton.setToolTipText("Restores previously hidden items");
      showHiddenButton.addActionListener(_ -> {
         hidingItemContainer.includeHiddenItems();
         update();
      });

      toolBar.add(List.of(hideSelectedButton, keepSelectedButton, showHiddenButton));

      List<ItemFeature.Category<T>> categoryFeatures = features.stream()
            .<ItemFeature.Category<T>>mapMulti((f, consumer) -> {
               if (f instanceof ItemFeature.Category<T> category) {
                  consumer.accept(category);
               }
            })
            .toList();
      if (!categoryFeatures.isEmpty()) {
         JButton selectButton = new JButton("Select by...");
         GuiUtils.addPopupMenuToButton(selectButton, popupMenu -> {
            for (ItemFeature.Category<T> categoryFeature : categoryFeatures) {
               JMenu menu = new JMenu(categoryFeature.name);
               popupMenu.add(menu);
               MultiColumnLayout multiColumnLayout = new MultiColumnLayout();
               multiColumnLayout.setHorizontalFill(true);
               menu.getPopupMenu().setLayout(multiColumnLayout);

               Map<String, List<T>> categoryToItems = hidingItemContainer.getAllItems().stream()
                     .collect(Collectors.groupingBy(categoryFeature.itemToString));
               for (String category : categoryFeature.getCategories()) {
                  List<T> items = categoryToItems.get(category);
                  if (items == null) {
                     continue;
                  }
                  String text = new HtmlStringBuilder().text(category)
                        .html(" &nbsp; <span style='color:gray;'>").text("(" + items.size() + ")").html("</span>")
                        .build();
                  menu.add(text).addActionListener(_ -> {
                     hidingItemContainer.addSelectedItems(items);
                  });
               }
               SwingUtilities.invokeLater(() -> {
                  int menuY = SwingUtilities.convertPoint(menu, 0, 0, mainPanel).y;
                  multiColumnLayout.setPreferredMaxHeight(mainPanel.getHeight() - menuY);
                  menu.getPopupMenu().pack();
               });
            }
         });
         toolBar.addSeparator();
         toolBar.add(selectButton);
      }

      toolBar.update();

      int selectedIndex = Math.clamp(preferences.getInt("selectedView", itemViews.indexOf(itemScatter)), 0, itemViews.size() - 1);
      setSelectedItemView(itemViews.get(selectedIndex));

      update();
   }

   private void setContent(JComponent component) {
      GuiUtils.replaceContent(contentPanel, component);
   }

   private void setSelectedItemView(ItemView<T> selectedItemView) {
      int index = itemViews.indexOf(selectedItemView);
      preferences.putInt("selectedView", index);
      viewButtons.get(index).setSelected(true);
      this.selectedItemView = selectedItemView;
      setContent(selectedItemView.getComponent());
      updateSelectedIfNeeded();
   }

   public JComponent getComponent() {
      return mainPanel;
   }

   private void updateSelectedIfNeeded() {
      if (selectedItemView == null) {
         return;
      }
      int i = itemViews.indexOf(selectedItemView);
      if (needUpdate[i]) {
         needUpdate[i] = false;
         selectedItemView.update();
      }
   }

   public void update() {
      Arrays.fill(needUpdate, true);
      hidingItemContainer.update();
      hideSelectedButton.setEnabled(!hidingItemContainer.getSelectedItems().isEmpty());
      keepSelectedButton.setEnabled(hidingItemContainer.getSelectedItems().size() < hidingItemContainer.getAllItems().size());
      showHiddenButton.setEnabled(!hidingItemContainer.getHiddenItems().isEmpty());
      updateSelectedIfNeeded();
   }

   public void selectFeatures(ItemFeature<T> x, ItemFeature<T> y) {
      itemViews.forEach(itemView -> itemView.selectFeatures(x, y));
      update();
   }

   public void show(@Nullable Component referenceComponent, String title) {
      JDialog dialog = new JDialog(GuiUtils.windowForComponent(referenceComponent), title, Dialog.ModalityType.MODELESS);
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      dialog.add(mainPanel);
      dialog.setSize(800, 500);
      GuiUtils.syncSize(dialog, preferences, "windowSize");
      dialog.setLocationRelativeTo(referenceComponent);
      GuiUtils.clampToScreen(dialog);
      dialog.setVisible(true);
   }
}
