package no.imr.korona.data.formats.ek60;

import com.google.common.collect.ImmutableMap;
import no.imr.korona.computation.broadband.EK80Parameters;
import no.imr.korona.computation.broadband.PulseCompressionFilterChain;
import no.imr.korona.data.datagrams.Fil0Datagram;
import no.imr.korona.data.datagrams.Fil1Datagram;
import no.imr.korona.data.datagrams.Xml0Datagram;
import no.imr.korona.data.formats.ek60.calibration.BroadbandFunction;
import no.imr.korona.data.formats.ek60.calibration.CalibrationContent;
import no.imr.korona.data.formats.ek60.calibration.CalibrationEntry;
import no.imr.korona.data.formats.ek60.calibration.CalibrationFile;
import no.imr.korona.data.formats.ek60.calibration.CalibrationType;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibration;
import no.imr.korona.data.formats.ek60.calibration.ChannelCalibrationBuilder;
import no.imr.korona.data.ping.items.PingItem;
import no.imr.korona.data.ping.items.configuration.BeamType;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.ping.items.configuration.TransceiverType;
import no.imr.korona.data.ping.items.configuration.TransducerNameAndSerialNumber;
import no.imr.tools.Utils;
import no.imr.tools.Version;
import no.imr.tools.logging.Log;
import no.imr.tools.math.linalg.Vec3;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.stream.Collectors;

public final class Xml0DatagramFactory {
   /**
    * The current version of the {@code LsssFileFormatVersion} attribute.
    * <p>
    * History:
    * <ul>
    * <li>1: LsssFileFormatVersion not written. Byte angle scaling always 1 and not written</li>
    * <li>2: Byte angle scaling always written to file when not 1</li>
    * </ul>
    */
   private static final String LSSS_FILE_FORMAT_VERSION = "2";

   private Xml0DatagramFactory() {
   }

   static RawFileConfiguration createRawFileConfiguration(long ntDate, Element configuration, Element environment, @Nullable Element initialParameter,
                                                          List<PingItem> configurationItems, Path file) throws XmlParseException {
      RawFileConfiguration rawFileConfiguration = new RawFileConfiguration(ntDate);

      Element header = XmlParse.element(configuration, "Header");
      String fileFormatVersion = XmlParse.stringAttribute(header, "FileFormatVersion", "");
      String lsssFileFormatVersion = header.attributeValue("LsssFileFormatVersion");
      if (lsssFileFormatVersion == null && fileFormatVersion.isEmpty()) {
         // Detect that this file was written by LSSS pri
         lsssFileFormatVersion = "1";
      }
      rawFileConfiguration.setSurveyName(XmlParse.stringAttribute(header, "SurveyName", ""));
      rawFileConfiguration.setTransectName(""); // todo ?
      rawFileConfiguration.setSounderName(XmlParse.stringAttribute(header, "ApplicationName"));
      rawFileConfiguration.setVersion(XmlParse.stringAttribute(header, "Version"));
      rawFileConfiguration.setMultiplexing(XmlParse.shortAttribute(header, "Multiplexing", (short) 0));
      rawFileConfiguration.setTimeBias(XmlParse.intAttribute(header, "TimeBias"));
      float soundSpeed = XmlParse.floatAttribute(environment, "SoundSpeed");
      rawFileConfiguration.setSoundVelocityAverage(soundSpeed);
      rawFileConfiguration.setSoundVelocityTransducer(soundSpeed); // todo ?

      rawFileConfiguration.setMRUOffset(new Vec3(XmlParse.floatAttribute(header, "MRUOffsetX", 0),
            XmlParse.floatAttribute(header, "MRUOffsetY", 0),
            XmlParse.floatAttribute(header, "MRUOffsetZ", 0)));

      rawFileConfiguration.setMRUAlpha(new Vec3(XmlParse.floatAttribute(header, "MRUAlphaX", 0),
            XmlParse.floatAttribute(header, "MRUAlphaY", 0),
            XmlParse.floatAttribute(header, "MRUAlphaZ", 0)));

      rawFileConfiguration.setGPSOffset(new Vec3(XmlParse.floatAttribute(header, "GPSOffsetX", 0),
            XmlParse.floatAttribute(header, "GPSOffsetY", 0),
            XmlParse.floatAttribute(header, "GPSOffsetZ", 0)));

      addTransducersAndCalibration(rawFileConfiguration, configuration, initialParameter, configurationItems, lsssFileFormatVersion, file);

      rawFileConfiguration.setXml0Info(createXml0Info(environment, fileFormatVersion));

      return rawFileConfiguration;
   }

