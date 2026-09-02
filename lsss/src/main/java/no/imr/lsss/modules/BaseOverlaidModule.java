package no.imr.lsss.modules;

import com.google.common.collect.ImmutableList;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.modules.pojodata.PojoData;
import no.imr.lsss.modules.pojodata.PojoDataContainer;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.lsss.util.ApiMenuBuilder;
import no.imr.lsss.util.LsssToolTip;
import no.imr.tools.LateInit;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.ConcurrentObject;
import no.imr.tools.help.ContextSensitiveHelp;
import no.imr.tools.help.HelpID;
import no.imr.tools.listening.ChangeManager;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.logging.Log;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.swing.DeepInputListener;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MouseAndKeyAdapter;
import no.imr.tools.swing.MultiColumnLayout;
import no.imr.tools.swing.PopupMenuAdapter;
import no.imr.tools.swing.PopupMenuMouseListener;
import no.imr.tools.swing.SwingDelayer;
import no.imr.tools.swing.VerticalScrollablePanel;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.swing.WhenShowingListening;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.observing.ObservableValue;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for modules with overlays.
 *
 * @see BaseModuleOverlay
 */
public abstract class BaseOverlaidModule<O extends BaseModuleOverlay> extends BaseViewModule implements PojoDataContainer {
   private static final String XML_OVERLAYS = "overlays";
   private static final String XML_OVERLAY = "overlay";
   private static final String XML_NAME = "name";
   private static final String XML_ENABLED = "enabled";

   private static final int RECTANGLE_SIZE = 10;

   private final Set<Integer> mouseButtons = new HashSet<>();
   private final ListenableProperty<Optional<Point>> mousePosition = new ListenableProperty<>(Optional.empty());
   private int modifiersEx;
   private final LateInit<O> backgroundOverlay = new LateInit<>();
   private final List<O> foregroundOverlays = new ArrayList<>();
   private final List<O> backgroundOverlays = new ArrayList<>();
   private List<O> overlays = List.of();
   private final LateInit<O> activeOverlay = new LateInit<>();
   private boolean activeOverlayLocked;
   private int width = 1;
   private int height = 1;
   private final ChangeManager sizeChangeManager = new ChangeManager();
   private final Runnable updateActiveOverlayListener = this::updateActiveOverlay;
   private List<Element> configurationOfUnknownOverlays = List.of();

   protected BaseOverlaidModule(ModuleInfo<?> moduleInfo) {
      super(moduleInfo);
   }

   protected @Nullable String getDefaultToolTipText(Point point) {
      return null;
   }

   private @Nullable String getFirstPossibleToolTipAmongOverlays(Point point) {
      Rectangle2D rectangle = createRectangle(point);

      for (int i = overlays.size() - 1; i >= 0; i--) {
         O overlay = overlays.get(i);
         OverlayDisplayData displayData = overlay.getDisplayData();
         if (displayData != null && displayData.intersects(rectangle)) {
            String toolTipText = overlay.getToolTipText(point);
            if (toolTipText != null) {
               return toolTipText;
            }
         }
      }
      return null;
   }

   public static Rectangle2D createRectangle(Point point) {
      return new Rectangle2D.Double(point.x - RECTANGLE_SIZE, point.y - RECTANGLE_SIZE, 2 * RECTANGLE_SIZE, 2 * RECTANGLE_SIZE);
   }

   public ChangeManager getSizeChangeManager() {
      return sizeChangeManager;
   }

   protected void draw(Graphics2D g2d) {
   }

   protected boolean shouldUseOverlays() {
      return true;
   }

   public ObservableValue<Optional<Point>> mousePosition() {
      return mousePosition;
   }

   public @Nullable Point getMousePosition() {
      return mousePosition.getValue().orElse(null);
   }

   public boolean isAnyMouseButtonPressed() {
      return !mouseButtons.isEmpty();
   }

   public int getModifiersEx() {
      return modifiersEx;
   }

