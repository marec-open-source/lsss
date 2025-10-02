package no.imr.lsss.modules.trawl.spd;

import no.imr.tools.logging.Log;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class SpdParser {
   private final List<SpdStation> stations = new ArrayList<>();
   private @Nullable SLine sLine;
   private List<SpdTarget> targets = new ArrayList<>();
   private @Nullable TLine tLine;
   private List<ULine> uLines = new ArrayList<>();
   private List<VLine> vLines = new ArrayList<>();

   private SpdParser() {
   }

   public static List<SpdStation> parse(BufferedReader reader) throws IOException {
      SpdParser parser = new SpdParser();
      parser.doParse(reader);
      return parser.stations;
   }

   private void doParse(BufferedReader reader) throws IOException {
      while (true) {
         String line = reader.readLine();
         if (line == null) {
            break;
         }
         try {
            switch (line.charAt(0)) {
               case 'S' -> {
                  endCurrentStation();
                  sLine = new SLine(line);
               }
               case 'T' -> {
                  if (sLine != null) {
                     endCurrentTarget();
                     tLine = new TLine(line);
                  }
               }
               case 'U' -> {
                  if (tLine != null) {
                     uLines.add(new ULine(line));
                  }
               }
               case 'V' -> {
                  if (tLine != null) {
                     vLines.add(new VLine(line));
                  }
               }
               default -> {
               }
            }
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error parsing line " + line, e);
         }
      }
      endCurrentStation();
   }

   private void endCurrentStation() {
      endCurrentTarget();
      if (sLine != null) {
         stations.add(new SpdStation(sLine, targets));
      }
      sLine = null;
      targets = new ArrayList<>();
   }

   private void endCurrentTarget() {
      if (tLine != null) {
         targets.add(new SpdTarget(tLine, uLines, vLines));
      }
      tLine = null;
      uLines = new ArrayList<>();
      vLines = new ArrayList<>();
   }
}
