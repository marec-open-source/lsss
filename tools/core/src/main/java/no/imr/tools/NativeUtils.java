package no.imr.tools;

import no.imr.tools.logging.LoggingManager;

import java.nio.file.Path;

public final class NativeUtils {
   private NativeUtils() {
   }

   public static boolean is64Bit() {
      return getDataModel().equals("64");
   }

   public static String getDataModel() {
      String dataModel = System.getProperty("sun.arch.data.model");
      if (dataModel != null) {
         return dataModel;
      }

      String vmName = System.getProperty("java.vm.name");
      if (vmName != null && vmName.contains("64")) {
         return "64";
      } else {
         return "32";
      }
   }

   public static OperatingSystem getOperatingSystem() {
      String osName = System.getProperty("os.name");
      if (osName.contains("Windows")) {
         return OperatingSystem.WIN;
      }
      if (osName.contains("Linux")) {
         return OperatingSystem.LINUX;
      }
      if (osName.contains("Mac OS")) {
         return OperatingSystem.MACOS;
      }
      throw new IllegalStateException("Unsupported operating system: " + osName);
   }

   public static void loadNativeLib(String libraryName) {
      getOperatingSystem().loadNativeLibrary(libraryName);
   }

   public enum OperatingSystem {
      WIN("win", "", ".dll"),
      LINUX("linux", "lib", ".so"),
      MACOS("macos", "lib", ".jnilib");

      public final String type;
      public final String prefix;
      public final String suffix;

      OperatingSystem(String type, String prefix, String suffix) {
         this.type = type;
         this.prefix = prefix;
         this.suffix = suffix;
      }

      private void loadNativeLibrary(String libraryName) {
         Path file = LoggingManager.getTopInstallationDir().resolve("lib").resolve("native")
               .resolve(type + getDataModel()).resolve(prefix + libraryName + suffix);
         System.load(file.toString());
      }
   }
}
