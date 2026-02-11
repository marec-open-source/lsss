package no.imr.tools.io;

import no.imr.tools.Utils;

import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class FilePredicates {
   private FilePredicates() {
   }

   public static Predicate<Path> includeAll() {
      return _ -> true;
   }

   public static Predicate<Path> endsWith(String suffix) {
      return fileNameFilter(fileName -> fileName.endsWith(suffix));
   }

   public static Predicate<Path> endsWithIgnoringCase(String suffix) {
      return fileNameFilter(fileName -> Utils.endsWithIgnoringCase(fileName, suffix));
   }

   public static Predicate<Path> fromPattern(Pattern pattern) {
      return fileNameFilter(fileName -> pattern.matcher(fileName).matches());
   }

   public static Predicate<Path> fileNameFilter(Predicate<String> fileNamePredicate) {
      return file -> {
         Path fileName = file.getFileName();
         return fileName != null && fileNamePredicate.test(fileName.toString());
      };
   }
}
