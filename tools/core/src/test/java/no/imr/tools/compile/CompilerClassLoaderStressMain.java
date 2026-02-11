package no.imr.tools.compile;

import java.util.Map;
import java.util.WeakHashMap;

@SuppressWarnings("PMD.SystemPrintln")
final class CompilerClassLoaderStressMain {
   private CompilerClassLoaderStressMain() {
   }

   static void main() throws CompileException {
      // For verifying that dynamically loaded classes get garbage collected.

      Map<Object, Boolean> map = new WeakHashMap<>();
      for (int i = 0; i < 1000000; i++) {
         Object obj = CompilerClassLoader.instantiate(Object.class, "T", "public class T {}");
         map.put(obj, true);
         if (i % 100 == 0) {
            System.out.println(i + " total mem: " + Runtime.getRuntime().totalMemory() + ", map size: " + map.size());
         }
      }
   }
}
