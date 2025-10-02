package no.imr.korona.data.datamanager.labelling;

import no.imr.korona.apps.relay.KoronaRelayUtils;
import no.imr.korona.data.track.SegmentHandle;
import no.imr.tools.Utils;
import no.imr.tools.logging.Log;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

public final class DataFileLabelling {
   public static final Color DEFAULT_COLOR = ColorUtils.PALETURQUOISE;

   private final Map<Set<DataFileLabel>, Set<DataFileLabel>> labelSetInterner = new WeakHashMap<>();
   private final List<DataFileLabel> allLabels = new ArrayList<>();
   private final Map<String, Set<DataFileLabel>> fileNameToLabels = new HashMap<>();

   public DataFileLabelling(Path file) {
      Document document;
      try {
         document = XmlUtils.readDocumentIfExists(file);
      } catch (IOException e) {
         Log.global.warning("Error reading file " + file + ": " + e);
         return;
      }
      if (document == null) {
         return;
      }
      Element topElement = document.getRootElement();

      Map<String, DataFileLabel> idToLabel = new HashMap<>();
      Element labelsElement = topElement.element("labels");
      if (labelsElement != null) {
         for (Element labelElement : labelsElement.elements()) {
            String id = labelElement.attributeValue("id");
            if (id == null) {
               continue;
            }
            String title = labelElement.attributeValue("title", "");
            String description = labelElement.attributeValue("description", "");
            Color color = ColorUtils.parseColor(labelElement.attributeValue("color", ""));
            if (color == null) {
               color = DEFAULT_COLOR;
            }
            DataFileLabel label = new DataFileLabel(title, description, color);
            idToLabel.put(id, label);
            allLabels.add(label);
         }
      }

      Element filesElement = topElement.element("files");
      if (filesElement != null) {
         Map<String, Set<DataFileLabel>> idsToLabelSet = new HashMap<>();
         for (Element fileElement : filesElement.elements()) {
            String fileName = fileElement.attributeValue("name");
            if (fileName == null) {
               continue;
            }
            String labelIds = fileElement.attributeValue("labels", "");
            Set<DataFileLabel> labels = idsToLabelSet.computeIfAbsent(labelIds, k -> {
               return Arrays.stream(k.split(","))
                     .map(idToLabel::get)
                     .filter(Objects::nonNull)
                     .collect(Collectors.toUnmodifiableSet());
            });
            setLabels(fileName, labels);
         }
      }
   }

   private @Nullable Element toXml() {
      if (allLabels.isEmpty()) {
         return null;
      }
      Element topElement = DocumentHelper.createElement("dataFileLabels");

      Element labelsElement = topElement.addElement("labels");
      Map<DataFileLabel, String> labelToId = new HashMap<>();
      for (DataFileLabel label : Utils.sorted(allLabels)) {
         String id = Integer.toString(labelToId.size() + 1);
         labelToId.put(label, id);
         labelsElement.addElement("label")
               .addAttribute("id", id)
               .addAttribute("color", ColorUtils.colorToHex(label.backgroundColor))
               .addAttribute("title", label.title)
               .addAttribute("description", label.description);
      }

      Element filesElement = topElement.addElement("files");
      Map<Set<DataFileLabel>, String> labelsToIds = new HashMap<>();
      for (String fileName : Utils.sorted(fileNameToLabels.keySet())) {
         Set<DataFileLabel> labels = fileNameToLabels.get(fileName);
         String ids = labelsToIds.computeIfAbsent(labels, k -> {
            return Utils.sorted(k).stream()
                  .map(labelToId::get)
                  .collect(Collectors.joining(","));
         });
         filesElement.addElement("file")
               .addAttribute("name", fileName)
               .addAttribute("labels", ids);
      }

      return topElement;
   }

   public void save(Path file) {
      Element element = toXml();
      if (element != null) {
         try {
            XmlUtils.writeDocument(element, file);
         } catch (IOException e) {
            Log.global.warning("Error saving file " + file + ": " + e);
         }
      } else {
         try {
            Files.deleteIfExists(file);
         } catch (IOException e) {
            Log.global.warning("Error deleting file " + file + ": " + e);
         }
      }
   }

   public Collection<DataFileLabel> getAllLabels() {
      return allLabels;
   }

   public @Nullable DataFileLabel getLabelByTitle(String title) {
      return allLabels.stream()
            .filter(label -> label.title.equals(title))
            .findFirst()
            .orElse(null);
   }

   public void addNewLabel(DataFileLabel label) {
      allLabels.add(label);
   }

   private static String toFileName(SegmentHandle segmentHandle) {
      return KoronaRelayUtils.baseNameWithoutKoronaSuffix(segmentHandle);
   }

   public Set<DataFileLabel> getLabels(SegmentHandle segmentHandle) {
      String fileName = toFileName(segmentHandle);
      return fileNameToLabels.getOrDefault(fileName, Set.of());
   }

   private void setLabels(String fileName, Set<DataFileLabel> labels) {
      if (labels.isEmpty()) {
         fileNameToLabels.remove(fileName);
      } else {
         fileNameToLabels.put(fileName, internLabels(labels));
      }
   }

   private Set<DataFileLabel> internLabels(Set<DataFileLabel> labels) {
      Set<DataFileLabel> internedLabels = labelSetInterner.get(labels);
      if (internedLabels == null) {
         internedLabels = Set.copyOf(labels);
         labelSetInterner.put(internedLabels, internedLabels);
      }
      return internedLabels;
   }

   public void addLabel(DataFileLabel label, Collection<SegmentHandle> segmentHandles) {
      for (SegmentHandle segmentHandle : segmentHandles) {
         String fileName = toFileName(segmentHandle);
         Set<DataFileLabel> existingLabels = fileNameToLabels.getOrDefault(fileName, Set.of());
         Set<DataFileLabel> newLabels = new HashSet<>(existingLabels);
         newLabels.add(label);
         setLabels(fileName, newLabels);
      }
   }

   public void removeLabel(DataFileLabel label, Collection<SegmentHandle> segmentHandles) {
      for (SegmentHandle segmentHandle : segmentHandles) {
         String fileName = toFileName(segmentHandle);
         Set<DataFileLabel> existingLabels = fileNameToLabels.get(fileName);
         if (existingLabels != null && existingLabels.contains(label)) {
            Set<DataFileLabel> newLabels = new HashSet<>(existingLabels);
            newLabels.remove(label);
            setLabels(fileName, newLabels);
         }
      }
   }

   public void removeAllLabels(Collection<SegmentHandle> segmentHandles) {
      for (SegmentHandle segmentHandle : segmentHandles) {
         String fileName = toFileName(segmentHandle);
         fileNameToLabels.remove(fileName);
      }
   }

   public void replaceLabel(DataFileLabel oldLabel, @Nullable DataFileLabel newLabel) {
      allLabels.remove(oldLabel);
      if (newLabel != null) {
         allLabels.add(newLabel);
      }
      for (Map.Entry<String, Set<DataFileLabel>> entry : fileNameToLabels.entrySet()) {
         if (entry.getValue().contains(oldLabel)) {
            Set<DataFileLabel> labels = new HashSet<>(entry.getValue());
            labels.remove(oldLabel);
            if (newLabel != null) {
               labels.add(newLabel);
            }
            entry.setValue(internLabels(labels));
         }
      }
   }
}