   static RawFileConfiguration.Xml0Info createXml0Info(Element environment, String fileFormatVersion) throws XmlParseException {
      RawFileConfiguration.Xml0Info xml0Info = new RawFileConfiguration.Xml0Info();
      xml0Info.setDepth(XmlParse.floatAttribute(environment, "Depth"));

      float salinity = XmlParse.floatAttribute(environment, "Salinity");
      if (salinity < 1 && !fileFormatVersion.isEmpty() && new Version(fileFormatVersion).isOlderThan(new Version("1.01"))) {
         // Salinity in PSU D20151105-T104028.raw with FileFormatVersion="1.01"
         salinity *= 1000;
      }
      xml0Info.setSalinity(salinity);

      float acidity;
      Attribute acidityAttribute = environment.attribute("Acidity");
      if (acidityAttribute != null) {
         acidity = XmlParse.parseFloat(acidityAttribute);
      } else {
         // No acidity attribute in XML0/Environment.
         // This is the case for raw-files written by KORONA < 3.1.0.
         if (salinity < 10) {
            acidity = 7; // Freshwater.
         } else {
            acidity = 8; // Seawater.
         }
      }
      xml0Info.setAcidity(acidity);

      xml0Info.setTemperature(XmlParse.floatAttribute(environment, "Temperature"));
      xml0Info.setDropKeelOffset(XmlParse.floatAttribute(environment, "DropKeelOffset", 0));
      xml0Info.setWaterLevelDraft(XmlParse.floatAttribute(environment, "WaterLevelDraft", 0));
      return xml0Info;
   }

   public static Xml0Datagram toXml0Environment(RawFileConfiguration rawFileConfiguration) {
      RawFileConfiguration.Xml0Info xml0Info = rawFileConfiguration.getXml0Info();
      Element environment = DocumentHelper.createElement("Environment");
      if (xml0Info != null) {
         environment
               .addAttribute("Depth", Utils.toString(xml0Info.getDepth()))
               .addAttribute("Acidity", Utils.toString(xml0Info.getAcidity()))
               .addAttribute("Salinity", Utils.toString(xml0Info.getSalinity()));
      }
      environment.addAttribute("SoundSpeed", Utils.toString(rawFileConfiguration.getSoundVelocityAverage()));
      if (xml0Info != null) {
         environment
               .addAttribute("Temperature", Utils.toString(xml0Info.getTemperature()))
               .addAttribute("DropKeelOffset", Utils.toString(xml0Info.getDropKeelOffset()))
               .addAttribute("WaterLevelDraft", Utils.toString(xml0Info.getWaterLevelDraft()));
      }
      environment.addElement("Transducer");
      return new Xml0Datagram(rawFileConfiguration.getNTDate(), DocumentHelper.createDocument(environment));
   }

