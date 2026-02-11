package no.imr.lsss.modules.interpretation;

import no.imr.tools.Utils;
import no.imr.tools.test.JUnitUtils;

import java.lang.management.ManagementFactory;

@SuppressWarnings("PMD.SystemPrintln")
final class InterpretationModuleMain {
   private InterpretationModuleMain() {
   }

   static void main() {
      InterpretationModuleTest interpretationModuleTest = new InterpretationModuleTest();
      interpretationModuleTest.beforeEach();

      for (int i = 0; i < Integer.MAX_VALUE; i++) {
         //System.gc();
         Runtime r = Runtime.getRuntime();
         long m = r.maxMemory() - r.totalMemory() + r.freeMemory();
         System.out.println("i = " + i + ", threads = " + ManagementFactory.getThreadMXBean().getThreadCount() + ", free memory = " + Utils.getByteSizeString(m));

         interpretationModuleTest.testStore();
         //testObjectNumber();
         //testInheritInterpretation();
         //ticket5();
         JUnitUtils.assertMaxLogLevel();
      }

      interpretationModuleTest.afterEach();
   }
}
