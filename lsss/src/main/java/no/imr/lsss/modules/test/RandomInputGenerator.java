package no.imr.lsss.modules.test;

import no.imr.lsss.LSSS;
import no.imr.lsss.modules.echogram.PelagicEchogramModule;
import no.imr.tools.RandomUtils;
import no.imr.tools.ShouldNotHappenException;
import no.imr.tools.logging.Log;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;

final class RandomInputGenerator {
   private final JButton button = new JButton("Randomly generate input");
   private final PelagicEchogramModule pelagicEchogramModule;
   private final Map<Integer, Boolean> mouseButtons = new LinkedHashMap<>();
   private final Map<Integer, Boolean> keys = new LinkedHashMap<>();
   private final Robot robot = new Robot();
   private final Random random = new Random();
   private final Timer timer = new Timer(1, _ -> tick());
   private Point previousMouseLocation = new Point();
   private int counter;

   private RandomInputGenerator(LSSS lsss) throws AWTException {
      pelagicEchogramModule = lsss.getModuleManager().getModule(PelagicEchogramModule.class);
      button.addActionListener(_ -> start());
   }

   static JComponent create(LSSS lsss) {
      try {
         return new RandomInputGenerator(lsss).getComponent();
      } catch (AWTException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
         return new JLabel(e.toString());
      }
   }

   private JComponent getComponent() {
      return button;
   }

   private void start() {
      long seed = random.nextLong();
      Log.global.info("seed = " + seed);
      //random.setSeed(8338632942019162780L);

      mouseButtons.put(InputEvent.BUTTON1_DOWN_MASK, false);
      mouseButtons.put(InputEvent.BUTTON2_DOWN_MASK, false);
      //mouseButtons.put(InputEvent.BUTTON3_DOWN_MASK, false);

      keys.put(KeyEvent.VK_SPACE, false);
      keys.put(KeyEvent.VK_ESCAPE, false);
      keys.put(KeyEvent.VK_SHIFT, false);

      previousMouseLocation = moveMouse(0, 0);
      counter = 0;
      timer.start();
   }

   private void stop() {
      timer.stop();
      mouseButtons.keySet().forEach(robot::mouseRelease);
      keys.keySet().forEach(robot::keyRelease);
   }

   private void tick() {
      if (counter >= 100) {
         Log.global.info("Input generation stopped (counter = " + counter + ")");
         stop();
         return;
      }

      PointerInfo pointerInfo = MouseInfo.getPointerInfo();
      if (pointerInfo != null && !previousMouseLocation.equals(pointerInfo.getLocation())) {
         Log.global.info("Input generation stopped (mouse moved)");
         stop();
         return;
      }

      counter++;

      switch (random.nextInt(4)) {
         case 0 -> {
            int x = random.nextInt(pelagicEchogramModule.getWidth());
            int y = random.nextInt(pelagicEchogramModule.getHeight());
            previousMouseLocation = moveMouse(x, y);
            Log.global.info(counter + ": mouse move to " + x + ", " + y);
         }
         case 1 -> {
            int wheel = random.nextInt(-2, 3);
            robot.mouseWheel(wheel);
            Log.global.info(counter + ": mouse scroll " + wheel);
         }
         case 2 -> {
            Integer mouseButton = RandomUtils.get(random, mouseButtons.keySet());
            mouseButtons.put(mouseButton, toggleMouseButton(mouseButtons.get(mouseButton), mouseButton));
            Log.global.info(counter + ": " + MouseEvent.getModifiersExText(mouseButton) + " " + (mouseButtons.get(mouseButton) ? "down" : "up"));
         }
         case 3 -> {
            Integer key = RandomUtils.get(random, keys.keySet());
            keys.put(key, toggleKey(keys.get(key), key));
            Log.global.info(counter + ": " + KeyEvent.getKeyText(key) + " " + (keys.get(key) ? "down" : "up"));
         }
         default -> {
            throw new ShouldNotHappenException();
         }
      }
   }

   private boolean toggleMouseButton(boolean down, int button) {
      if (down) {
         robot.mouseRelease(button);
      } else {
         robot.mousePress(button);
      }
      return !down;
   }

   private boolean toggleKey(boolean down, int keyCode) {
      if (down) {
         robot.keyRelease(keyCode);
      } else {
         robot.keyPress(keyCode);
      }
      return !down;
   }

   private Point moveMouse(int x, int y) {
      Point p = new Point(x, y);
      SwingUtilities.convertPointToScreen(p, pelagicEchogramModule.getComponent());
      robot.mouseMove(p.x, p.y);
      return p;
   }
}