   public static Xml0Datagram toXml0Configuration(RawFileConfiguration rawFileConfiguration) {
      Element configuration = DocumentHelper.createElement("Configuration");
      configuration.addElement("Header")
            .addAttribute("ApplicationName", rawFileConfiguration.getSounderName())
            .addAttribute("Version", rawFileConfiguration.getVersion())
            .addAttribute("LsssFileFormatVersion", LSSS_FILE_FORMAT_VERSION)
            .addAttribute("TimeBias", Integer.toString(rawFileConfiguration.getTimeBias()));
      Element transceiver = configuration.addElement("Transceivers");
      rawFileConfiguration.getTransducers().stream()
            .map(Xml0DatagramFactory::transducerToXml)
            .forEach(transceiver::add);
      Element transducers = configuration.addElement("Transducers");
      rawFileConfiguration.getTransducers().stream()
            .map(Xml0DatagramFactory::transducerToOtherXml)
            .forEach(transducers::add);
      return new Xml0Datagram(rawFileConfiguration.getNTDate(), DocumentHelper.createDocument(configuration));
   }

   public static @Nullable Xml0Datagram toXml0InitialParameter(RawFileConfiguration rawFileConfiguration) {
      Element initialParameter = DocumentHelper.createElement("InitialParameter");
      Element channels = initialParameter.addElement("Channels");
      rawFileConfiguration.getTransducers().stream()
            .map(Xml0DatagramFactory::transducerToInitialParameterChannel)
            .filter(Objects::nonNull)
            .forEach(channels::add);
      if (channels.elements().isEmpty()) {
         return null;
      }
      return new Xml0Datagram(rawFileConfiguration.getNTDate(), DocumentHelper.createDocument(initialParameter));
   }

   private static void addTransducersAndCalibration(RawFileConfiguration rawFileConfiguration, Element configuration,
                                                    @Nullable Element initialParameter, List<PingItem> configurationItems,
                                                    @Nullable String lsssFileFormatVersion, Path file) throws XmlParseException {
      Map<TransducerNameAndSerialNumber, Element> serialNumberToOtherTransducer = configuration.elements("Transducers").stream()
            .flatMap(element -> element.elements("Transducer").stream())
            .collect(Collectors.toMap(
                  element -> getNameAndSerialNumber(element, "TransducerSerialNumber"),
                  Function.identity(),
                  (elementA, elementB) -> {
                     if (!otherTransducersAreEquivalent(elementA, elementB)) {
                        Log.global.warning("Duplicated Configuration/Transducers/Transducer name and serial number with conflicting content in " + file);
                     }
                     return elementA;
                  }));

      Map<String, Element> channelIdToInitialParameterChannel = new HashMap<>();
      Element initialParametersChannels = initialParameter != null ? initialParameter.element("Channels") : null;
      if (initialParametersChannels != null) {
         for (Element e : initialParametersChannels.elements("Channel")) {
            String channelId = e.attributeValue("ChannelID");
            if (channelId != null) {
               channelIdToInitialParameterChannel.put(channelId, e);
            }
         }
      }

      ImmutableMap.Builder<String, ChannelCalibration> channelCalibrationById = ImmutableMap.builder();

      Function<String, PulseCompressionFilterChain> channelIdToFilterChain = PulseCompressionFilterChain.makeChannelIdToFilterChain(configurationItems);

      Set<String> ignoredChannelIds = new HashSet<>();
      Element transceivers = XmlParse.element(configuration, "Transceivers");
      for (Element transceiver : transceivers.elements()) {
         for (Element channels : transceiver.elements("Channels")) {
            for (Element channel : channels.elements()) {
               String channelId = XmlParse.attribute(channel, "ChannelID", "ChannelIdLong").getValue();
               for (Element transducer : channel.elements("Transducer")) {
                  int beamType = XmlParse.intAttribute(transducer, "BeamType");
                  if (beamType == BeamType.SPLIT_4_B || beamType == BeamType.ADCP_SINGLE) {
                     rawFileConfiguration.getNotices().add("Ignoring channel " + channelId + " with beam type " + beamType);
                     ignoredChannelIds.add(channelId);
                     continue;
                  }
                  TransducerNameAndSerialNumber nameAndSerialNumber = getNameAndSerialNumber(transducer, "SerialNumber");
                  Element otherTransducer = serialNumberToOtherTransducer.get(nameAndSerialNumber);
                  Element initialParameterChannel = channelIdToInitialParameterChannel.get(channelId);
                  RawFileTransducer rawFileTransducer;
                  try {
                     rawFileTransducer = xmlToTransducer(transceiver, channel, transducer, otherTransducer,
                           initialParameterChannel, lsssFileFormatVersion);
                  } catch (XmlParseException e) {
                     Log.global.log(Level.WARNING, "Error parsing channel " + channelId + " in " + file, e);
                     ignoredChannelIds.add(channelId);
                     continue;
                  }
                  rawFileTransducer.setPulseCompressionFilterChain(channelIdToFilterChain.apply(channelId));
                  rawFileConfiguration.getTransducers().add(rawFileTransducer);
                  ChannelCalibration channelCalibration = createChannelCalibration(transducer, null);
                  channelCalibrationById.put(rawFileTransducer.getChannelId(), channelCalibration);
               }
            }
         }
      }
      rawFileConfiguration.setIgnoredChannelIds(Set.copyOf(ignoredChannelIds));

      configurationItems.removeIf(item -> item instanceof Fil0Datagram || item instanceof Fil1Datagram);

      CalibrationType defaultCalibrationType = new CalibrationType();
      defaultCalibrationType.putEntry(Long.MIN_VALUE, Long.MAX_VALUE, new CalibrationEntry(ChannelCalibration.EMPTY, channelCalibrationById.build(), ImmutableMap.of(), ImmutableMap.of()));
      CalibrationContent calibrationContent = new CalibrationContent(defaultCalibrationType, ImmutableMap.of());
      rawFileConfiguration.setCalibrationFile(new CalibrationFile(null, 0, null, 0, calibrationContent));
   }

