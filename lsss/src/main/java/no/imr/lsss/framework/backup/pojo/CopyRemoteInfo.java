package no.imr.lsss.framework.backup.pojo;

import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CopyRemoteInfo {
   public List<DirInfo> directories = new ArrayList<>();
   public Map<String, Boolean> options = new LinkedHashMap<>();

   public CopyRemoteInfo() {
   }

   public static final class DirInfo {
      public String id = "";
      public @Nullable Path destinationDir;
      public boolean selected;

      @ReflectionEntryPoint
      public DirInfo() {
      }

      public DirInfo(String id, Path destinationDir, boolean selected) {
         this.id = id;
         this.destinationDir = destinationDir;
         this.selected = selected;
      }
   }
}
