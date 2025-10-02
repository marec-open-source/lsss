package no.imr.lsss.framework.backup.pojo;

import com.fasterxml.jackson.annotation.JsonInclude;
import no.imr.tools.annotations.ReflectionEntryPoint;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BackupInfo {
   public @Nullable Path outputDirectory;
   public List<DirInfo> directories = new ArrayList<>();
   public Map<String, Boolean> options = new LinkedHashMap<>();

   public BackupInfo() {
   }

   public static final class DirInfo {
      @JsonInclude(JsonInclude.Include.NON_EMPTY)
      public String id = "";
      public @Nullable Path sourceDir;
      public boolean selected;

      @ReflectionEntryPoint
      public DirInfo() {
      }

      public DirInfo(String id, Path sourceDir, boolean selected) {
         this.id = id;
         this.sourceDir = sourceDir;
         this.selected = selected;
      }
   }
}