   private static TransducerNameAndSerialNumber getNameAndSerialNumber(Element element, String serialNumberAttribute) {
      String name = XmlParse.stringAttribute(element, "TransducerName", "");
      String serialNumber = XmlParse.stringAttribute(element, serialNumberAttribute, "");
      return new TransducerNameAndSerialNumber(name, serialNumber);
   }

   private static RawFileTransducer xmlToTransducer(Element transceiver, Element channel, Element transducer,
                                                    @Nullable Element otherTransducer, @Nullable Element initialParameterChannel,
                                                    @Nullable String lsssFileFormatVersion) throws XmlParseException {
      RawFileTransducer rawFileTransducer = new RawFileTransducer();
      rawFileTransducer.setChannelId(XmlParse.attribute(channel, "ChannelID", "ChannelIdLong").getValue());
      rawFileTransducer.setBeamType(XmlParse.intAttribute(transducer, "BeamType"));
      rawFileTransducer.setFrequency(XmlParse.floatAttribute(transducer, "Frequency"));
      rawFileTransducer.setGainAndGainTable(Float.NaN);
      rawFileTransducer.setEquivalentBeamAngle(XmlParse.floatAttribute(transducer, "EquivalentBeamAngle"));
      rawFileTransducer.setBeamWidthAlongship(XmlParse.floatAttribute(transducer, "BeamWidthAlongship"));
      rawFileTransducer.setBeamWidthAthwartship(XmlParse.floatAttribute(transducer, "BeamWidthAthwartship"));
      boolean splitBeam = BeamType.isSplit(rawFileTransducer.getBeamType());
      rawFileTransducer.setAngleSensitivityAlongship(parseAngleAttribute(splitBeam, transducer, "AngleSensitivityAlongship"));
      rawFileTransducer.setAngleSensitivityAthwartship(parseAngleAttribute(splitBeam, transducer, "AngleSensitivityAthwartship"));
      rawFileTransducer.setAngleOffsetAlongship(parseAngleAttribute(splitBeam, transducer, "AngleOffsetAlongship"));
      rawFileTransducer.setAngleOffsetAthwartship(parseAngleAttribute(splitBeam, transducer, "AngleOffsetAthwartship"));
      rawFileTransducer.setPos(Vec3.ZERO); // todo ?
      rawFileTransducer.setDir(Vec3.ZERO); // todo ?
      parse(rawFileTransducer.getPulseDurationTable(), XmlParse.attribute(channel, "PulseDuration", "PulseLength"));
      parse(rawFileTransducer.getGainTable(), XmlParse.attribute(transducer, "Gain"));
      parse(rawFileTransducer.getSaCorrectionTable(), XmlParse.attribute(transducer, "SaCorrection"));
      rawFileTransducer.setFilterSlope(0.1f); // todo ?
      rawFileTransducer.setDirectivityDrop(0); // todo ?
      rawFileTransducer.setTransceiverVersion(""); // todo ?

      RawFileTransducer.Xml0Info xml0Info = new RawFileTransducer.Xml0Info();
      rawFileTransducer.setXml0Info(xml0Info);
      xml0Info.setChannelIdShort(XmlParse.stringAttribute(channel, "ChannelIdShort", ""));
      xml0Info.setName(XmlParse.stringAttribute(transducer, "TransducerName", ""));
      xml0Info.setTransducerSerialNumber(XmlParse.stringAttribute(transducer, "SerialNumber", ""));
      xml0Info.setTransceiverSerialNumber(XmlParse.stringAttribute(transceiver, "SerialNumber", ""));
      xml0Info.setTransceiverSoftwareVersion(XmlParse.stringAttribute(transceiver, "TransceiverSoftwareVersion", ""));
      xml0Info.setTransceiverType(XmlParse.stringAttribute(transceiver, "TransceiverType", ""));
      xml0Info.setImpedance(XmlParse.floatAttribute(transceiver, "Impedance", (float) EK80Parameters.rwbtrx));
      xml0Info.setSamplingFrequency(XmlParse.floatAttribute(transceiver, "RxSampleFrequency", EK80Parameters.fs));
      if (otherTransducer != null) {
         parseOtherTransducer(otherTransducer, rawFileTransducer, xml0Info);
      }
      if (initialParameterChannel != null) {
         parseInitialParameterChannel(initialParameterChannel, xml0Info);
      }

      if (TransceiverType.isWBT(xml0Info.getTransceiverType()) && BeamType.has3Or3Plus1Sectors(rawFileTransducer.getBeamType())) {
         // Since EK80 v1.12, angles stored as power/angle must be multiplied with certain factors
         // for WBT transducers with 3(+1) sectors.
         // These factors are not explicitly specified in the configuration datagram written by EK80.
         // See email from Sverre Berg (Simrad), 17.08.2018 11.00.
         if (lsssFileFormatVersion == null) {
            xml0Info.setByteAngleScalingAthwartship(2);
            xml0Info.setByteAngleScalingAlongship((float) (2 / Math.sqrt(3)));
         } else {
            xml0Info.setByteAngleScalingAthwartship(XmlParse.floatAttribute(transducer, "LsssByteAngleScalingAthwartship", 1));
            xml0Info.setByteAngleScalingAlongship(XmlParse.floatAttribute(transducer, "LsssByteAngleScalingAlongship", 1));
         }
      }

      return rawFileTransducer;
   }

