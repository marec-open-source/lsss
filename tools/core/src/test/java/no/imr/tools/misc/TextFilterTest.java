package no.imr.tools.misc;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class TextFilterTest {
   @Test
   void empty() {
      TextFilter filter = new TextFilter("");
      assertTrue(filter.test(""));
      assertTrue(filter.test("aa"));

      assertTrue(filter.test(List.of()));
      assertTrue(filter.test(List.of("aa", "bb", "cc")));
   }

   @Test
   void oneWord() {
      TextFilter filter = new TextFilter("aa");
      assertTrue(filter.test("aAaA"));
      assertTrue(filter.test("xx Aa bb"));
      assertFalse(filter.test("xx yy zz"));

      assertTrue(filter.test(List.of("xx", "AA", "CC")));
      assertFalse(filter.test(List.of("xx", "yy", "dd")));
   }

   @Test
   void exclude() {
      TextFilter filter = new TextFilter("aa bb -cc -dd");
      assertTrue(filter.test("aaa BbB xx"));
      assertTrue(filter.test("bb xx Aax"));
      assertFalse(filter.test("bb xx Aa CC zz"));
      assertFalse(filter.test("bb ddxx Aa zz"));
      assertFalse(filter.test("bb ddxx Aa ddd zz"));

      assertTrue(filter.test(List.of("aa", "BB", "xx")));
      assertFalse(filter.test(List.of("aa", "xx", "yy")));
      assertFalse(filter.test(List.of("aa", "BB", "CC")));
   }
}
