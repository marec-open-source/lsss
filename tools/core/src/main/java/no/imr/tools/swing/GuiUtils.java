package no.imr.tools.swing;

import com.google.common.util.concurrent.UncheckedExecutionException;
import no.imr.tools.ResourceUtils;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.Utils;
import no.imr.tools.concurrent.AsyncHandle;
import no.imr.tools.io.FileUtils;
import no.imr.tools.listening.ListenableProperty;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.ValueParameter;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.marec.lsss.api.util.observing.ObservableProperty;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.MenuEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.text.html.parser.ParserDelegator;
import javax.swing.undo.UndoManager;
import java.awt.AWTException;
import java.awt.AWTKeyStroke;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EventObject;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Various graphical utility functions.
 */
public final class GuiUtils {
   public static final Color SEPARATOR_COLOR = new Color(0xa0a0a0);
   public static final Border DEFAULT_MARGIN = BorderFactory.createEmptyBorder(5, 5, 5, 5);

   public static final BasicStroke STROKE_1 = new BasicStroke();
   public static final BasicStroke STROKE_2 = new BasicStroke(2);
   public static final BasicStroke STROKE_3 = new BasicStroke(3);

   private static final Object INFO_KEY = new Object();

   private GuiUtils() {
   }

   public static void putInfoProperty(JComponent component, String info) {
      component.putClientProperty(INFO_KEY, info);
   }

   public static @Nullable String getInfoProperty(Component component) {
      return (String) getAncestorProperty(component, INFO_KEY);
   }

   public static @Nullable Object getAncestorProperty(Component component, Object key) {
      while (component != null) {
         if (component instanceof JComponent jComponent) {
            Object property = jComponent.getClientProperty(key);
            if (property != null) {
               return property;
            }
         }
         component = component.getParent();
      }
      return null;
   }

   public static void invokeLaterLater(Runnable runnable) {
      SwingUtilities.invokeLater(() -> SwingUtilities.invokeLater(runnable));
   }

   /**
    * Executes a runnable in the event dispatching thread.
    * If called from the event dispatching thread, the runnable is executed immediately.
    * This function can be called from any thread.
    *
    * @param runnable the runnable to execute
    */
   public static void invokeNowOrLater(Runnable runnable) {
      if (SwingUtilities.isEventDispatchThread()) {
         runnable.run();
      } else {
         SwingUtilities.invokeLater(runnable);
      }
   }

   /**
    * Executes a runnable in the event dispatching thread.
    * If called from the event dispatching thread, the runnable is executed immediately.
    * This function can be called from any thread.
    *
    * @param runnable the runnable to execute
    * @throws UncheckedExecutionException as wrapper for InvocationTargetException, InterruptedException
    */
   public static void invokeNowOrWait(Runnable runnable) {
      if (SwingUtilities.isEventDispatchThread()) {
         runnable.run();
      } else {
         try {
            SwingUtilities.invokeAndWait(runnable);
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         } catch (InvocationTargetException e) {
            throw new UncheckedExecutionException(e);
         }
      }
   }

   public static void invokeNowOrWait(AsyncHandle asyncHandle, Runnable runnable) {
      if (SwingUtilities.isEventDispatchThread()) {
         runnable.run();
      } else {
         asyncHandle.invokeAndWait(GuiUtils::invokeNowOrLater, runnable);
      }
   }

   public static <T> T getNowOrWait(Supplier<T> supplier) {
      AtomicReference<T> value = new AtomicReference<>();
      invokeNowOrWait(() -> value.set(supplier.get()));
      return value.get();
   }

   /**
    * Returns the window for a component.
    * This function allows the specified component to be a window,
    * in contrast to {@link SwingUtilities#windowForComponent(Component)}.
    *
    * @param component a component
    * @return the window for the component
    */
   public static @Nullable Window windowForComponent(@Nullable Component component) {
      while (true) {
         switch (component) {
            case null -> {
               return null;
            }
            case Window window -> {
               return window;
            }
            case JPopupMenu popupMenu -> {
               component = popupMenu.getInvoker();
            }
            default -> {
               component = component.getParent();
            }
         }
      }
   }

   public static @Nullable Window windowForEvent(EventObject e) {
      if (e.getSource() instanceof Component component) {
         return windowForComponent(component);
      }
      return null;
   }