   private static float parseAngleAttribute(boolean splitBeam, Element element, String name) throws XmlParseException {
      return splitBeam
            ? XmlParse.floatAttribute(element, name)
            : XmlParse.floatAttribute(element, name, Float.NaN);
   }

   private static void parseOtherTransducer(Element otherTransducer, RawFileTransducer rawFileTransducer, RawFileTransducer.Xml0Info xml0Info) throws XmlParseException {
      xml0Info.setTransducerMounting(XmlParse.stringAttribute(otherTransducer, "TransducerMounting", ""));
      rawFileTransducer.setPos(new Vec3(XmlParse.floatAttribute(otherTransducer, "TransducerOffsetX", 0),
            XmlParse.floatAttribute(otherTransducer, "TransducerOffsetY", 0),
            XmlParse.floatAttribute(otherTransducer, "TransducerOffsetZ", 0)));
   }

   private static boolean otherTransducersAreEquivalent(Element otherTransducer1, Element otherTransducer2) {
      String[] attributes = {"TransducerMounting", "TransducerOffsetX", "TransducerOffsetY", "TransducerOffsetZ"};
      return Arrays.stream(attributes).allMatch(attribute -> {
         return Objects.equals(otherTransducer1.attributeValue(attribute), otherTransducer2.attributeValue(attribute));
      });
   }