   public Stream<O> userVisibleForegroundOverlays() {
      return foregroundOverlays.stream()
            .filter(BaseModuleOverlay::isUserVisibleForegroundOverlay);
   }

   public List<O> getOverlays() {
      return overlays;
   }

   void setOverlays(List<O> overlays) {
      this.overlays = overlays;
      for (O overlay : overlays) {
         if (overlay.isBackgroundOverlay()) {
            overlay.setEnabledByUser(false);
            backgroundOverlays.add(overlay);
         } else {
            foregroundOverlays.add(overlay);
         }
         overlay.getEnabledChangeManager().addListener(this::repaint);
      }

      O firstBackgroundOverlay = backgroundOverlays.getFirst();
      backgroundOverlay.set(firstBackgroundOverlay);
      activeOverlay.set(firstBackgroundOverlay);
      setBackgroundOverlay(firstBackgroundOverlay);
      firstBackgroundOverlay.onActivate();
   }

   private void setBackgroundOverlay(O overlay) {
      backgroundOverlay.get().setEnabledByUser(false);
      overlay.setEnabledByUser(true);

      if (activeOverlay.get() == backgroundOverlay.get()) {
         setActiveOverlay(overlay);
      }

      backgroundOverlay.set(overlay);
   }

   protected void setBackgroundOverlay(Class<? extends O> overlayClass) {
      O overlay = getBackgroundOverlay(overlayClass);
      if (overlay != null) {
         setBackgroundOverlay(overlay);
      } else {
         Log.global.warning("No such overlay: " + overlayClass);
      }
   }

   public <T extends O> @Nullable T getBackgroundOverlay(Class<T> overlayClass) {
      return Utils.getFirstOrNull(backgroundOverlays, overlayClass);
   }

   O getActiveOverlay() {
      return activeOverlay.get();
   }

   private void setActiveOverlay(O overlay) {
      O previous = activeOverlay.get();
      if (previous != overlay) {
         // Update field activeOverlay before calling deactivate on previous overlay.
         activeOverlay.set(overlay);
         previous.onDeactivate();
         setCursor(Cursor.getDefaultCursor());
         overlay.onActivate();
         updateToolTipText();
      }
   }

   private @Nullable String getToolTipText(Point position) {
      String toolTipText = activeOverlay.get().getToolTipText(position);
      if (toolTipText != null) {
         return toolTipText;
      }
      toolTipText = getFirstPossibleToolTipAmongOverlays(position);
      if (toolTipText != null) {
         return toolTipText;
      }
      return getDefaultToolTipText(position);
   }

   private O findOverlappingOverlay(@Nullable Point point) {
      if (point == null || point.x < 0 || point.x >= width || point.y < 0 || point.y >= height) {
         return backgroundOverlay.get();
      }

      Rectangle2D rectangle = createRectangle(point);

      for (int i = foregroundOverlays.size() - 1; i >= 0; i--) {
         O overlay = foregroundOverlays.get(i);
         OverlayDisplayData displayData = overlay.getDisplayData();
         if (overlay.isEnabled() && displayData != null && displayData.intersects(rectangle) && overlay.readyToTakeFocus()) {
            return overlay;
         }
      }

      return backgroundOverlay.get();
   }

   public void updateActiveOverlayLater() {
      SwingDelayer.invokeLater(updateActiveOverlayListener, updateActiveOverlayListener);
   }

   public void updateActiveOverlay() {
      if (shouldUseOverlays() && !activeOverlayLocked && !isAnyMouseButtonPressed()) {
         setActiveOverlay(findOverlappingOverlay(getMousePosition()));
      }
   }

   public boolean isActiveOverlayLocked() {
      return activeOverlayLocked;
   }

   public void setActiveOverlayLocked(boolean activeOverlayLocked) {
      this.activeOverlayLocked = activeOverlayLocked;
   }

   public Rectangle getBounds() {
      return new Rectangle(width, height);
   }

   public GraphicsConfiguration getGraphicsConfiguration() {
      return GuiUtils.getNowOrWait(() -> {
         GraphicsConfiguration graphicsConfiguration = getComponent().getGraphicsConfiguration();
         if (graphicsConfiguration != null) {
            return graphicsConfiguration;
         }
         return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
      });
   }

