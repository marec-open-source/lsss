package no.imr.tools;

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
}