   private static void parseInitialParameterChannel(Element initialParameterChannel, RawFileTransducer.Xml0Info xml0Info) throws XmlParseException {
      RawFileTransducer.InitialParameters p = new RawFileTransducer.InitialParameters(
            XmlParse.stringAttribute(initialParameterChannel, "PingId"),
            XmlParse.intAttribute(initialParameterChannel, "ChannelMode"),
            XmlParse.intAttribute(initialParameterChannel, "PulseForm")
      );
      xml0Info.setInitialParameters(p);
   }

   private static @Nullable Element transducerToInitialParameterChannel(RawFileTransducer transducer) {
      RawFileTransducer.Xml0Info xml0Info = transducer.getXml0Info();
      if (xml0Info == null) {
         return null;
      }
      RawFileTransducer.InitialParameters initialParameters = xml0Info.getInitialParameters();
      if (initialParameters == null) {
         return null;
      }
      return DocumentHelper.createElement("Channel")
            .addAttribute("PingId", initialParameters.pingId())
            .addAttribute("ChannelMode", String.valueOf(initialParameters.channelMode()))
            .addAttribute("PulseForm", String.valueOf(initialParameters.pulseForm()));
   }

   private static Element transducerToXml(RawFileTransducer rawFileTransducer) {
      Element transceiver = DocumentHelper.createElement("Transceiver");
      RawFileTransducer.Xml0Info xml0Info = rawFileTransducer.getXml0Info();
      if (xml0Info != null) {
         transceiver
               .addAttribute("SerialNumber", xml0Info.getTransceiverSerialNumber())
               .addAttribute("TransceiverSoftwareVersion", xml0Info.getTransceiverSoftwareVersion())
               .addAttribute("TransceiverType", xml0Info.getTransceiverType())
               .addAttribute("Impedance", Utils.toString(xml0Info.getImpedance()))
               .addAttribute("RxSampleFrequency", Utils.toString(xml0Info.getSamplingFrequency()));
      }
      Element channel = transceiver.addElement("Channels").addElement("Channel")
            .addAttribute("ChannelID", rawFileTransducer.getChannelId())
            .addAttribute("ChannelIdShort", xml0Info != null ? xml0Info.getChannelIdShort() : null)
            .addAttribute("PulseDuration", arrayToString(rawFileTransducer.getPulseDurationTable()));
      Element transducer = channel.addElement("Transducer");
      if (xml0Info != null) {
         transducer
               .addAttribute("TransducerName", xml0Info.getName())
               .addAttribute("SerialNumber", xml0Info.getTransducerSerialNumber());
         if (xml0Info.getByteAngleScalingAthwartship() != 1 || xml0Info.getByteAngleScalingAlongship() != 1) {
            transducer
                  .addAttribute("LsssByteAngleScalingAthwartship", Utils.toString(xml0Info.getByteAngleScalingAthwartship()))
                  .addAttribute("LsssByteAngleScalingAlongship", Utils.toString(xml0Info.getByteAngleScalingAlongship()));
         }
      }
      transducer
            .addAttribute("Frequency", Utils.toString(rawFileTransducer.getFrequency()))
            .addAttribute("BeamType", Integer.toString(rawFileTransducer.getBeamType()))
            .addAttribute("EquivalentBeamAngle", Utils.toString(rawFileTransducer.getEquivalentBeamAngle()))
            .addAttribute("Gain", arrayToString(rawFileTransducer.getGainTable()))
            .addAttribute("SaCorrection", arrayToString(rawFileTransducer.getSaCorrectionTable()))
            .addAttribute("BeamWidthAlongship", Utils.toString(rawFileTransducer.getBeamWidthAlongship()))
            .addAttribute("BeamWidthAthwartship", Utils.toString(rawFileTransducer.getBeamWidthAthwartship()))
            .addAttribute("AngleSensitivityAlongship", Utils.toString(rawFileTransducer.getAngleSensitivityAlongship()))
            .addAttribute("AngleSensitivityAthwartship", Utils.toString(rawFileTransducer.getAngleSensitivityAthwartship()))
            .addAttribute("AngleOffsetAlongship", Utils.toString(rawFileTransducer.getAngleOffsetAlongship()))
            .addAttribute("AngleOffsetAthwartship", Utils.toString(rawFileTransducer.getAngleOffsetAthwartship()));
      addChannelCalibration(transducer, rawFileTransducer.getChannelCalibration());
      return transceiver;
   }

