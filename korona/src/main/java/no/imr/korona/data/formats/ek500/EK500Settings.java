package no.imr.korona.data.formats.ek500;

import no.imr.korona.Korona;
import no.imr.korona.data.util.NoticeHandler;
import no.imr.korona.util.KoronaUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Some settings for converting EK500 data to EK60 format.
 */
public final class EK500Settings {
   private static final String EK500_SETTINGS_FILE = "EK500Settings.xml";
   public static final String XML_EK500 = "ek500";

   private static final boolean IGNORE_LAST_PING = true;
   static final int MAX_TIME_RECORD_JUMP_SECONDS = 30;
   static final int NMEA_GENERATION_INTERVAL = 10;
   static final int DATA_THRESHOLD = -10000;

   private final Map<Float, EK500TransducerSettings> frequencyToEk500TransducerSettings = new LinkedHashMap<>();

   private EK500Settings() {
   }

   public EK500Settings(Element element) {
      fromXml(element);
   }

   EK500Settings(Collection<EK500TransducerSettings> ek500TransducerSettingsCollection) {
      for (EK500TransducerSettings ek500TransducerSettings : ek500TransducerSettingsCollection) {
         add(ek500TransducerSettings);
      }
   }

   public static EK500Settings createFromReferenceLocation(Path referenceFile) throws IOException {
      Path dir = Files.isDirectory(referenceFile) ? referenceFile : referenceFile.getParent();

      Path file = getSettingsFileInDirectory(dir);
      if (!Files.exists(file)) {
         file = getDefaultEK500SettingsFile();
      }

      EK500Settings ek500Settings = new EK500Settings();
      if (Files.exists(file)) {
         ek500Settings.load(file);
      }
      return ek500Settings;
   }

   public static Path getSettingsFileInDirectory(Path dir) {
      return dir.resolve(EK500_SETTINGS_FILE);
   }

   public static Path getDefaultEK500SettingsFile() {
      return getSettingsFileInDirectory(Korona.getInstallationDataDir());
   }

   public void load(Path file) throws IOException {
      fromXml(XmlUtils.readDocument(file).getRootElement());
   }

   public void save(Path file) throws IOException {
      XmlUtils.writeDocument(toXml(), file);
   }

   public Element toXml() {
      Element element = DocumentHelper.createElement(XML_EK500);
      for (EK500TransducerSettings ek500TransducerSettings : frequencyToEk500TransducerSettings.values()) {
         element.add(ek500TransducerSettings.toXml());
      }
      return element;
   }

   public void fromXml(Element element) {
      frequencyToEk500TransducerSettings.clear();

      for (Element transducerElement : element.elements(EK500TransducerSettings.TRANSDUCER_XML)) {
         EK500TransducerSettings ek500TransducerSettings = new EK500TransducerSettings();
         ek500TransducerSettings.fromXml(transducerElement);
         add(ek500TransducerSettings);
      }
   }

   private void add(EK500TransducerSettings ek500TransducerSettings) {
      frequencyToEk500TransducerSettings.put(ek500TransducerSettings.frequency.getFloatValue() * 1000, ek500TransducerSettings);
   }

   boolean ignoreLastPing() {
      return IGNORE_LAST_PING;
   }

   Map<Float, EK500TransducerSettings> getEK500TransducerSettings() {
      return frequencyToEk500TransducerSettings;
   }

   float roundToConfiguredFrequency(float frequency) {
      EK500TransducerSettings ek500TransducerSetting = getExistingEK500TransducerSetting(frequency);
      return ek500TransducerSetting != null ? ek500TransducerSetting.frequency.getFloatValue() * 1000 : frequency;
   }

   private @Nullable EK500TransducerSettings getExistingEK500TransducerSetting(float frequency) {
      for (Map.Entry<Float, EK500TransducerSettings> entry : frequencyToEk500TransducerSettings.entrySet()) {
         if (Math.abs(frequency - entry.getKey()) < 1e3) {
            return entry.getValue();
         }
      }
      return null;
   }

   EK500TransducerSettings getEK500TransducerSettings(float frequency, NoticeHandler noticeHandler) {
      EK500TransducerSettings ek500TransducerSettings = getExistingEK500TransducerSetting(frequency);
      if (ek500TransducerSettings == null) {
         int kHz = KoronaUtils.hzToKHz(frequency);
         noticeHandler.addNotice("Did not find EK500 settings for " + kHz + " kHz. Using default settings.");
         ek500TransducerSettings = new EK500TransducerSettings();
         ek500TransducerSettings.frequency.setFloatValue(kHz);
      }
      return ek500TransducerSettings;
   }
}