   public static void validateAndRepaintTopmostParent(Component component) {
      Component topmostParent = getTopmostParent(component);
      topmostParent.validate();
      topmostParent.repaint();
   }

   public static Component getTopmostParent(Component component) {
      while (component.getParent() != null && !(component instanceof Window)) {
         component = component.getParent();
      }
      return component;
   }

   public static void autoCreateContentMenu(JMenu menu, Runnable contentCreator) {
      menu.addMenuListener(new MenuAdapter() {
         @Override
         public void menuSelected(MenuEvent e) {
            contentCreator.run();
         }

         @Override
         public void menuDeselected(MenuEvent e) {
            // Clear menu later so that a menu item processing an event has a parent.
            SwingUtilities.invokeLater(menu::removeAll);
         }
      });
   }

   public static void addPopupMenuToButton(JButton button, Consumer<JPopupMenu> contentCreator) {
      AtomicLong hideTime = new AtomicLong();
      button.addActionListener(e -> {
         if (hideTime.get() > System.currentTimeMillis() - 250) {
            return;
         }
         JPopupMenu popupMenu = new JPopupMenu();
         contentCreator.accept(popupMenu);
         popupMenu.addPopupMenuListener(new PopupMenuAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
               hideTime.set(System.currentTimeMillis());
            }
         });
         popupMenu.show(button, 0, button.getHeight());
      });
   }

   public static JPanel createPanel(LayoutManager layout, Component... components) {
      JPanel panel = new JPanel(layout);
      for (Component component : components) {
         panel.add(component);
      }
      return panel;
   }

   public static JPanel createPanel(LayoutManager layout, Color background, Component... components) {
      JPanel panel = createPanel(layout, components);
      panel.setBackground(background);
      return panel;
   }

   public static JScrollPane createScrollPane(ParameterEditor parameterEditor) {
      JScrollPane scrollPane = createScrollPane(parameterEditor.getEditorComponent());
      parameterEditor.addPopupMenuMouseListener(scrollPane);
      return scrollPane;
   }

   public static JScrollPane createScrollPane(Component component) {
      JPanel panel = VerticalScrollablePanel.wrap(component);
      panel.setBorder(DEFAULT_MARGIN);
      return new JScrollPane(panel);
   }

   public static JScrollPane createScrollPane(Component... components) {
      return createScrollPane(List.of(components));
   }

   public static JScrollPane createScrollPane(Collection<? extends Component> components) {
      JPanel panel = new VerticalScrollablePanel(new GridBagLayout());
      panel.setBorder(DEFAULT_MARGIN);
      GridBag gridBag = new GridBag(panel)
            .configureVerticalBox();
      for (Component component : components) {
         gridBag.add(component);
      }
      return new JScrollPane(panel);
   }

   public static void createButtonGroup(AbstractButton... buttons) {
      ButtonGroup buttonGroup = new ButtonGroup();
      for (AbstractButton button : buttons) {
         buttonGroup.add(button);
      }
   }

   public static int showOptionDialog(@Nullable Component referenceComponent, String title, String message, String[] options) {
      return JOptionPane.showOptionDialog(referenceComponent, message, title,
            JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, null);
   }

   public static void showErrorDialog(@Nullable Component referenceComponent, String message) {
      Log.global.log(Level.INFO, "Error: " + message);
      doShowErrorDialog(referenceComponent, message, null);
   }

   public static void showErrorDialog(@Nullable Component referenceComponent, String message, @Nullable Throwable throwable) {
      Log.global.log(Level.INFO, "Error: " + message, throwable);
      doShowErrorDialog(referenceComponent, message, throwable != null ? Utils.stackTraceToString(throwable) : null);
   }

   public static void showErrorDialog(@Nullable Component referenceComponent, String message, @Nullable String details) {
      Log.global.log(Level.INFO, "Error: " + message + (details != null ? ":\n" + details : ""));
      doShowErrorDialog(referenceComponent, message, details);
   }

   private static void doShowErrorDialog(@Nullable Component referenceComponent, String message, @Nullable String details) {
      JPanel panel = new JPanel(new BorderLayout());
      panel.add(makeMultiLineLabel(message), BorderLayout.NORTH);

      JOptionPane optionPane = new JOptionPane(panel, JOptionPane.ERROR_MESSAGE);
      JDialog dialog = optionPane.createDialog(referenceComponent, "Error");
      dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
      dialog.setResizable(true);
      dialog.pack();

      if (details != null) {
         JTextArea detailText = new JTextArea();
         detailText.setEditable(false);
         neverUpdateCaret(detailText);
         detailText.setText(details);

         JLabel detailLabel = new JLabel("Details:");
         detailLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));

         JPanel detailPanel = new JPanel(new BorderLayout());
         detailPanel.add(detailLabel, BorderLayout.NORTH);
         detailPanel.add(new JScrollPane(detailText));
         panel.add(detailPanel);

         // Expand size somewhat, but not too much
         int maxWidth = dialog.getWidth() + 500;
         int maxHeight = dialog.getHeight() + 300;
         dialog.pack();
         int horizontalScrollbarHeight = dialog.getWidth() > maxWidth ? UiUtils.scrollBarWidth() : 0;
         int verticalScrollbarWidth = dialog.getHeight() > maxHeight ? UiUtils.scrollBarWidth() : 0;
         dialog.setSize(
               Math.min(maxWidth, dialog.getWidth() + verticalScrollbarWidth),
               Math.min(maxHeight, dialog.getHeight() + horizontalScrollbarHeight));
      }

      dialog.setLocationRelativeTo(referenceComponent);
      dialog.setVisible(true);
      dialog.dispose();
   }

   public static JComponent makeMultiLineLabel(String text) {
      if (text.startsWith("<html>") || text.indexOf('\n') < 0) {
         return new JLabel(text);
      }
      Box box = Box.createVerticalBox();
      text.lines().forEach(line -> {
         box.add(new JLabel(line.isEmpty() ? " " : line));
      });
      return box;
   }

   /**
    * Moves the {@link Window} of a component so that the mouse cursor is centered over the component.
    *
    * @param component the component that should be centered under the mouse cursor
    */
   public static void moveToMouse(Component component) {
      Window window = windowForComponent(component);
      if (window == null) {
         return;
      }
      Point p = SwingUtilities.convertPoint(component, component.getWidth() / 2, component.getHeight() / 2, window);
      Point m = MouseInfo.getPointerInfo().getLocation();
      window.setLocation(m.x - p.x, m.y - p.y);
   }

   /**
    * Creates a new cursor.
    *
    * @param dir     the location of the resource
    * @param name    the name of the resource
    * @param hotSpot the hot spot location
    * @return the cursor
    * @see Toolkit#createCustomCursor(Image, Point, String)
    */
   public static Cursor createCursor(String dir, String name, Point hotSpot) {
      Dimension bestSize = Toolkit.getDefaultToolkit().getBestCursorSize(32, 32);
      int imageSize;
      if (bestSize.width < 40) {
         imageSize = 32;
      } else if (bestSize.width < 56) {
         imageSize = 48;
         hotSpot = new Point(3 * hotSpot.x / 2, 3 * hotSpot.y / 2);
      } else {
         imageSize = 64;
         hotSpot = new Point(2 * hotSpot.x, 2 * hotSpot.y);
      }
      Image image = Toolkit.getDefaultToolkit().getImage(ResourceUtils.getUrl(dir + '/' + name + '-' + imageSize + ".png"));
      if (imageSize != bestSize.width || imageSize != bestSize.height) {
         image = image.getScaledInstance(bestSize.width, bestSize.height, Image.SCALE_DEFAULT);
         hotSpot = new Point(hotSpot.x * bestSize.width / imageSize, hotSpot.y * bestSize.height / imageSize);
      }
      return Toolkit.getDefaultToolkit().createCustomCursor(image, hotSpot, name);
   }

   public static Action toAction(Runnable runnable) {
      return new AbstractAction() {
         @Override
         public void actionPerformed(ActionEvent e) {
            runnable.run();
         }
      };
   }

   /**
    * Registers a keystroke as a shortcut for pressing a button.
    *
    * @param button    a push button or a toggle button
    * @param keyStroke a keystroke
    */
   public static void setAccelerator(AbstractButton button, KeyStroke keyStroke) {
      Action action = button.getAction();
      if (action == null || button instanceof JToggleButton) {
         action = toAction(button::doClick);
      }
      setAccelerator(button, keyStroke, action);
   }

   public static void setAccelerator(JComponent component, KeyStroke keyStroke, Action action) {
      component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, action);
      component.getActionMap().put(action, action);
   }

   public static void setAccelerator(JComponent component, KeyStroke keyStroke, Runnable action) {
      setAccelerator(component, keyStroke, toAction(action));
   }

   public static void connectMinimumSizeToPreferredLayoutSize(JComponent component) {
      component.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            Dimension dimension = component.getLayout().preferredLayoutSize(component);
            component.setMinimumSize(dimension);
            Container parent = component.getParent();
            parent.revalidate();
         }
      });
   }

   public static void connectToMousePosition(ListenableProperty<Optional<Point>> property, JComponent component) {
      MouseAdapter listener = new MouseAdapter() {
         @Override
         public void mouseEntered(MouseEvent e) {
            property.setValue(Optional.of(e.getPoint()));
         }

         @Override
         public void mouseExited(MouseEvent e) {
            property.setValue(Optional.empty());
         }

         @Override
         public void mouseDragged(MouseEvent e) {
            property.setValue(Optional.of(e.getPoint()));
         }

         @Override
         public void mouseMoved(MouseEvent e) {
            property.setValue(Optional.of(e.getPoint()));
         }
      };
      component.addMouseListener(listener);
      component.addMouseMotionListener(listener);
   }

   public static void connect(AbstractButton button, ObservableProperty<Boolean> property) {
      button.setSelected(property.getValue());
      button.addItemListener(e -> property.setValue(button.isSelected()));
      WhenShowingListening.connect(button, property, () -> button.setSelected(property.getValue()));
   }

   public static void connect(JSpinner spinner, ObservableProperty<Float> parameter) {
      spinner.setValue((double) parameter.getValue());
      spinner.addChangeListener(e -> parameter.setValue(((Number) spinner.getValue()).floatValue()));
      WhenShowingListening.connect(spinner, parameter, () -> spinner.setValue((double) parameter.getValue()));
   }

   public static <T> void connect(ListenableProperty<T> property, ValueParameter<T> parameter) {
      parameter.setValue(property.getValue());
      parameter.subscribe(property);
      property.subscribe(parameter);
   }

   public static <T> void connect(ValueParameter<T> parameterA, ValueParameter<T> parameterB) {
      parameterA.subscribe(parameterB);
      parameterB.subscribe(parameterA);
   }

   /**
    * Disables the use of focus traversal keys for a component.
    *
    * @param component a component
    */
   public static void disableFocusTraversalKeys(Component component) {
      Set<AWTKeyStroke> emptyKeystrokeSet = Set.of();
      component.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptyKeystrokeSet);
      component.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptyKeystrokeSet);
      component.setFocusTraversalKeys(KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, emptyKeystrokeSet);
      component.setFocusTraversalKeys(KeyboardFocusManager.DOWN_CYCLE_TRAVERSAL_KEYS, emptyKeystrokeSet);
   }

   /**
    * Creates a component taking up all available horizontal space.
    *
    * @return the filler component
    */
   public static JComponent createHorizontalFiller() {
      return new Box.Filler(new Dimension(0, 0), new Dimension(Short.MAX_VALUE, 0), new Dimension(Short.MAX_VALUE, 0));
   }

   /**
    * Creates a component taking up all available vertical space.
    *
    * @return the filler component
    */
   public static JComponent createVerticalFiller() {
      return new Box.Filler(new Dimension(0, 0), new Dimension(0, Short.MAX_VALUE), new Dimension(0, Short.MAX_VALUE));
   }

   public static void draw(Graphics2D g, Collection<? extends Drawable> drawables) {
      drawables.forEach(drawable -> drawable.draw(g));
   }

   /**
    * Test for intersection between line strip and rectangle.
    *
    * @param lineStripPath a path containing only {@link PathIterator#SEG_MOVETO}, {@link PathIterator#SEG_LINETO} and {@link PathIterator#SEG_CLOSE}
    * @param rectangle     a rectangle
    * @return {@code true} if intersection
    */
   public static boolean intersects(Path2D lineStripPath, Rectangle2D rectangle) {
      double[] a = new double[6];
      double x = Double.NaN;
      double y = Double.NaN;
      double moveToX = Double.NaN;
      double moveToY = Double.NaN;

      for (PathIterator pathIterator = lineStripPath.getPathIterator(null); !pathIterator.isDone(); pathIterator.next()) {
         int type = pathIterator.currentSegment(a);

         switch (type) {
            case PathIterator.SEG_MOVETO -> {
               x = a[0];
               y = a[1];
               moveToX = x;
               moveToY = y;
            }
            case PathIterator.SEG_LINETO -> {
               double x0 = x;
               double y0 = y;
               x = a[0];
               y = a[1];
               if (rectangle.intersectsLine(x0, y0, x, y)) {
                  return true;
               }
            }
            case PathIterator.SEG_CLOSE -> {
               double x0 = x;
               double y0 = y;
               x = moveToX;
               y = moveToY;
               if (rectangle.intersectsLine(x0, y0, x, y)) {
                  return true;
               }
            }
            default -> {
               throw new IllegalArgumentException("Illegal segment type: " + type);
            }
         }
      }

      return false;
   }

   /**
    * Appends a rectangle to a path.
    *
    * @param path a path
    * @param x0   x0
    * @param y0   y0
    * @param x1   x1
    * @param y1   y1
    */
   public static void appendRectangle(Path2D.Float path, float x0, float y0, float x1, float y1) {
      path.moveTo(x0, y0);
      path.lineTo(x1, y0);
      path.lineTo(x1, y1);
      path.lineTo(x0, y1);
      path.closePath();
   }

   public static void appendRectangleFromCenterAndRadius(Path2D.Float path, Point2D center, float radius) {
      appendRectangleFromCenterAndRadius(path, (float) center.getX(), (float) center.getY(), radius);
   }

   public static void appendRectangleFromCenterAndRadius(Path2D.Float path, float x, float y, float radius) {
      appendRectangle(path, x - radius, y - radius, x + radius, y + radius);
   }

   public static void appendPolygon(Path2D path, List<? extends Point2D> points) {
      moveTo(path, points.getFirst());
      for (int i = 1; i < points.size(); i++) {
         lineTo(path, points.get(i));
      }
      path.closePath();
   }

   public static void moveTo(Path2D path, Point2D p) {
      path.moveTo(p.getX(), p.getY());
   }

   public static void lineTo(Path2D path, Point2D p) {
      path.lineTo(p.getX(), p.getY());
   }

   public static void setCircle(Ellipse2D ellipse, Point2D point, double radius) {
      double d = 2 * radius;
      ellipse.setFrame(point.getX() - radius, point.getY() - radius, d, d);
   }

   public static void clampToScreen(Window window) {
      clampToScreen(window, window.getBounds());
   }

   public static void clampToScreen(Window window, int x, int y, int width, int height) {
      clampToScreen(window, new Rectangle(x, y, width, height));
   }

   public static void clampToScreen(Window window, Rectangle bounds) {
      window.setBounds(clampToScreen(bounds));
   }

   public static Rectangle clampToScreen(Rectangle bounds) {
      Rectangle screenBounds = getTotalScreenBounds(bounds);
      int width = Math.min(screenBounds.width, bounds.width);
      int height = Math.min(screenBounds.height, bounds.height);
      int x = (int) Math.clamp(bounds.x, screenBounds.getMinX(), screenBounds.getMaxX() - width);
      int y = (int) Math.clamp(bounds.y, screenBounds.getMinY(), screenBounds.getMaxY() - height);
      return new Rectangle(x, y, width, height);
   }

   private static Rectangle getTotalScreenBounds(Rectangle testBounds) {
      Rectangle totalBounds = new Rectangle(0, 0, -1, -1);
      int minX = Integer.MIN_VALUE;
      int maxX = Integer.MAX_VALUE;
      int minY = Integer.MIN_VALUE;
      int maxY = Integer.MAX_VALUE;
      for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
         Rectangle deviceBounds = graphicsDevice.getDefaultConfiguration().getBounds();
         totalBounds.add(deviceBounds);
         if (testBounds.intersects(deviceBounds)) {
            Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(graphicsDevice.getDefaultConfiguration());
            if (insets.left > 0) {
               minX = Math.max(minX, deviceBounds.x + insets.left);
            }
            if (insets.right > 0) {
               maxX = Math.min(maxX, deviceBounds.x + deviceBounds.width - insets.right);
            }
            if (insets.top > 0) {
               minY = Math.max(minY, deviceBounds.y + insets.top);
            }
            if (insets.bottom > 0) {
               maxY = Math.min(maxY, deviceBounds.y + deviceBounds.height - insets.bottom);
            }
         }
      }
      int x1 = Math.max(minX, totalBounds.x);
      int y1 = Math.max(minY, totalBounds.y);
      int x2 = Math.min(maxX, totalBounds.x + totalBounds.width);
      int y2 = Math.min(maxY, totalBounds.y + totalBounds.height);
      totalBounds.setBounds(x1, y1, x2 - x1, y2 - y1);
      return totalBounds;
   }

   public static void clearImage(Image image) {
      clearImage(image, 0, 0, image.getWidth(null), image.getHeight(null));
   }

   public static void clearImage(Image image, int x, int y, int width, int height) {
      Graphics2D g = (Graphics2D) image.getGraphics();
      g.setBackground(Color.BLACK);
      g.clearRect(x, y, width, height);
      g.dispose();
   }

   public static BufferedImage toImage(Component component) {
      BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D g = image.createGraphics();
      component.paint(g);
      g.dispose();
      return image;
   }

   public static BufferedImage screenshot(Component component) throws AWTException {
      Point locationOnScreen = component.getLocationOnScreen();
      Robot robot = new Robot();
      return robot.createScreenCapture(new Rectangle(locationOnScreen.x, locationOnScreen.y, component.getWidth(), component.getHeight()));
   }

   public static void setFullScreen(Window window, boolean fullScreen) {
      GraphicsDevice chosenGraphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
      double maxIntersectionArea = -1;
      for (GraphicsDevice graphicsDevice : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
         double intersectionArea = getIntersectionArea(window.getBounds(), graphicsDevice.getDefaultConfiguration().getBounds());
         if (intersectionArea > maxIntersectionArea) {
            maxIntersectionArea = intersectionArea;
            chosenGraphicsDevice = graphicsDevice;
         }
      }
      window.dispose();
      if (window instanceof JFrame frame) {
         frame.setUndecorated(fullScreen);
      }
      if (window instanceof JDialog dialog) {
         dialog.setUndecorated(fullScreen);
      }
      chosenGraphicsDevice.setFullScreenWindow(fullScreen ? window : null);
      window.setVisible(true);
   }

   public static boolean isFullScreen(Window window) {
      return window.getGraphicsConfiguration().getDevice().getFullScreenWindow() == window;
   }

   public static Rectangle getScreenBounds(Component component) {
      Point point = new Point();
      SwingUtilities.convertPointToScreen(point, component);
      return new Rectangle(point.x, point.y, component.getWidth(), component.getHeight());
   }

   public static double getIntersectionArea(Rectangle2D a, Rectangle2D b) {
      Rectangle2D c = a.createIntersection(b);
      return c.getWidth() * c.getHeight();
   }

   public static void setFileListTransferHandler(JFileChooser fileChooser) {
      fileChooser.setTransferHandler(new FileListTransferHandler(files -> {
         if (fileChooser.isMultiSelectionEnabled()) {
            fileChooser.setSelectedFiles(FileUtils.toFiles(files).toArray(File[]::new));
         } else {
            fileChooser.setSelectedFile(files.getFirst().toFile());
         }
      }));
   }

   private static boolean desktopSupportedOrShowErrorDialog(@Nullable Component referenceComponent, Desktop.Action action) {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(action)) {
         return true;
      } else {
         showErrorDialog(referenceComponent, "Unsupported desktop action: " + action);
         return false;
      }
   }

   public static void desktopMail(URI uri, @Nullable Component referenceComponent) {
      if (!desktopSupportedOrShowErrorDialog(referenceComponent, Desktop.Action.MAIL)) {
         return;
      }
      try {
         Desktop.getDesktop().mail(uri);
      } catch (IOException e) {
         showErrorDialog(referenceComponent, "Error opening " + uri, e);
      }
   }

   public static void desktopBrowse(URI uri, @Nullable Component referenceComponent) {
      if (!desktopSupportedOrShowErrorDialog(referenceComponent, Desktop.Action.BROWSE)) {
         return;
      }
      try {
         Desktop.getDesktop().browse(uri);
      } catch (IOException e) {
         showErrorDialog(referenceComponent, "Error opening " + uri, e);
      }
   }

   public static void desktopOpen(Path file, @Nullable Component referenceComponent) {
      if (!desktopSupportedOrShowErrorDialog(referenceComponent, Desktop.Action.OPEN)) {
         return;
      }
      try {
         Desktop.getDesktop().open(file.toFile());
      } catch (IOException e) {
         showErrorDialog(referenceComponent, "Error opening " + file, e);
      }
   }

   public static void desktopEdit(Path file, @Nullable Component referenceComponent) {
      if (!desktopSupportedOrShowErrorDialog(referenceComponent, Desktop.Action.EDIT)) {
         return;
      }
      try {
         Desktop.getDesktop().edit(file.toFile());
      } catch (IOException e) {
         showErrorDialog(referenceComponent, "Error opening " + file, e);
      }
   }

   public static void taskbarSetIconImage(Image image) {
      if (Taskbar.isTaskbarSupported()) {
         Taskbar taskbar = Taskbar.getTaskbar();
         if (taskbar.isSupported(Taskbar.Feature.ICON_IMAGE)) {
            taskbar.setIconImage(image);
         }
      }
   }

   public static boolean fileExists(Path file, @Nullable Component referenceComponent) {
      Boolean exists = new WorkerDialog(referenceComponent, "Accessing file:\n" + file)
            .setWaitUntilFinishedIfCancelled(false)
            .startMakeValue(asyncHandle -> Files.exists(file));
      return exists != null && exists;
   }

   public static void shrinkSizeTo(Component component, Dimension maxSize) {
      shrinkSizeTo(component, maxSize.width, maxSize.height);
   }

   public static void shrinkSizeTo(Component component, int maxWidth, int maxHeight) {
      component.setSize(Math.min(component.getWidth(), maxWidth), Math.min(component.getHeight(), maxHeight));
   }

   public static void expandSizeTo(Component component, Dimension minSize) {
      expandSizeTo(component, minSize.width, minSize.height);
   }

   public static void expandSizeTo(Component component, int minWidth, int minHeight) {
      component.setSize(Math.max(component.getWidth(), minWidth), Math.max(component.getHeight(), minHeight));
   }

   public static void expandSizeWith(Component component, int extraWidth, int extraHeight) {
      component.setSize(component.getWidth() + extraWidth, component.getHeight() + extraHeight);
   }

   public static <T extends Container> T add(T container, Component... components) {
      for (Component component : components) {
         container.add(component);
      }
      return container;
   }

   public static void replaceContent(Container container, Component component) {
      container.removeAll();
      container.add(component);
      container.revalidate();
      container.repaint();
   }

   public static void detachFromParent(Component component) {
      Container parent = component.getParent();
      if (parent != null) {
         parent.remove(component);
         parent.revalidate();
         parent.repaint();
      }
   }

   public static Stream<Component> hierarchyStream(Component component) {
      return Stream.concat(Stream.of(component),
            component instanceof Container container
                  ? children(container).flatMap(GuiUtils::hierarchyStream)
                  : Stream.empty());
   }

   private static Stream<Component> children(Container container) {
      return IntStream.range(0, container.getComponentCount())
            .mapToObj(container::getComponent);
   }

   public static void setUIFontFactor(double factor) {
      for (Map.Entry<Object, Object> entry : UIManager.getDefaults().entrySet()) { // forEach not overridden in MultiUIDefaults
         Object key = entry.getKey();
         Object value = entry.getValue();
         if (value instanceof FontUIResource defaultFont) {
            UIManager.put(key, new FontUIResource(defaultFont.getName(), defaultFont.getStyle(), (int) (factor * defaultFont.getSize())));
         }
      }
   }

   public static void syncSize(Window window, Preferences preferences, String key) {
      String value = preferences.get(key, "");
      try {
         String[] parts = value.split("x");
         window.setSize(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
      } catch (Exception e) {
         if (!value.isBlank()) {
            Log.global.log(Level.WARNING, "Could not set window size: " + value, e);
         }
      }
      window.addComponentListener(new ComponentAdapter() {
         @Override
         public void componentResized(ComponentEvent e) {
            preferences.put(key, window.getWidth() + "x" + window.getHeight());
         }
      });
   }

   public static String getComponentPlainText(Component component) {
      String text = getComponentText(component);
      if (text.startsWith("<html>")) {
         text = htmlToPlainText(text);
      }
      return text;
   }

   public static String getComponentText(Component component) {
      return switch (component) {
         case JTextComponent textComponent -> {
            yield textComponent.getText();
         }
         case JLabel label -> {
            yield label.getText();
         }
         case AbstractButton button -> {
            yield button.getText();
         }
         case JTableHeader tableHeader -> {
            TableModel model = tableHeader.getTable().getModel();
            yield IntStream.range(0, model.getColumnCount())
                  .mapToObj(model::getColumnName)
                  .collect(Collectors.joining("\n"));
         }
         default -> "";
      };
   }

   public static String htmlToPlainText(String html) {
      StringBuilder sb = new StringBuilder();
      HTMLEditorKit.ParserCallback parserCallback = new HTMLEditorKit.ParserCallback() {
         @Override
         public void handleText(char[] data, int pos) {
            sb.append(data);
         }
      };
      try {
         new ParserDelegator().parse(new StringReader(html), parserCallback, false);
      } catch (IOException e) {
         throw new ShouldNotHappenException("Error parsing html: " + html, e);
      }
      return sb.toString();
   }

   public static void addUndoSupport(JTextComponent textComponent) {
      UndoManager undoManager = new UndoManager();
      textComponent.getDocument().addUndoableEditListener(e -> {
         undoManager.addEdit(e.getEdit());
      });
      textComponent.addKeyListener(new KeyAdapter() {
         @Override
         public void keyPressed(KeyEvent e) {
            switch (e.getKeyCode()) {
               case KeyEvent.VK_Y -> {
                  if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                     if (undoManager.canRedo()) {
                        undoManager.redo();
                     }
                  }
               }
               case KeyEvent.VK_Z -> {
                  if (e.getModifiersEx() == KeyEvent.CTRL_DOWN_MASK) {
                     if (undoManager.canUndo()) {
                        undoManager.undo();
                     }
                  } else if (e.getModifiersEx() == (KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK)) {
                     if (undoManager.canRedo()) {
                        undoManager.redo();
                     }
                  }
               }
               default -> {
               }
            }
         }
      });
   }

   public static void neverUpdateCaret(JTextComponent descriptionLabel) {
      DefaultCaret caret = (DefaultCaret) descriptionLabel.getCaret();
      caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
   }

   public static JTextPane readonlyHtmlTextPane(String html) {
      JTextPane textPane = new JTextPane();
      textPane.setEditable(false);
      textPane.setContentType("text/html");
      StyleSheet styleSheet = new StyleSheet();
      styleSheet.addRule("a { text-decoration: none; }");
      ((HTMLDocument) textPane.getDocument()).getStyleSheet().addStyleSheet(styleSheet);
      textPane.putClientProperty(JTextPane.HONOR_DISPLAY_PROPERTIES, true);
      neverUpdateCaret(textPane);
      textPane.setText(html);
      return textPane;
   }

   public static JTextPane labelLikeHtmlTextPane(String html) {
      JTextPane textPane = readonlyHtmlTextPane(html);
      textPane.setBackground(UiUtils.panelBackground());
      textPane.setBorder(null);
      return textPane;
   }

   public static JTextPane labelLikeHtmlTextPane(String html, Consumer<String> hrefListener) {
      return addHrefListener(labelLikeHtmlTextPane(html), hrefListener);
   }

   public static JTextPane addHrefListener(JTextPane textPane, Consumer<String> hrefListener) {
      textPane.addHyperlinkListener(e -> {
         if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            hrefListener.accept(e.getDescription());
         }
      });
      return textPane;
   }

   public static @Nullable <T> T pointToListItem(JList<T> list, Point point) {
      int i = list.locationToIndex(point);
      if (i < 0 || !list.getCellBounds(i, i).contains(point)) {
         return null;
      }
      return list.getModel().getElementAt(i);
   }

   public static int getKeyModifiers(ActionEvent actionEvent) {
      int keyModifierMask = ActionEvent.SHIFT_MASK | ActionEvent.CTRL_MASK | ActionEvent.META_MASK | ActionEvent.ALT_MASK;
      return actionEvent.getModifiers() & keyModifierMask;
   }

   public static int getActionEventModifiers(InputEvent inputEvent) {
      return (inputEvent.isShiftDown() ? ActionEvent.SHIFT_MASK : 0)
            | (inputEvent.isControlDown() ? ActionEvent.CTRL_MASK : 0)
            | (inputEvent.isMetaDown() ? ActionEvent.META_MASK : 0)
            | (inputEvent.isAltDown() ? ActionEvent.ALT_MASK : 0);
   }
}
