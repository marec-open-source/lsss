package no.imr.lsss.viewer;

import no.imr.lsss.LSSS;
import no.imr.lsss.modules.BaseViewModule;
import no.imr.lsss.modules.ModuleManager;
import no.imr.lsss.modules.Where;
import no.imr.lsss.modules.echogram.BottomEchogramModule;
import no.imr.lsss.modules.echogram.ColorBarModule;
import no.imr.lsss.modules.echogram.PelagicEchogramModule;
import no.imr.lsss.modules.echogram.ScrollBarModule;
import no.imr.lsss.resources.LsssHelp;
import no.imr.lsss.resources.LsssResource;
import no.imr.tools.adm.AdmService;
import no.imr.tools.help.ContextSensitiveHelp;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.GeometryListener;
import no.imr.tools.swing.GuiListeners;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.SplitPaneContainer;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The main frame of LSSS.
 */
public final class MainDisplay {
   static final Object LSSS_KEY = new Object();

   public static final String XML_DISPLAY = "display";

   private static final Dimension DEFAULT_SIZE = new Dimension(900, 600);

   private final LSSS lsss;
   private final JFrame frame = new JFrame();

   private final KeyStrokeDispatcher keystrokeDispatcher = new KeyStrokeDispatcher();

   private final JPanel echogramPanel = new JPanel(new BorderLayout());
   private final JPanel echogramAndBelowPanel = new JPanel(new BorderLayout());

   private final FloatableModuleManager belowEchogramFloatableModuleManager;
   private final FloatableModuleManager rightFloatableModuleManager;
   private final FloatableModuleManager bottomFloatableModuleManager;

   public MainDisplay(LSSS lsss) {
      this.lsss = lsss;

      belowEchogramFloatableModuleManager = new FloatableModuleManager(this, JSplitPane.VERTICAL_SPLIT, JSplitPane.VERTICAL_SPLIT,
            new Name("BelowEchogram"), echogramPanel);
      rightFloatableModuleManager = new FloatableModuleManager(this, JSplitPane.HORIZONTAL_SPLIT, JSplitPane.VERTICAL_SPLIT,
            new Name("EastModuleManager"), echogramAndBelowPanel);
      bottomFloatableModuleManager = new FloatableModuleManager(this, JSplitPane.VERTICAL_SPLIT, JSplitPane.HORIZONTAL_SPLIT,
            new Name("SouthModuleManager"), rightFloatableModuleManager.getComponent());

      GeometryListener.startPreferenceSyncing(frame, DEFAULT_SIZE, null, lsss.getPreferences("display"), "windowGeometry");
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.setIconImage(LsssResource.LSSS_32);
      frame.setVisible(true);
   }

   LSSS getLSSS() {
      return lsss;
   }

   public JFrame getFrame() {
      return frame;
   }

   private void updateFrameTitle() {
      String title = "LSSS " + LSSS.VERSION;
      Path surveyFile = lsss.getSurveyManager().getSurveyFile();
      if (surveyFile != null) {
         title = surveyFile.getFileName() + " - [" + surveyFile.getParent() + "] - " + title;
      }
      frame.setTitle(title);
   }

   private void updateShowOnlyEchogram() {
      boolean modulesVisible = !lsss.getActions().showOnlyEchogram.get();
      rightFloatableModuleManager.setModulesVisible(modulesVisible);
      bottomFloatableModuleManager.setModulesVisible(modulesVisible);
      belowEchogramFloatableModuleManager.setModulesVisible(modulesVisible);
   }

