package no.imr.tools.swing;

import no.imr.tools.Utils;
import no.imr.tools.io.FileType;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A file filter using file name suffixes.
 * Note that the character '.' has no special significance here.
 */
public final class SuffixFileFilter extends FileFilter {
   private final List<String> suffixes;
   private final String fullDescription;

   public SuffixFileFilter(String description, List<String> suffixes) {
      this.suffixes = suffixes;
      fullDescription = suffixes.stream()
            .map(suffix -> "*" + suffix)
            .collect(Collectors.joining(", ", description + " (", ")"));
   }

   public SuffixFileFilter(String description, String suffix) {
      this(description, List.of(suffix));
   }

   public SuffixFileFilter(FileType fileType) {
      this(fileType.description(), fileType.suffix());
   }

   @Override
   public String getDescription() {
      return fullDescription;
   }

   @Override
   public boolean accept(File file) {
      String name = file.getName();
      for (String suffix : suffixes) {
         if (Utils.endsWithIgnoringCase(name, suffix)) {
            return true;
         }
      }
      return file.isDirectory();
   }
}
