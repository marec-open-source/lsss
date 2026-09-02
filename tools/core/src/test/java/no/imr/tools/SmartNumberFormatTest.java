package no.imr.tools;

import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.ParsePosition;

import static org.junit.jupiter.api.Assertions.*;

final class SmartNumberFormatTest {
   @Test
   void format() {
      SmartNumberFormat f = new SmartNumberFormat();
      assertEquals("23", f.format(23));
      assertEquals("23", f.format(23.0));
      assertEquals("23.4", f.format(23.4));

      assertEquals("999.9999999", f.format(999.9999999));
      assertEquals("1000", f.format(999.99999999));
      assertEquals("1E3", f.format(1000f));

      assertEquals("0.0123457", f.format(0.0123456789));
      assertEquals("1.23456789E-3", f.format(0.00123456789));
      assertEquals("123.456789E-6", f.format(0.000123456789));

      assertEquals("2.3456789E9", f.format(2.3456789e9));
      assertEquals("23.456789E9", f.format(2.3456789e10));
      assertEquals("234.56789E9", f.format(2.3456789e11));
      assertEquals("2.3456789E12", f.format(2.3456789e12));

      assertEquals("2.3456789E120", f.format(2.3456789e120));
   }

   @Test
   void parse() throws ParseException {
      SmartNumberFormat f = new SmartNumberFormat();
      assertEquals(23L, f.parse("23"));
      assertEquals(23.4, f.parse("23.4"));
      assertEquals(23456789000L, f.parse("2.3456789E10"));
      assertEquals(2.3456789e120, f.parse("2.3456789E120"));
      assertNull(f.parse("", new ParsePosition(0)));
      assertNull(f.parse("a", new ParsePosition(0)));
   }
}