   @Override
   public abstract ViewHolder<? extends BaseOverlaidView> getViewHolder();

   @Override
   public Element toXml() {
      Element moduleElement = DocumentHelper.createElement(XML_MODULE);

      Element parametersElement = new ParameterCollection(this).toXml();
      if (parametersElement.hasContent()) {
         moduleElement.add(parametersElement);
      }

      Element overlaysElement = DocumentHelper.createElement(XML_OVERLAYS);
      moduleElement.add(overlaysElement);

      List<Element> overlayElements = new ArrayList<>();
      for (O overlay : foregroundOverlays) {
         Element overlayElement = DocumentHelper.createElement(XML_OVERLAY)
               .addAttribute(XML_NAME, overlay.getPersistentName())
               .addAttribute(XML_ENABLED, Boolean.toString(overlay.isEnabledByUser()));
         overlayElements.add(overlayElement);
      }

      configurationOfUnknownOverlays.forEach(element -> {
         overlayElements.add(element.createCopy());
      });

      overlayElements.sort(Comparator.comparing(element -> element.attributeValue(XML_NAME)));

      overlaysElement.elements().addAll(overlayElements);

      return moduleElement;
   }

   @Override
   public void fromXml(Element element) {
      Element parametersElement = element.element(ParameterCollection.XML_PARAMETERS);
      if (parametersElement != null) {
         new ParameterCollection(this).fromXml(parametersElement);
      }

      Map<String, O> nameToOverlay = overlays.stream()
            .collect(Collectors.toMap(BaseLsssModule::getPersistentName, Function.identity()));

      ImmutableList.Builder<Element> unknowns = ImmutableList.builder();
      Element overlaysElement = element.element(XML_OVERLAYS);
      if (overlaysElement != null) {
         for (Element overlayElement : overlaysElement.elements(XML_OVERLAY)) {
            String name = overlayElement.attributeValue(XML_NAME);
            O overlay = nameToOverlay.get(name);
            if (overlay == null) {
               Element copy = overlayElement.createCopy();
               XmlUtils.removeBlankMixedContentText(copy);
               unknowns.add(copy);
               continue;
            }
            if (overlay.isBackgroundOverlay()) {
               continue;
            }
            overlay.setEnabledByUser(Boolean.parseBoolean(overlayElement.attributeValue(XML_ENABLED)));
         }
      }
      configurationOfUnknownOverlays = unknowns.build();
   }

   public int getWidth() {
      return width;
   }

   public int getHeight() {
      return height;
   }

   private void setSize(int aWidth, int aHeight) {
      if (aWidth >= 1 && aHeight >= 1 && (width != aWidth || height != aHeight)) {
         width = aWidth;
         height = aHeight;
         sizeChangeManager.notifyListeners();
      }
   }

