package no.imr.lsss.viewer;

import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.resources.LsssIcons;
import no.imr.lsss.util.ApiMenuBuilder;
import no.imr.tools.Utils;
import no.imr.tools.help.ContextSensitiveHelp;
import no.imr.tools.logging.Log;
import no.imr.tools.math.MathUtils;
import no.imr.tools.misc.HtmlStringBuilder;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.MultiSplitPane;
import no.imr.tools.swing.SplitPaneContainer;
import no.imr.tools.swing.TryCatchPanel;
import no.imr.tools.swing.UiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Manages floatable modules.
 */
final class FloatableModuleManager {
   static final String XML_MODULE_MANAGER = "moduleManager";
   static final String XML_NAME = "name";
   private static final String XML_DIVIDER = "divider";

   static final String XML_MODULE = "module";
   static final String XML_VISIBLE = "visible";
   private static final String XML_FLOATING = "floating";
   private static final String XML_RELATIVE_SIZE = "relativeSize";
   private static final String XML_WINDOW = "window";
   private static final String XML_WINDOW_X = "x";
   private static final String XML_WINDOW_Y = "y";
   private static final String XML_WINDOW_WIDTH = "width";
   private static final String XML_WINDOW_HEIGHT = "height";

   private final MainDisplay mainDisplay;
   private boolean modulesVisible = true;
   private final SplitPaneContainer splitPaneContainer;
   private final MultiSplitPane multiSplitPane;
   private final List<Wrapper> floatableModules = new ArrayList<>();
   private float dividerProportionFromXml;
   private Instant minTimeForUpdatingRelativeSizes = Instant.EPOCH;
   private boolean doingFromXml;
   private final Name name;

   FloatableModuleManager(MainDisplay mainDisplay, int splitPaneContainerOrientation, int multiSplitPaneOrientation, Name name, JComponent component) {
      this.mainDisplay = mainDisplay;
      multiSplitPane = new MultiSplitPane(multiSplitPaneOrientation);
      multiSplitPane.getChangeManager().addListener(this::updateRelativeSizes);
      this.name = name;
      splitPaneContainer = new SplitPaneContainer(splitPaneContainerOrientation, component, multiSplitPane.getPanel());
      splitPaneContainer.getSplitPane().setResizeWeight(1);
   }

   @Override
   public String toString() {
      return name.persistentName() + ", " + multiSplitPane.getComponents().size() + "/" + floatableModules.size();
   }

   JComponent getComponent() {
      return splitPaneContainer.getComponent();
   }

   SplitPaneContainer getSplitPaneContainer() {
      return splitPaneContainer;
   }

   void setModulesVisible(boolean visible) {
      modulesVisible = visible;
      updateModulesVisibility();
   }

   void addModules(List<BaseViewModule> modules) {
      modules.forEach(module -> {
         floatableModules.add(new Wrapper(module));
      });
   }

   void relayoutFloatableModules() {
      if (doingFromXml) {
         return;
      }

      updateMinTimeForUpdatingRelativeSizes();

      List<JPanel> components = floatableModules.stream()
            .filter(wrapper -> wrapper.dockedPanel != null)
            .map(wrapper -> wrapper.dockedPanel.mainPanel)
            .toList();

      multiSplitPane.clear();
      multiSplitPane.addAll(components);

      updateModulesVisibility();
   }

   private void updateModulesVisibility() {
      boolean visible = modulesVisible && !multiSplitPane.getComponents().isEmpty();
      splitPaneContainer.setVisible(multiSplitPane.getPanel(), visible);
      if (visible) {
         checkMinimumSize();
         updateSplitters();
      }
   }

