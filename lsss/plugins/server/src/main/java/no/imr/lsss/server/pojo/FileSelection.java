package no.imr.lsss.server.pojo;

import no.imr.tools.annotations.ReflectionEntryPoint;

public final class FileSelection {
   public int firstIndex;
   public int lastIndex;

   @ReflectionEntryPoint
   public FileSelection() {
   }

   public FileSelection(int firstIndex, int lastIndex) {
      this.firstIndex = firstIndex;
      this.lastIndex = lastIndex;
   }

   @Override
   public String toString() {
      return "FileSelection{" +
            "firstIndex=" + firstIndex +
            ", lastIndex=" + lastIndex +
            '}';
   }
}
