package no.imr.tools.main;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;

/**
 * Used by startup scripts.
 */
@SuppressWarnings("PMD.SystemPrintln")
final class PrintMaxMemoryMbMain {
   private PrintMaxMemoryMbMain() {
   }

   private static int getMaxMemoryMB() {
      if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean bean) {
         long totalMB = bean.getTotalMemorySize() / (1024 * 1024);
         return Math.clamp(totalMB * 2 / 3, 3000, 30_000);
      } else {
         return 3000;
      }
   }

   public static void main(String[] args) {
      System.out.println(getMaxMemoryMB());
   }
}