   private void checkMinimumSize() {
      boolean outerHorizontal = splitPaneContainer.isHorizontal();
      boolean innerHorizontal = multiSplitPane.isHorizontal();
      IntStream minSizeStream = multiSplitPane.getComponents().stream()
            .map(JComponent::getMinimumSize)
            .mapToInt(splitPaneContainer::getSize);
      int minSize = innerHorizontal == outerHorizontal
            ? minSizeStream.sum() + multiSplitPane.getComponents().size() * UiUtils.splitPaneDividerSize()
            : minSizeStream.max().orElse(0);
      int actualSize = splitPaneContainer.getSize(multiSplitPane.getPanel());
      if (actualSize < minSize) {
         splitPaneContainer.setDividerLocation(splitPaneContainer.getSize(splitPaneContainer.getComponent()) - minSize);
         splitPaneContainer.getSplitPane().validate();
      }
   }

   private static final class Resizing {
      private final double relativeSize;
      private final int minSize;
      private final int maxSize;
      private double size;

      private Resizing(double relativeSize, Wrapper.DockedPanel dockedPanel) {
         this.relativeSize = relativeSize;
         minSize = dockedPanel.getMinimumSize();
         maxSize = dockedPanel.getMaximumSize();
      }
   }

   private void updateSplitters() {
      List<JSplitPane> splitPanes = multiSplitPane.getSplitPanes();
      if (splitPanes.isEmpty()) {
         return;
      }

      int availableSize = multiSplitPane.getAvailableSize();
      List<Resizing> resizings = floatableModules.stream()
            .filter(wrapper -> wrapper.dockedPanel != null)
            .map(wrapper -> new Resizing(wrapper.relativeSize, wrapper.dockedPanel))
            .toList();

      int minimumSize = resizings.stream()
            .mapToInt(resizing -> resizing.minSize)
            .sum();
      if (availableSize < minimumSize) {
         double f = (double) availableSize / minimumSize;
         resizings.forEach(resizing -> resizing.size = f * resizing.minSize);
         applyResizings(resizings);
         return;
      }

      int maximumSize = resizings.stream()
            .mapToInt(resizing -> resizing.maxSize)
            .sum();
      if (availableSize > maximumSize) {
         double f = (double) availableSize / maximumSize;
         resizings.forEach(resizing -> resizing.size = f * resizing.maxSize);
         applyResizings(resizings);
         return;
      }

      double remainingAvailableSize = availableSize;
      Set<Resizing> remainingResizings = new HashSet<>(resizings);
      while (true) {
         double relativeSizeSum = remainingResizings.stream()
               .mapToDouble(resizing -> resizing.relativeSize)
               .sum();
         double f = remainingAvailableSize / relativeSizeSum;
         remainingResizings.forEach(resizing -> resizing.size = f * resizing.relativeSize);

         boolean allSizesOk = true;
         for (Iterator<Resizing> it = remainingResizings.iterator(); it.hasNext(); ) {
            Resizing resizing = it.next();
            if (resizing.size < resizing.minSize) {
               allSizesOk = false;
               resizing.size = resizing.minSize;
               remainingAvailableSize -= resizing.size;
               it.remove();
            } else if (resizing.size > resizing.maxSize) {
               allSizesOk = false;
               resizing.size = resizing.maxSize;
               remainingAvailableSize -= resizing.size;
               it.remove();
            }
         }
         if (allSizesOk) {
            break;
         }
      }
      applyResizings(resizings);
   }

   private void applyResizings(List<Resizing> resizings) {
      List<JSplitPane> splitPanes = multiSplitPane.getSplitPanes();
      double remainingSize = multiSplitPane.getAvailableSize();
      for (int i = 0; i < splitPanes.size(); i++) {
         JSplitPane splitPane = splitPanes.get(i);
         double size = resizings.get(i).size;
         splitPane.setDividerLocation((int) Math.round(size));
         splitPane.setResizeWeight(size / remainingSize);
         splitPane.validate();
         remainingSize -= size;
      }
   }

   private void updateMinTimeForUpdatingRelativeSizes() {
      minTimeForUpdatingRelativeSizes = Instant.now().plusSeconds(1);
   }

