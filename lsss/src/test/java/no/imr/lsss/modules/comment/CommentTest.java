package no.imr.lsss.modules.comment;

import no.imr.tools.xml.XmlParseException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

final class CommentTest {
   @Test
   void time() {
      // Check that time is rounded to hundredth of a second to match database time.
      Instant time = Instant.ofEpochSecond(123456789);
      assertSame(time, new Comment(time, 0, 0, "").time());
      assertEquals(time, new Comment(time.plusNanos(1), "").time());
      assertEquals(time.plusMillis(990), new Comment(time.plusNanos(999_999_999), "").time());
   }

   @Test
   void xml() throws XmlParseException {
      Comment a = new Comment(Instant.ofEpochSecond(123456789, 999_999_999), 1, 0.1, "");
      assertEquals(a, Comment.fromXml(a.toXml()));

      Comment b = new Comment(Instant.ofEpochSecond(123456789, 999_999_999), "abc");
      assertEquals(b, Comment.fromXml(b.toXml()));
   }
}