   protected JComponent createOverlayEditor() {
      MultiColumnLayout multiColumnLayout = new MultiColumnLayout();
      multiColumnLayout.setHorizontalFill(true);
      JPanel scrollablePanel = new VerticalScrollablePanel(multiColumnLayout);

      List<JCheckBox> checkBoxes = new ArrayList<>();

      Map<String, List<O>> pluginToOverlays = userVisibleForegroundOverlays()
            .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getDisplayName))
            .collect(Collectors.groupingBy(overlay -> overlay.getPlugin().getMainPluginId()));

      for (FeaturePlugin plugin : getLSSS().getPluginManager().getFeaturePlugins()) {
         List<O> overlays = pluginToOverlays.get(plugin.getPersistentName());
         if (overlays == null) {
            continue;
         }
         if (!(plugin instanceof BaseSystemFeaturePlugin)) {
            scrollablePanel.add(Box.createVerticalStrut(5));
            scrollablePanel.add(new JSeparator());
            JLabel label = plugin.getIconOrEmpty().on(new JLabel(plugin.getName().displayName()));
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            label.setBorder(BorderFactory.createEmptyBorder(5, 3, 2, 0));
            scrollablePanel.add(label);
         }
         for (O overlay : overlays) {
            JCheckBox checkBox = new JCheckBox(overlay.getDisplayName(), overlay.isEnabledByUser());
            checkBox.setToolTipText(overlay.getDescription());
            checkBox.addItemListener(e -> {
               overlay.setEnabledByUser(e.getStateChange() == ItemEvent.SELECTED);
               getComponent().repaint();
            });
            checkBox.addMouseListener(new PopupMenuMouseListener(_ -> {
               JPopupMenu popupMenu = new JPopupMenu();
               new ApiMenuBuilder(getLSSS(), getComponent())
                     .overlayMenu(overlay)
                     .addTo(popupMenu);
               return popupMenu;
            }));
            WhenShowingListening.connect(checkBox, overlay.getEnabledByUserChangeManager(), GuiListeners.later(() -> checkBox.setSelected(overlay.isEnabledByUser())));
            JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrapper.add(checkBox);
            if (overlay.isConfigurable()) {
               AtomicBoolean visible = new AtomicBoolean(); // button.isVisible() is true to include it in layout
               JButton configureButton = new JButton("...") {
                  @Override
                  public void paint(Graphics g) {
                     if (visible.get()) {
                        super.paint(g);
                     }
                  }
               };
               configureButton.setMargin(new Insets(0, 3, 0, 3));
               configureButton.setToolTipText(new HtmlStringBuilder()
                     .text("Configure ").html("<b>").text(overlay.getDisplayName()).html("</b>")
                     .build());
               configureButton.setFocusable(false);
               configureButton.addActionListener(_ -> getConfigurationManager().showDialog(overlay));
               new DeepInputListener(wrapper, new MouseAndKeyAdapter() {
                  @Override
                  public void mouseEntered(MouseEvent e) {
                     setInside(true);
                  }

                  @Override
                  public void mouseExited(MouseEvent e) {
                     setInside(false);
                  }

                  private void setInside(boolean inside) {
                     visible.set(inside);
                     wrapper.repaint();
                  }
               });
               wrapper.add(configureButton);
            }
            scrollablePanel.add(wrapper);
            checkBoxes.add(checkBox);
         }
      }

      JScrollPane scrollPane = new JScrollPane(scrollablePanel);
      MultiColumnLayout.addRelayoutListener(scrollPane, scrollablePanel);

      JButton allOnButton = new JButton("All on");
      allOnButton.addActionListener(_ -> setForegroundOverlaysEnabled(checkBoxes, true));

      JButton allOffButton = new JButton("All off");
      allOffButton.addActionListener(_ -> setForegroundOverlaysEnabled(checkBoxes, false));

      JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
      buttonPanel.add(allOnButton);
      buttonPanel.add(allOffButton);

      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(scrollPane);
      mainPanel.add(buttonPanel, BorderLayout.SOUTH);
      return mainPanel;
   }

   private void setForegroundOverlaysEnabled(List<JCheckBox> checkBoxes, boolean enabled) {
      for (JCheckBox checkBox : checkBoxes) {
         checkBox.setSelected(enabled);
      }
      userVisibleForegroundOverlays().forEach(overlay -> {
         overlay.setEnabledByUser(enabled);
      });
   }

   public void repaint() {
      getViewHolder().ifView(BaseOverlaidView::repaint);
   }

   public void setCursor(Cursor cursor) {
      getViewHolder().ifView(view -> view.getComponent().setCursor(cursor));
   }

   public void updateToolTipText() {
      getViewHolder().ifView(BaseOverlaidView::updateToolTipText);
   }

   @Override
   public PojoData getPojoData() {
      List<PojoData> overlayData = overlays.stream()
            .filter(ConcurrentObject::isEnabled)
            .sorted(Comparator.comparing(BaseLsssModule::getPersistentName))
            .gather(Utils.allOfType(PojoDataContainer.class))
            .map(PojoDataContainer::getPojoData)
            .toList();
      return PojoData.newBuilder(getPersistentName())
            .with("overlays", overlayData)
            .build();
   }

   public abstract static class BaseOverlaidView extends BaseView {
      private final BaseOverlaidModule<?> module;
      private final OverlayComponent overlayComponent;
      private final LsssToolTip toolTip;
      private boolean isPopupShowing;
      private final SequencedSet<BaseModuleOverlay> recentlyHiddenOverlays = new LinkedHashSet<>();

      protected BaseOverlaidView(BaseOverlaidModule<?> module) {
         super(module);

         this.module = module;

         overlayComponent = new OverlayComponent();
         GuiUtils.disableFocusTraversalKeys(overlayComponent);
         ContextSensitiveHelp.setHelpIdProvider(overlayComponent, this::getHelpID);
         toolTip = new LsssToolTip(module.getLSSS(), overlayComponent, module::getToolTipText);

         for (BaseModuleOverlay overlay : module.getOverlays()) {
            overlay.getEnabledByUserChangeManager().addListener(GuiListeners.later(enabledByUser -> {
               recentlyHiddenOverlays.remove(overlay);
               if (!enabledByUser) {
                  recentlyHiddenOverlays.add(overlay);
               }
            }));
         }
      }

      @Override
      public JComponent getComponent() {
         return overlayComponent;
      }

      protected abstract KeyListener getKeyListener();

      private void updateToolTipText() {
         toolTip.update();
      }

      private void repaint() {
         overlayComponent.repaint();
      }

      private void showPopupMenu(Point point) {
         JPopupMenu popupMenu = null;
         if (module.shouldUseOverlays()) {
            popupMenu = module.activeOverlay.get().getPopupMenu(point);
         }
         if (popupMenu == null) {
            popupMenu = getFirstPossiblePopupMenuAmongOverlays(point);
         }
         if (popupMenu == null) {
            popupMenu = getDefaultPopupMenu(point);
         }

         popupMenu.addSeparator();

         new ApiMenuBuilder(module.getLSSS(), overlayComponent)
               .moduleMenu(module, findOverlays(point, popupMenu), point)
               .addTo(popupMenu);

         popupMenu.addPopupMenuListener(new PopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
               isPopupShowing = false;
            }
         });
         isPopupShowing = true;

         popupMenu.show(overlayComponent, point.x, point.y);
      }

      private @Nullable JPopupMenu getFirstPossiblePopupMenuAmongOverlays(Point point) {
         Rectangle2D rectangle = createRectangle(point);
         for (int i = module.overlays.size() - 1; i >= 0; i--) {
            BaseModuleOverlay overlay = module.overlays.get(i);
            OverlayDisplayData displayData = overlay.getDisplayData();
            if (displayData != null && displayData.intersects(rectangle)) {
               JPopupMenu menu = overlay.getPopupMenu(point);
               if (menu != null) {
                  return menu;
               }
            }
         }
         return null;
      }

      protected JPopupMenu getDefaultPopupMenu(Point point) {
         JPopupMenu popupMenu = new JPopupMenu();

         popupMenu.add(module.createConfigureMenuItem());
         addConfigureOverlaysItems(point, popupMenu);
         addDisableOverlaysItems(point, popupMenu);

         return popupMenu;
      }

      protected void addConfigureOverlaysItems(Point point, JPopupMenu popupMenu) {
         List<BaseModuleOverlay> configurableOverlays = new ArrayList<>();
         for (BaseModuleOverlay overlay : findSortedOverlays(point, popupMenu)) {
            if (overlay.isConfigurable()) {
               configurableOverlays.add(overlay);
            }
         }
         if (configurableOverlays.isEmpty()) {
            return;
         }

         boolean onlyOne = configurableOverlays.size() == 1;
         JComponent container;
         if (onlyOne) {
            container = popupMenu;
         } else {
            container = new JMenu("Configure overlay");
            popupMenu.add(container);
         }
         for (BaseModuleOverlay overlay : configurableOverlays) {
            String text = (onlyOne ? "Configure overlay " : "") + overlay.getDisplayName() + "...";
            JMenuItem item = new JMenuItem(text);
            item.setToolTipText(overlay.getDescription());
            item.addActionListener(_ -> module.getConfigurationManager().showDialog(overlay));
            container.add(item);
         }
      }

      public void addDisableOverlaysItems(Point point, JPopupMenu popupMenu) {
         List<BaseModuleOverlay> overlays = findSortedOverlays(point, popupMenu);
         if (!overlays.isEmpty()) {
            boolean onlyOne = overlays.size() == 1;
            JComponent container;
            if (onlyOne) {
               container = popupMenu;
            } else {
               container = new JMenu("Hide overlay");
               popupMenu.add(container);
            }
            for (BaseModuleOverlay overlay : overlays) {
               String text = (onlyOne ? "Hide overlay " : "") + overlay.getDisplayName();
               JMenuItem item = new JMenuItem(text);
               item.setToolTipText(overlay.getDescription());
               item.addActionListener(_ -> overlay.setEnabledByUser(false));
               container.add(item);
            }
         }
         if (!recentlyHiddenOverlays.isEmpty()) {
            JMenu unhideMenu = new JMenu("Unhide overlay");
            popupMenu.add(unhideMenu);
            for (BaseModuleOverlay overlay : recentlyHiddenOverlays.reversed()) {
               JMenuItem item = unhideMenu.add(overlay.getDisplayName());
               item.setToolTipText(overlay.getDescription());
               item.addActionListener(_ -> overlay.setEnabledByUser(true));
            }
            if (recentlyHiddenOverlays.size() > 1) {
               unhideMenu.addSeparator();
               JMenuItem item = unhideMenu.add("All");
               item.addActionListener(_ -> {
                  for (BaseModuleOverlay overlay : recentlyHiddenOverlays) {
                     overlay.setEnabledByUser(true);
                  }
               });
            }
         }
      }

      private boolean forwardEventsToOverlays() {
         return module.shouldUseOverlays() && !isPopupShowing;
      }

      private boolean forwardEventsToOverlays(MouseEvent mouseEvent) {
         return forwardEventsToOverlays() && !SwingUtilities.isRightMouseButton(mouseEvent);
      }

      private List<BaseModuleOverlay> findOverlays(Point point, @Nullable JPopupMenu popupMenu) {
         if (popupMenu != null && popupMenu.getClientProperty(OverlayFinder.CLIENT_PROPERTY_KEY) instanceof OverlayFinder overlayFinder) {
            return overlayFinder.getFoundOverlays();
         }
         OverlayFinder overlayFinder = new OverlayFinder(overlayComponent, point);
         if (popupMenu != null) {
            popupMenu.putClientProperty(OverlayFinder.CLIENT_PROPERTY_KEY, overlayFinder);
         }
         return overlayFinder.getFoundOverlays();
      }

      private List<BaseModuleOverlay> findSortedOverlays(Point point, JPopupMenu popupMenu) {
         return findOverlays(point, popupMenu).stream()
               .sorted(Utils.comparingIgnoringCase(BaseLsssModule::getDisplayName))
               .toList();
      }

      private HelpID getHelpID(MouseEvent mouseEvent) {
         Point p = SwingUtilities.convertPoint(mouseEvent.getComponent(), mouseEvent.getPoint(), overlayComponent);

         List<BaseModuleOverlay> overlays = findOverlays(p, null);
         for (int i = overlays.size() - 1; i >= 0; i--) {
            BaseModuleOverlay overlay = overlays.get(i);
            HelpID helpID = overlay.getHelpID();
            if (helpID.isValid()) {
               return helpID;
            }
         }
         return module.getHelpID();
      }

      private final class OverlayComponent extends JComponent {
         private boolean mouseEntered;

         private OverlayComponent() {
            addKeyListener(new KeyAdapter() {
               @Override
               public void keyTyped(KeyEvent e) {
                  boolean handled = false;
                  if (forwardEventsToOverlays()) {
                     handled = module.activeOverlay.get().keyTyped(e);
                  }
                  if (!handled) {
                     getKeyListener().keyTyped(e);
                  }
               }

               @Override
               public void keyPressed(KeyEvent e) {
                  module.modifiersEx = e.getModifiersEx();
                  boolean handled = false;
                  if (forwardEventsToOverlays()) {
                     handled = module.activeOverlay.get().keyPressed(e);
                  }
                  if (!handled) {
                     getKeyListener().keyPressed(e);
                  }
               }

               @Override
               public void keyReleased(KeyEvent e) {
                  module.modifiersEx = e.getModifiersEx();
                  boolean handled = false;
                  if (forwardEventsToOverlays()) {
                     handled = module.activeOverlay.get().keyReleased(e);
                  }
                  if (!handled) {
                     getKeyListener().keyReleased(e);
                  }
               }
            });

            addMouseListener(new MouseAdapter() {
               @Override
               public void mousePressed(MouseEvent e) {
                  module.mouseButtons.add(e.getButton());

                  if (e.isPopupTrigger()) {
                     showPopupMenu(e.getPoint());
                  } else {
                     if (forwardEventsToOverlays(e)) {
                        module.activeOverlay.get().mousePressed(e);
                     }
                  }
               }

               @Override
               public void mouseReleased(MouseEvent e) {
                  module.mouseButtons.remove(e.getButton());

                  if (e.isPopupTrigger()) {
                     showPopupMenu(e.getPoint());
                  } else {
                     if (forwardEventsToOverlays(e)) {
                        module.activeOverlay.get().mouseReleased(e);
                     }
                  }

                  if (!mouseEntered) {
                     onMouseExitedOrMouseReleased(e);
                  }
               }

               @Override
               public void mouseClicked(MouseEvent e) {
                  if (forwardEventsToOverlays(e)) {
                     module.activeOverlay.get().mouseClicked(e);
                  }
               }

               @Override
               public void mouseEntered(MouseEvent e) {
                  mouseEntered = true;
                  setFocusable(true);
                  requestFocusInWindow();
                  module.modifiersEx = e.getModifiersEx();
                  module.mousePosition.setValue(Optional.of(e.getPoint()));
                  if (module.shouldUseOverlays()) {
                     module.activeOverlay.get().mouseEntered(e);
                  }
                  updateToolTipText();
               }

               @Override
               public void mouseExited(MouseEvent e) {
                  mouseEntered = false;
                  if (!module.isAnyMouseButtonPressed()) {
                     onMouseExitedOrMouseReleased(e);
                  }
               }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
               @Override
               public void mouseDragged(MouseEvent e) {
                  module.mousePosition.setValue(Optional.of(e.getPoint()));
                  if (forwardEventsToOverlays(e)) {
                     module.activeOverlay.get().mouseDragged(e);
                  }
                  updateToolTipText();
               }

               @Override
               public void mouseMoved(MouseEvent e) {
                  module.mousePosition.setValue(Optional.of(e.getPoint()));
                  module.updateActiveOverlay();
                  if (forwardEventsToOverlays()) {
                     module.activeOverlay.get().mouseMoved(e);
                  }
                  updateToolTipText();
               }
            });

            addHierarchyListener(_ -> {
               // Needed since componentResized is not called initially for phantom echogram below echogram. WHY?
               module.setSize(getWidth(), getHeight());
            });

            addComponentListener(new ComponentAdapter() {
               @Override
               public void componentResized(ComponentEvent e) {
                  module.setSize(getWidth(), getHeight());
               }
            });
         }

         private void onMouseExitedOrMouseReleased(MouseEvent e) {
            setFocusable(false);
            module.mousePosition.setValue(Optional.empty());
            if (module.shouldUseOverlays()) {
               module.activeOverlay.get().mouseExited(e);
            }
            module.updateActiveOverlay();
         }

         @Override
         public Point getToolTipLocation(MouseEvent event) {
            return new Point(event.getX() + 20, event.getY() + 20);
         }

         @Override
         protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;

            module.draw(g2d);

            paintOverlays(g2d, null);
         }

         private void paintOverlays(Graphics2D g2d, @Nullable OverlayFinder overlayFinder) {
            if (module.shouldUseOverlays()) {
               drawOverlays(g2d, module.foregroundOverlays, overlayFinder);
               drawTextOverlays(g2d, module.foregroundOverlays, overlayFinder);
               drawOverlay(g2d, module.backgroundOverlay.get(), overlayFinder);
               drawTextOverlay(g2d, module.backgroundOverlay.get(), overlayFinder);
            }
         }

         private static void drawOverlays(Graphics2D g2d, List<? extends BaseModuleOverlay> overlays, @Nullable OverlayFinder overlayFinder) {
            for (BaseModuleOverlay overlay : overlays) {
               if (overlay.isEnabled()) {
                  drawOverlay(g2d, overlay, overlayFinder);
               }
            }
         }

         private static void drawOverlay(Graphics2D g2d, BaseModuleOverlay overlay, @Nullable OverlayFinder overlayFinder) {
            OverlayDisplayData displayData = overlay.getDisplayData();
            if (displayData == null) {
               return;
            }
            Graphics2D tmpG2d = (Graphics2D) g2d.create();
            displayData.draw(tmpG2d);
            tmpG2d.dispose();
            if (overlayFinder != null) {
               overlayFinder.didDraw(overlay);
            }
         }

         private static void drawTextOverlays(Graphics2D g2d, List<? extends BaseModuleOverlay> overlays, @Nullable OverlayFinder overlayFinder) {
            for (BaseModuleOverlay overlay : overlays) {
               if (overlay.isEnabled()) {
                  drawTextOverlay(g2d, overlay, overlayFinder);
               }
            }
         }

         private static void drawTextOverlay(Graphics2D g2d, BaseModuleOverlay overlay, @Nullable OverlayFinder overlayFinder) {
            OverlayDisplayData displayData = overlay.getDisplayData();
            if (displayData == null) {
               return;
            }
            displayData.drawText(g2d);
            if (overlayFinder != null) {
               overlayFinder.didDraw(overlay);
            }
         }
      }

      private static final class OverlayFinder {
         private static final Object CLIENT_PROPERTY_KEY = new Object();
         private final BufferedImage image = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
         private final List<BaseModuleOverlay> foundOverlays = new ArrayList<>();

         private OverlayFinder(OverlayComponent overlayComponent, Point point) {
            Graphics2D g = image.createGraphics();
            Rectangle2D rectangle = createRectangle(point);
            g.scale(image.getWidth() / rectangle.getWidth(), image.getHeight() / rectangle.getHeight());
            g.translate(-rectangle.getX(), -rectangle.getY());
            overlayComponent.paintOverlays(g, this);
            g.dispose();
         }

         private List<BaseModuleOverlay> getFoundOverlays() {
            return foundOverlays;
         }

         private void didDraw(BaseModuleOverlay overlay) {
            if (!overlay.isUserVisibleForegroundOverlay()) {
               clear();
               return;
            }

            for (int x = 0; x < image.getWidth(); x++) {
               for (int y = 0; y < image.getHeight(); y++) {
                  if (image.getRGB(x, y) != 0) {
                     /*
                     JDialog dialog = new JDialog(null, overlay.getPersistentName(), Dialog.ModalityType.APPLICATION_MODAL);
                     dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                     dialog.getContentPane().add(new JLabel(new ImageIcon(image)));
                     dialog.setSize(400, 100);
                     dialog.setVisible(true);
                     */
                     foundOverlays.remove(overlay);
                     foundOverlays.add(overlay);
                     clear();
                     return;
                  }
               }
            }
         }

         private void clear() {
            Graphics2D g = image.createGraphics();
            g.setBackground(new Color(0, 0, 0, 0));
            g.clearRect(0, 0, image.getWidth(), image.getHeight());
            g.dispose();
         }
      }
   }
}