   public void setup() {
      MainMenuBar menuBar = new MainMenuBar(this);
      MainToolBar toolBar = new MainToolBar(lsss);
      MainStatusBar statusBar = new MainStatusBar(lsss);

      menuBar.setup();
      toolBar.setup();
      statusBar.setup();

      ContextSensitiveHelp.setHelpID(menuBar.getMenuBar(), LsssHelp.DISPLAY_MENU_BAR);
      ContextSensitiveHelp.setHelpID(toolBar.getToolBar(), LsssHelp.DISPLAY_TOOLBAR);
      ContextSensitiveHelp.setHelpID(statusBar.getComponent(), LsssHelp.DISPLAY_STATUS_BAR);

      JPanel topPanel = new JPanel(new BorderLayout());
      topPanel.add(toolBar.getToolBar());
      JPanel infoPanel = AdmService.INSTANCE.mainDisplayInfoPanel(LSSS.APPLICATION_INFO);
      if (infoPanel != null) {
         topPanel.add(infoPanel, BorderLayout.SOUTH);
      }
      JPanel mainPanel = new JPanel(new BorderLayout());
      mainPanel.add(topPanel, BorderLayout.NORTH);

      GuiUtils.disableFocusTraversalKeys(frame);
      frame.getRootPane().putClientProperty(LSSS_KEY, lsss);
      frame.setJMenuBar(menuBar.getMenuBar());
      frame.getContentPane().add(mainPanel);
      frame.addWindowListener(new WindowAdapter() {
         @Override
         public void windowClosing(WindowEvent e) {
            shutDownIfUnmodifiedOrUserApproved();
         }
      });

      updateFrameTitle();

      SplitPaneContainer echogramSplitPaneContainer = new SplitPaneContainer(JSplitPane.VERTICAL_SPLIT,
            lsss.getModuleManager().getModule(PelagicEchogramModule.class).getComponent(),
            lsss.getModuleManager().getModule(BottomEchogramModule.class).getComponent());
      echogramSplitPaneContainer.getSplitPane().setResizeWeight(1);

      echogramPanel.add(echogramSplitPaneContainer.getComponent());
      echogramPanel.add(lsss.getModuleManager().getModule(ScrollBarModule.class).getComponent(), BorderLayout.SOUTH);

      echogramAndBelowPanel.add(belowEchogramFloatableModuleManager.getComponent());
      echogramAndBelowPanel.add(lsss.getModuleManager().getModule(ColorBarModule.class).getComponent(), BorderLayout.EAST);

      Map<Where, List<BaseViewModule>> viewModules = lsss.getModuleManager().getViewModules();
      belowEchogramFloatableModuleManager.addModules(viewModules.get(Where.BELOW_ECHOGRAM));
      bottomFloatableModuleManager.addModules(viewModules.get(Where.BOTTOM));
      rightFloatableModuleManager.addModules(viewModules.get(Where.RIGHT));

      mainPanel.add(bottomFloatableModuleManager.getComponent());
      mainPanel.add(statusBar.getComponent(), BorderLayout.SOUTH);

      frame.validate();

      belowEchogramFloatableModuleManager.relayoutFloatableModules();
      rightFloatableModuleManager.relayoutFloatableModules();
      bottomFloatableModuleManager.relayoutFloatableModules();

      setDividerLocation(belowEchogramFloatableModuleManager.getSplitPaneContainer(), 0.8);
      setDividerLocation(bottomFloatableModuleManager.getSplitPaneContainer(), 0.7);
      setDividerLocation(rightFloatableModuleManager.getSplitPaneContainer(), 0.8);
      setDividerLocation(echogramSplitPaneContainer, 0.7);

      if (lsss.getLsssConfig().isPrimaryLSSS) {
         keystrokeDispatcher.start();
      }
      lsss.getActions().showOnlyEchogram.getChangeManager().addListener(GuiListeners.coalescingLater(this::updateShowOnlyEchogram));
      lsss.getConfigurationManager().getSurveyMiscConf().pelagicMode.subscribe(GuiListeners.coalescingLater(pelagicMode -> {
         echogramSplitPaneContainer.setRightVisible(!pelagicMode);
      }));
      lsss.getSurveyManager().getChangeManager().addListener(GuiListeners.coalescingLater(this::updateFrameTitle));
   }

   public void close() {
      keystrokeDispatcher.stop();
      frame.dispose();
   }

   private static void setDividerLocation(SplitPaneContainer splitPaneContainer, double proportionalDividerLocation) {
      int size = splitPaneContainer.getSize(splitPaneContainer.getComponent());
      splitPaneContainer.setDividerLocation((int) (size * proportionalDividerLocation));
      splitPaneContainer.getSplitPane().validate();
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(XML_DISPLAY);

      element.add(bottomFloatableModuleManager.toXml());
      element.add(rightFloatableModuleManager.toXml());
      element.add(belowEchogramFloatableModuleManager.toXml());

      return element;
   }

   public void fromXml(Element displayElement) {
      bottomFloatableModuleManager.fromXml(displayElement);
      rightFloatableModuleManager.fromXml(displayElement);
      belowEchogramFloatableModuleManager.fromXml(displayElement);
   }

   public static void noDisplayFromXml(Element displayElement, ModuleManager moduleManager) {
      Set<String> visibleModules = displayElement.elements(FloatableModuleManager.XML_MODULE_MANAGER).stream()
            .flatMap(element -> element.elements(FloatableModuleManager.XML_MODULE).stream())
            .filter(element -> Boolean.parseBoolean(element.attributeValue(FloatableModuleManager.XML_VISIBLE)))
            .map(element -> element.attributeValue(FloatableModuleManager.XML_NAME))
            .collect(Collectors.toSet());
      moduleManager.getViewModules().values().stream()
            .flatMap(List::stream)
            .forEach(module -> module.setEnabled(visibleModules.contains(module.getPersistentName())));
   }

   void shutDownIfUnmodifiedOrUserApproved() {
      if (lsss.getSurveyManager().isUnmodifiedOrUserApproved()) {
         lsss.getSurveyManager().close();
         if (lsss.getConfigurationManager().getApplicationConfiguration().isUnmodifiedOrUserApproved(getFrame())) {
            lsss.close();
         }
      }
   }
}
