package no.imr.korona.data.formats.ek60.calibration;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.formats.ek60.Xml0DatagramFactory;
import no.imr.korona.data.ping.items.configuration.TransducerNameAndSerialNumber;
import no.imr.tools.io.FileUtils;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

final class CalibrationLoader {
   private static final LoadingCache<Path, CalibrationFile> DIR_TO_CALIBRATION_FILE = CacheBuilder.newBuilder()
         .expireAfterAccess(Duration.ofMinutes(5))
         .build(CacheLoader.from(dir -> {
            Path file = dir.resolve(CalibrationFile.FILE_NAME);
            try {
               return loadCalibration(file);
            } catch (Exception e) {
               Log.global.log(Level.WARNING, "Error reading calibration file " + file, e);
               return new CalibrationFile(file, FileUtils.lastModifiedOrNull(file), e);
            }
         }));

   private CalibrationLoader() {
   }

   static CalibrationFile forDirectory(Path dir) {
      CalibrationFile calibrationFile = DIR_TO_CALIBRATION_FILE.getIfPresent(dir);
      if (calibrationFile != null) {
         if (!calibrationFile.isModified(false)) {
            return calibrationFile;
         }
         DIR_TO_CALIBRATION_FILE.invalidate(dir);
      }
      return DIR_TO_CALIBRATION_FILE.getUnchecked(dir);
   }

   private static CalibrationFile loadCalibration(Path file) throws IOException {
      Document document = XmlUtils.readDocumentIfExists(file);
      if (document == null) {
         return new CalibrationFile(file);
      }
      Element rootElement = document.getRootElement();

      Path ek80File;
      Instant ek80LastModified;
      CalibrationContent calibrationContent;

      String refEK80 = rootElement.attributeValue(CalibrationXml.REF_EK80);
      if (refEK80 != null) {
         ek80File = file.resolveSibling(Path.of(refEK80));
         ek80LastModified = FileUtils.lastModified(ek80File);
         CalibrationType defaultCalibrationType = loadEK80Calibration(ek80File);
         calibrationContent = new CalibrationContent(defaultCalibrationType, ImmutableMap.of());
      } else {
         ek80File = null;
         ek80LastModified = null;
         calibrationContent = parseCalibrationContent(rootElement);
      }

      return new CalibrationFile(file, FileUtils.lastModified(file), ek80File, ek80LastModified, calibrationContent);
   }

   static CalibrationContent parseCalibrationContent(Element element) {
      CalibrationType defaultCalibrationType = parseCalibrationType(element);
      ImmutableMap.Builder<String, CalibrationType> calibrationTypesBuilder = ImmutableMap.builder();
      for (Element typeElement : element.elements(CalibrationXml.TYPE)) {
         calibrationTypesBuilder.put(typeElement.attributeValue(CalibrationXml.NAME), parseCalibrationType(typeElement));
      }
      return new CalibrationContent(defaultCalibrationType, calibrationTypesBuilder.build());
   }

   private static CalibrationType parseCalibrationType(Element element) {
      CalibrationType calibrationType = new CalibrationType();
      parseCalibrationEntry(calibrationType, element);
      for (Element calibrationElement : element.elements(CalibrationXml.CALIBRATION)) {
         parseCalibrationEntry(calibrationType, calibrationElement);
      }
      return calibrationType;
   }

   private static void parseCalibrationEntry(CalibrationType calibrationType, Element element) {
      Element defaultElement = element.element(CalibrationXml.DEFAULT);
      List<Element> caseElements = element.elements(CalibrationXml.CASE);
      String beginAttribute = element.attributeValue(CalibrationXml.BEGIN);
      String endAttribute = element.attributeValue(CalibrationXml.END);
      if (defaultElement == null && caseElements.isEmpty() && beginAttribute == null && endAttribute == null) {
         return;
      }

      ChannelCalibration defaultChannelCalibration = defaultElement != null
            ? new ChannelCalibration(defaultElement)
            : ChannelCalibration.EMPTY;

      List<ChannelCalibration> channelCalibrations = caseElements.stream()
            .map(ChannelCalibration::new)
            .toList();

      Instant begin = parseOptionalDate(beginAttribute, Instant.MIN);
      Instant end = parseOptionalDate(endAttribute, Instant.MAX);

      calibrationType.putEntry(begin, end, new CalibrationEntry(defaultChannelCalibration, channelCalibrations));
   }

   private static Instant parseOptionalDate(@Nullable String value, Instant defaultValue) {
      if (value == null || value.isBlank()) {
         return defaultValue;
      }
      return Instant.parse(value);
   }

   private static CalibrationType loadEK80Calibration(Path file) throws IOException {
      Element transducerDataElement = XmlUtils.readDocument(file).getRootElement().element("TransducerData");
      CalibrationType calibrationType = new CalibrationType();
      if (transducerDataElement == null) {
         return calibrationType;
      }
      ImmutableMap.Builder<TransducerNameAndSerialNumber, ChannelCalibration> builder = ImmutableMap.builder();
      transducerDataElement.elements("Transducer").forEach(transducerElement -> {
         String transducerName = transducerElement.attributeValue("TransducerName");
         if (transducerName == null) {
            return;
         }
         String serialNumber = transducerElement.attributeValue("TransducerSerialNumber");
         if (serialNumber == null) {
            return;
         }
         TransducerNameAndSerialNumber nameAndSerialNumber = new TransducerNameAndSerialNumber(transducerName, serialNumber);
         ChannelCalibration channelCalibration = Xml0DatagramFactory.createChannelCalibration(transducerElement, nameAndSerialNumber);
         builder.put(nameAndSerialNumber, channelCalibration);
      });
      calibrationType.putEntry(Instant.MIN, Instant.MAX, new CalibrationEntry(ChannelCalibration.EMPTY, ImmutableMap.of(), ImmutableMap.of(), builder.build()));
      return calibrationType;
   }
}
