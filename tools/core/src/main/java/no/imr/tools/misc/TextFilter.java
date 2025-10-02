package no.imr.tools.misc;

import com.google.common.base.Splitter;
import no.imr.tools.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class TextFilter implements Predicate<String> {
   private final List<String> excludedWords = new ArrayList<>();
   private final List<String> includedWords = new ArrayList<>();

   public TextFilter(String query) {
      Splitter splitter = Splitter.on(Pattern.compile("\\s+")).omitEmptyStrings();
      for (String word : splitter.split(query)) {
         if (word.startsWith("-")) {
            if (word.length() > 1) {
               excludedWords.add(word.substring(1));
            }
         } else {
            includedWords.add(word);
         }
      }
   }

   @Override
   public boolean test(String text) {
      return test(List.of(text));
   }

   public boolean test(List<String> texts) {
      for (String excludedWord : excludedWords) {
         if (texts.stream().anyMatch(text -> Utils.containsIgnoringCase(text, excludedWord))) {
            return false;
         }
      }
      for (String includedWord : includedWords) {
         if (texts.stream().noneMatch(text -> Utils.containsIgnoringCase(text, includedWord))) {
            return false;
         }
      }
      return true;
   }
}