   private static Element transducerToOtherXml(RawFileTransducer rawFileTransducer) {
      Element transducer = DocumentHelper.createElement("Transducer");
      RawFileTransducer.Xml0Info xml0Info = rawFileTransducer.getXml0Info();
      if (xml0Info != null) {
         transducer
               .addAttribute("TransducerName", xml0Info.getName())
               .addAttribute("TransducerMounting", xml0Info.getTransducerMounting())
               .addAttribute("TransducerSerialNumber", xml0Info.getTransducerSerialNumber());
      }
      transducer
            .addAttribute("TransducerOffsetX", Utils.toString(rawFileTransducer.getPos().x()))
            .addAttribute("TransducerOffsetY", Utils.toString(rawFileTransducer.getPos().y()))
            .addAttribute("TransducerOffsetZ", Utils.toString(rawFileTransducer.getPos().z()));
      return transducer;
   }

   private static void parse(float[] array, Attribute attribute) throws XmlParseException {
      String attributeValue = attribute.getValue();
      attributeValue = attributeValue.replace(',', '.'); // EK15 uses comma as decimal separator for Gain
      String[] parts = attributeValue.split(";");
      if (parts.length != array.length) {
         throw new XmlParseException(attribute);
      }
      for (int i = 0; i < array.length; i++) {
         try {
            array[i] = Float.parseFloat(parts[i]);
         } catch (NumberFormatException e) {
            throw new XmlParseException(attribute, e);
         }
      }
   }

   private static String arrayToString(float[] array) {
      StringBuilder sb = new StringBuilder()
            .append(Utils.toString(array[0]));
      for (int i = 1; i < array.length; i++) {
         sb.append(';').append(Utils.toString(array[i]));
      }
      return sb.toString();
   }