   private void updateRelativeSizes() {
      if (Instant.now().isBefore(minTimeForUpdatingRelativeSizes)) {
         return;
      }

      double averageSize = (double) multiSplitPane.getAvailableSize() / (multiSplitPane.getSplitPanes().size() + 1);
      floatableModules.stream()
            .filter(wrapper -> wrapper.dockedPanel != null)
            .forEach(wrapper -> wrapper.relativeSize = MathUtils.round(wrapper.dockedPanel.getActualSize() / averageSize, 10_000));
   }

   private static final Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);
   private static final Dimension TITLE_LABEL_MINIMUM_SIZE = new Dimension(0, 0);
   private static final Border CONTROL_PANEL_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

   Element toXml() {
      Element element = DocumentHelper.createElement(XML_MODULE_MANAGER)
            .addAttribute(XML_NAME, name.persistentName())
            .addAttribute(XML_DIVIDER, getDividerText());
      floatableModules.stream()
            .map(Wrapper::toXml)
            .forEach(element::add);
      return element;
   }

   void fromXml(Element displayElement) {
      Element element = XmlUtils.getFirstWithAttribute(displayElement.elements(), XML_NAME, name.persistentName());
      if (element != null) {
         doingFromXml = true;
         updateMinTimeForUpdatingRelativeSizes();
         Map<String, Element> nameToModuleElement = element.elements().stream()
               .collect(Collectors.toMap(e -> e.attributeValue(XML_NAME), Function.identity()));
         floatableModules.forEach(floatableModule -> {
            try {
               floatableModule.fromXml(nameToModuleElement);
            } catch (Exception e) {
               Log.global.log(Level.WARNING, "Error parsing XML for " + floatableModule.module.getPersistentName(), e);
            }
         });
         doingFromXml = false;
         relayoutFloatableModules();
         parseDividerText(element.attributeValue(XML_DIVIDER));
      }
   }

   private String getDividerText() {
      JSplitPane splitPane = splitPaneContainer.getSplitPane();
      int availableSize = splitPaneContainer.getSize() - UiUtils.splitPaneDividerSize();
      int dividerLocation = splitPane.getDividerLocation();
      float dividerProportion = Math.abs(dividerLocation - availableSize * dividerProportionFromXml) < 2
            ? dividerProportionFromXml
            : dividerLocation / (float) availableSize;
      return Float.toString(MathUtils.round(dividerProportion, 10_000));
   }

   private void parseDividerText(@Nullable String dividerText) {
      if (dividerText == null) {
         dividerProportionFromXml = 0;
         return;
      }
      JSplitPane splitPane = splitPaneContainer.getSplitPane();
      int availableSize = splitPaneContainer.getSize() - UiUtils.splitPaneDividerSize();
      dividerProportionFromXml = Float.parseFloat(dividerText);
      int dividerLocation = Math.round(dividerProportionFromXml * availableSize);
      splitPane.setDividerLocation(dividerLocation);
      splitPane.validate();
   }

   /**
    * Wrapper class for a module. The visibility of a module is controlled
    * by the module enabled state.
    */
   private final class Wrapper implements BaseViewModule.FloatableInfo {
      private final BaseViewModule module;
      private @Nullable JFrame frame;
      private @Nullable Rectangle frameBounds;
      private @Nullable DockedPanel dockedPanel;
      private @Nullable JPanel modulePanel;
      private boolean visible;
      private boolean floating;
      private double relativeSize = 1;

      private Wrapper(BaseViewModule module) {
         this.module = module;
         module.getEnabledChangeManager().addListener(GuiListeners.later(this::setVisible));
         setVisible(module.isEnabled());
         module.setFloatableInfo(this);
      }

      private static JButton createControlButton(SvgIcon icon, String toolTipText) {
         JButton button = icon.on(new JButton());
         button.setToolTipText(toolTipText);
         button.setBackground(Color.WHITE);
         button.setFocusable(false);
         button.setMargin(EMPTY_INSETS);
         return button;
      }

      private JFrame createFrame() {
         JFrame frame = new JFrame(module.getDisplayName());
         frame.getRootPane().putClientProperty(MainDisplay.LSSS_KEY, mainDisplay.getLSSS());
         GuiUtils.setAccelerator(frame.getRootPane(), Shortcuts.HELP, module.getHelpID()::show);
         frame.setIconImage(mainDisplay.getFrame().getIconImage());
         frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
               setFloating(false);
            }
         });
         if (frameBounds == null && modulePanel != null && modulePanel.isShowing()) {
            Point position = modulePanel.getLocationOnScreen();
            Insets insets = mainDisplay.getFrame().getInsets();
            frameBounds = new Rectangle(position.x - insets.left, position.y - insets.top,
                  modulePanel.getWidth() + insets.left + insets.right,
                  modulePanel.getHeight() + insets.top + insets.bottom);
         }
         frame.getContentPane().add(modulePanel);
         if (frameBounds != null) {
            frame.setBounds(frameBounds);
         } else {
            frame.pack();
            frame.setLocationRelativeTo(mainDisplay.getFrame());
         }
         frame.setVisible(true);
         return frame;
      }

      private void disposeFrame() {
         if (frame != null) {
            frameBounds = frame.getBounds();
            frame.dispose();
            frame = null;
         }
      }

      @Override
      public boolean isFloating() {
         return floating;
      }

      @Override
      public void setFloating(boolean floating) {
         if (this.floating == floating) {
            return;
         }
         this.floating = floating;
         update();
      }

      private void setVisible(boolean visible) {
         if (this.visible == visible) {
            return;
         }
         this.visible = visible;
         module.setEnabled(visible);
         update();
      }

      private void update() {
         if (visible) {
            if (modulePanel == null) {
               modulePanel = new TryCatchPanel(new BorderLayout());
               modulePanel.add(module.getComponent());
               modulePanel.setBackground(Color.WHITE);
            }
            if (floating) {
               if (frame == null) {
                  frame = createFrame();
               }
               dockedPanel = null;
            } else {
               disposeFrame();
               if (dockedPanel == null) {
                  dockedPanel = new DockedPanel();
               }
            }
         } else {
            modulePanel = null;
            disposeFrame();
            dockedPanel = null;
         }
         relayoutFloatableModules();
      }

      private Element toXml() {
         Element element = DocumentHelper.createElement(XML_MODULE)
               .addAttribute(XML_NAME, module.getName().persistentName())
               .addAttribute(XML_VISIBLE, Boolean.toString(visible))
               .addAttribute(XML_FLOATING, Boolean.toString(floating))
               .addAttribute(XML_RELATIVE_SIZE, Utils.toString(relativeSize));

         if (floating) {
            if (frame != null) {
               frameBounds = frame.getBounds();
            }
            if (frameBounds != null) {
               element.addElement(XML_WINDOW)
                     .addAttribute(XML_WINDOW_X, Integer.toString(frameBounds.x))
                     .addAttribute(XML_WINDOW_Y, Integer.toString(frameBounds.y))
                     .addAttribute(XML_WINDOW_WIDTH, Integer.toString(frameBounds.width))
                     .addAttribute(XML_WINDOW_HEIGHT, Integer.toString(frameBounds.height));
            }
         }

         return element;
      }

      private void fromXml(Map<String, Element> nameToModuleElement) {
         boolean xmlVisible = false;
         boolean xmlFloating = false;
         relativeSize = 1;
         frameBounds = null;

         Element moduleElement = nameToModuleElement.get(module.getName().persistentName());
         if (moduleElement != null) {
            xmlVisible = Boolean.parseBoolean(moduleElement.attributeValue(XML_VISIBLE));
            xmlFloating = Boolean.parseBoolean(moduleElement.attributeValue(XML_FLOATING));
            relativeSize = Double.parseDouble(moduleElement.attributeValue(XML_RELATIVE_SIZE));

            Element windowElement = moduleElement.element(XML_WINDOW);
            if (windowElement != null) {
               int x = Integer.parseInt(windowElement.attributeValue(XML_WINDOW_X));
               int y = Integer.parseInt(windowElement.attributeValue(XML_WINDOW_Y));
               int width = Integer.parseInt(windowElement.attributeValue(XML_WINDOW_WIDTH));
               int height = Integer.parseInt(windowElement.attributeValue(XML_WINDOW_HEIGHT));
               frameBounds = GuiUtils.clampToScreen(new Rectangle(x, y, width, height));
            }
         }

         setVisible(xmlVisible);
         setFloating(xmlFloating);
         if (frame != null && frameBounds != null) {
            frame.setBounds(frameBounds);
         }
      }

      private final class DockedPanel {
         private final JPanel mainPanel = new JPanel(new BorderLayout());
         private final JPanel controlPanel = new JPanel(new BorderLayout());

         private DockedPanel() {
            mainPanel.add(modulePanel);

            Box controlBox = Box.createHorizontalBox();

            JLabel label = new JLabel(module.getDisplayName());
            label.setToolTipText(new HtmlStringBuilder()
                  .text(module.getDisplayName())
                  .html("<p>")
                  .text(module.getDescription())
                  .build());
            label.setMinimumSize(TITLE_LABEL_MINIMUM_SIZE);
            controlBox.add(label);
            controlBox.add(Box.createHorizontalGlue());

            if (module.isConfigurable()) {
               JButton configureButton = createControlButton(LsssIcons.SMALL_SETTINGS, "Configure");
               controlBox.add(configureButton);
               configureButton.addActionListener(_ -> mainDisplay.getLSSS().getConfigurationManager().showDialog(module));

               controlBox.add(Box.createHorizontalStrut(2));
            }

            JButton menuButton = createControlButton(LsssIcons.SMALL_MENU, "Menu");
            controlBox.add(menuButton);
            GuiUtils.addPopupMenuToButton(menuButton, popupMenu -> {
               JMenuItem helpItem = MiscIcons.HELP.on(popupMenu.add("Help"));
               module.getHelpID().enableHelpOnButton(helpItem);

               module.getViewHolder().getView().addToFloatableModuleMenu(popupMenu);

               popupMenu.addSeparator();

               new ApiMenuBuilder(mainDisplay.getLSSS(), mainPanel)
                     .moduleMenu(module)
                     .addTo(popupMenu);
            });

            controlBox.add(Box.createHorizontalStrut(2));

            JButton floatButton = createControlButton(LsssIcons.SMALL_FLOAT, "Float");
            controlBox.add(floatButton);
            floatButton.addActionListener(_ -> setFloating(!floating));

            controlBox.add(Box.createHorizontalStrut(2));

            JButton hideButton = createControlButton(LsssIcons.SMALL_HIDE, "Hide");
            controlBox.add(hideButton);
            hideButton.addActionListener(_ -> module.setEnabled(false));

            controlPanel.setBorder(CONTROL_PANEL_BORDER);
            controlPanel.setBackground(Color.WHITE);
            controlPanel.add(controlBox);

            mainPanel.add(controlPanel, BorderLayout.NORTH);

            ContextSensitiveHelp.setHelpID(controlPanel, LsssHelp.DISPLAY_FLOATABLE_VIEWS);
         }

         private int getActualSize() {
            return multiSplitPane.getSize(mainPanel);
         }

         private int getControlPanelSize() {
            return multiSplitPane.isHorizontal() ? 0 : controlPanel.getHeight();
         }

         private int getMinimumSize() {
            int minSize = multiSplitPane.getSize(module.getComponent().getMinimumSize());
            return minSize + getControlPanelSize();
         }

         private int getMaximumSize() {
            int controlPanelSize = getControlPanelSize();
            int maxSize = multiSplitPane.getSize(module.getComponent().getMaximumSize());
            if (maxSize > Short.MAX_VALUE - controlPanelSize) {
               return Short.MAX_VALUE;
            }
            int minSize = multiSplitPane.getSize(module.getComponent().getMinimumSize());
            return Math.max(minSize, maxSize) + controlPanelSize;
         }
      }
   }
}
