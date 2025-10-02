package no.imr.lsss.modules.filedraw;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class FileDrawDataLoaderTest {
   @Test
   void readLine() throws IOException {
      FileDrawLine line = FileDrawDataLoader.readLine(new BufferedReader(new StringReader("""
            LSSS 3 3.00.41
            8
            # Comment a

            20001214 160237000  150.321984 3
            20001214 1603000000 160.043990 0
            # Comment b
            20001214 16063000   170.743539 3

            """)));
      FileDrawLine expected = new FileDrawLine(List.of(
            new FileDrawPoint(Instant.parse("2000-12-14T16:02:37Z").toEpochMilli(), 150.321984f),
            new FileDrawPoint(Instant.parse("2000-12-14T16:03:00Z").toEpochMilli(), 160.043990f),
            new FileDrawPoint(Instant.parse("2000-12-14T16:06:30Z").toEpochMilli(), 170.743539f)
      ));
      assertEquals(expected, line);
   }
}