   public static ChannelCalibration createChannelCalibration(Element element, @Nullable TransducerNameAndSerialNumber nameAndSerialNumber) {
      ChannelCalibrationBuilder builder = new ChannelCalibrationBuilder();

      if (nameAndSerialNumber != null) {
         builder.nameAndSerialNumber = Optional.of(nameAndSerialNumber);
      }

      Element frequencyParCWElement = element.element("FrequencyParCW");
      if (frequencyParCWElement != null) {
         builder.kHz = parseFloat(frequencyParCWElement, "Frequency").map(Utils::hzToKHz);
         builder.gain = parseFloat(frequencyParCWElement, "Gain");
         builder.equivalentBeamAngle = parseFloat(frequencyParCWElement, "EquivalentBeamAngle");
         builder.beamWidthAlongship = parseFloat(frequencyParCWElement, "BeamWidthAlongship");
         builder.beamWidthAthwartship = parseFloat(frequencyParCWElement, "BeamWidthAthwartship");
         builder.angleOffsetAlongship = parseFloat(frequencyParCWElement, "AngleOffsetAlongship");
         builder.angleOffsetAthwartship = parseFloat(frequencyParCWElement, "AngleOffsetAthwartship");

         Optional<Float> pulseDuration = parseFloat(frequencyParCWElement, "PulseLength");
         Optional<Float> saCorrection = parseFloat(frequencyParCWElement, "SaCorrection");
         if (pulseDuration.isPresent() && saCorrection.isPresent()) {
            builder.saCorrections = ImmutableMap.of(pulseDuration.get(), saCorrection.get());
         }
      }

      List<Element> frequencyParElements = element.elements("FrequencyPar");
      if (!frequencyParElements.isEmpty()) {
         frequencyParElements.sort(Comparator.comparingDouble(e -> Double.parseDouble(e.attributeValue("Frequency", "0"))));
         parseDoubles(frequencyParElements, "Frequency").ifPresent(hz -> {
            builder.broadbandGain = parseBroadbandFunction(hz, frequencyParElements, "Gain");
            builder.broadbandTransducerImpedance = parseBroadbandFunction(hz, frequencyParElements, "Impedance");
            builder.broadbandEquivalentBeamAngle = parseBroadbandFunction(hz, frequencyParElements, "EquivalentBeamAngle");
            builder.broadbandBeamWidthAlongship = parseBroadbandFunction(hz, frequencyParElements, "BeamWidthAlongship");
            builder.broadbandBeamWidthAthwartship = parseBroadbandFunction(hz, frequencyParElements, "BeamWidthAthwartship");
            builder.broadbandAngleOffsetAlongship = parseBroadbandFunction(hz, frequencyParElements, "AngleOffsetAlongship");
            builder.broadbandAngleOffsetAthwartship = parseBroadbandFunction(hz, frequencyParElements, "AngleOffsetAthwartship");
         });
      }

      return builder.build();
   }

   private static void addChannelCalibration(Element transducer, ChannelCalibration channelCalibration) {
      Map<String, BroadbandFunction> functions = new LinkedHashMap<>();
      channelCalibration.broadbandGain.ifPresent(function -> functions.put("Gain", function));
      channelCalibration.broadbandTransducerImpedance.ifPresent(function -> functions.put("Impedance", function));
      channelCalibration.broadbandEquivalentBeamAngle.ifPresent(function -> functions.put("EquivalentBeamAngle", function));
      channelCalibration.broadbandBeamWidthAlongship.ifPresent(function -> functions.put("BeamWidthAlongship", function));
      channelCalibration.broadbandBeamWidthAthwartship.ifPresent(function -> functions.put("BeamWidthAthwartship", function));
      channelCalibration.broadbandAngleOffsetAlongship.ifPresent(function -> functions.put("AngleOffsetAlongship", function));
      channelCalibration.broadbandAngleOffsetAthwartship.ifPresent(function -> functions.put("AngleOffsetAthwartship", function));

      functions.values().stream()
            .flatMapToDouble(function -> Arrays.stream(function.getHz()))
            .distinct()
            .sorted()
            .forEach(frequency -> {
               Element frequencyPar = transducer.addElement("FrequencyPar").addAttribute("Frequency", Utils.toString(frequency));
               functions.forEach((name, function) -> frequencyPar.addAttribute(name, Utils.toString(function.getValue(frequency))));
            });
   }

   private static Optional<Float> parseFloat(Element element, String attribute) {
      return Optional.ofNullable(element.attributeValue(attribute))
            .map(Float::parseFloat);
   }

   private static Optional<BroadbandFunction> parseBroadbandFunction(double[] hz, List<Element> elements, String attribute) {
      return parseDoubles(elements, attribute)
            .map(values -> new BroadbandFunction(hz, values));
   }

   private static Optional<double[]> parseDoubles(List<Element> elements, String attribute) {
      double[] values = new double[elements.size()];
      for (int i = 0; i < elements.size(); i++) {
         Element frequencyParameter = elements.get(i);
         String value = frequencyParameter.attributeValue(attribute);
         if (value == null) {
            return Optional.empty();
         }
         values[i] = Double.parseDouble(value);
      }
      return Optional.of(values);
   }
}
