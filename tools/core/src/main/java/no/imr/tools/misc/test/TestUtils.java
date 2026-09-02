package no.imr.tools.misc.test;

import no.imr.tools.ExitCodes;
import no.imr.tools.Utils;
import no.imr.tools.adm.AppEvent;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LoggingManager;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.icons.MiscIcons;
import org.jspecify.annotations.Nullable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.DoNotCallGarbageCollectionExplicitly")
public final class TestUtils {
   private static @Nullable Object memoryAllocation;

   private TestUtils() {
   }

   public static JMenu createDebugMenu(LoggingManager loggingManager) {
      return createDebugMenu(loggingManager, Utils.emptyConsumer());
   }

   public static JMenu createDebugMenu(LoggingManager loggingManager, Consumer<JMenu> additional) {
      JMenu menu = new JMenu("Debug");

      GuiUtils.autoCreateContentMenu(menu, () -> {
         JMenuItem appEventItem = menu.add("Create app event");
         appEventItem.setMnemonic(KeyEvent.VK_E);
         appEventItem.setDisplayedMnemonicIndex(appEventItem.getText().indexOf("event"));
         appEventItem.addActionListener(_ -> loggingManager.getAppEventHandler().handle(AppEvent.create(loggingManager, AppEvent.Type.info, "Testing...")));

         JMenuItem logWarningItem = menu.add("Log warning");
         logWarningItem.setMnemonic(KeyEvent.VK_W);
         logWarningItem.addActionListener(_ -> Log.global.warning("Test warning..."));

         JMenuItem logSilentWarningItem = menu.add("Log silent warning");
         logSilentWarningItem.setMnemonic(KeyEvent.VK_S);
         logSilentWarningItem.addActionListener(_ -> Log.global.log(Log.SILENT_WARNING, "Test silent warning..."));

         Window window = GuiUtils.windowForComponent(menu);

         JMenuItem optionPaneItem = menu.add("Show JOptionPane");
         optionPaneItem.setMnemonic(KeyEvent.VK_J);
         optionPaneItem.addActionListener(_ -> JOptionPane.showMessageDialog(window, "Test JOptionPane"));

         JMenuItem openTmpItem = menu.add("Open tmp dir");
         openTmpItem.setMnemonic(KeyEvent.VK_T);
         openTmpItem.addActionListener(_ -> GuiUtils.desktopOpen(Utils.getTmpDir(), window));

         JMenuItem fullScreenItem = menu.add("Full screen");
         fullScreenItem.setMnemonic(KeyEvent.VK_F);
         if (window != null) {
            boolean isFullScreen = GuiUtils.isFullScreen(window);
            MiscIcons.check(isFullScreen).on(fullScreenItem);
            fullScreenItem.addActionListener(_ -> GuiUtils.setFullScreen(window, !isFullScreen));
         } else {
            fullScreenItem.setEnabled(false);
         }

         JMenuItem largeMemoryAllocationItem = menu.add(memoryAllocation != null ? "Free large memory allocation" : "Make large memory allocation");
         largeMemoryAllocationItem.setMnemonic(KeyEvent.VK_M);
         largeMemoryAllocationItem.setDisplayedMnemonicIndex(largeMemoryAllocationItem.getText().indexOf("memory"));
         largeMemoryAllocationItem.addActionListener(_ -> {
            if (memoryAllocation == null) {
               System.gc();
               int mb = 1024 * 1024;
               Runtime runtime = Runtime.getRuntime();
               long bytesToAllocate = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory() - 1024L * mb;
               while (bytesToAllocate > 0) {
                  try {
                     Log.global.info("Trying to allocate " + bytesToAllocate + " bytes");
                     memoryAllocation = new byte[(int) (bytesToAllocate / mb)][mb];
                     break;
                  } catch (OutOfMemoryError _) {
                     bytesToAllocate -= 1024L * mb;
                  }
               }
            } else {
               memoryAllocation = null;
               System.gc();
            }
         });

         JMenuItem outOfMemoryItem = menu.add("OutOfMemoryError");
         outOfMemoryItem.setMnemonic(KeyEvent.VK_O);
         outOfMemoryItem.addActionListener(_ -> {
            Double[][][] impossiblyLarge = new Double[Integer.MAX_VALUE][Integer.MAX_VALUE][Integer.MAX_VALUE];
            impossiblyLarge[0][0][0] = Double.NaN; // Usage to avoid inspection warnings
            Log.global.warning("The impossible happened. " + impossiblyLarge[0][0][0]);
         });

         JMenuItem exitWithErrorItem = menu.add("Exit with error");
         exitWithErrorItem.setMnemonic(KeyEvent.VK_X);
         exitWithErrorItem.addActionListener(_ -> System.exit(ExitCodes.TEST));

         additional.accept(menu);
      });

      return menu;
   }

   public static <T> void assertEquals(Set<T> expected, Set<T> actual) {
      if (expected.equals(actual)) {
         return;
      }
      String onlyInExpected = diff(expected, actual);
      String onlyInActual = diff(actual, expected);
      String message = "Only in expected: (" + onlyInExpected.lines().count() + ")\n"
            + onlyInExpected + "\n"
            + "Only in actual: (" + onlyInActual.lines().count() + ")\n"
            + onlyInActual;
      throw new AssertionError(message);
   }

   public static <T> String diff(Set<T> minuend, Set<T> subtrahend) {
      return minuend.stream()
            .filter(Predicate.not(subtrahend::contains))
            .map(Objects::toString)
            .sorted()
            .collect(Collectors.joining("\n"));
   }
}
