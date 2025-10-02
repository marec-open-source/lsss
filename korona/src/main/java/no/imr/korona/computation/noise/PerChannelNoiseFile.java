package no.imr.korona.computation.noise;

import no.imr.tools.logging.Log;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

public final class PerChannelNoiseFile extends BaseNoiseFile {
   private final NavigableMap<Integer, NoiseData> perChannelNoiseMap = new TreeMap<>();

   public PerChannelNoiseFile(Path file) {
      super(file);

      initNoiseMap();
   }

   public PerChannelNoiseFile(@Nullable Path file, NavigableMap<Integer, NoiseData> noiseData) {
      super(file);

      perChannelNoiseMap.putAll(noiseData);
   }

   private void initNoiseMap() {
      try {
         Document document = readNoiseFile();
         if (document != null) {
            parseNoiseDocument(document, perChannelNoiseMap);
         }
      } catch (IOException e) {
         Log.global.log(Level.WARNING, e.getMessage(), e);
      }
   }

   private static void parseNoiseDocument(Document document, NavigableMap<Integer, NoiseData> perChannelNoiseMap) {
      for (Element element : document.getRootElement().elements()) {
         int channel = Integer.parseInt(element.attributeValue("channel"));
         float ne = Float.parseFloat(element.attributeValue("NE"));
         float nh = Float.parseFloat(element.attributeValue("NH"));
         perChannelNoiseMap.put(channel, new NoiseData(ne, nh));
      }
   }

   @Override
   protected Document mergedNoiseDocument(@Nullable Document documentOnFile) {
      //Writing the latest map to file, using values on file only if the latest map is missing some keys
      Element root = DocumentHelper.createElement("noise");
      NavigableMap<Integer, NoiseData> mergedMap;
      if (documentOnFile == null) {
         mergedMap = perChannelNoiseMap;
      } else {
         mergedMap = new TreeMap<>(perChannelNoiseMap);
         NavigableMap<Integer, NoiseData> parsedFileMap = new TreeMap<>();
         parseNoiseDocument(documentOnFile, parsedFileMap);
         for (Map.Entry<Integer, NoiseData> fileEntry : parsedFileMap.entrySet()) {
            Integer kHz = fileEntry.getKey();
            if (!mergedMap.containsKey(kHz)) {
               mergedMap.put(kHz, fileEntry.getValue());
            }
         }
      }
      for (Map.Entry<Integer, NoiseData> entry : mergedMap.entrySet()) {
         Integer channel = entry.getKey();
         NoiseData noiseData = entry.getValue();
         root.addElement("entry")
               .addAttribute("channel", Integer.toString(channel))
               .addAttribute("NE", Float.toString(noiseData.ne))
               .addAttribute("NH", Float.toString(noiseData.nh));
      }
      return DocumentHelper.createDocument(root);
   }

   public float getNe(int channel) {
      NoiseData noiseData = perChannelNoiseMap.get(channel);
      return noiseData != null ? noiseData.ne : 0;
   }

   public float getNh(int channel) {
      NoiseData noiseData = perChannelNoiseMap.get(channel);
      return noiseData != null ? noiseData.nh : 0;
   }

   public void write() {
      writeNoiseFile();
   }

   public Set<Map.Entry<Integer, NoiseData>> getEntries() {
      return perChannelNoiseMap.entrySet();
   }

   public record NoiseData(float ne, float nh) {
   }
}
