package no.imr.lsss.database.reports;

import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.ices.IcesCalibration;
import no.imr.lsss.database.ices.IcesDataAcquisition;
import no.imr.lsss.database.ices.IcesDataProcessing;
import no.imr.lsss.database.ices.IcesGroup;
import no.imr.lsss.database.ices.IcesInstrument;
import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.ScatterPK;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.time.DateTimeMillis;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

sealed class PrintUser25 extends BaseMultiFrequencyXmlReport permits PrintUser26 {
   private final ReportEngine.Feedback feedback;
   private String intervalTypeId = "";
   private String intervalUnitId = "";
   private String intervalOriginId = "";
   private boolean hasPrintedMetadata;
   private final Set<String> instrumentIds = new HashSet<>();
   private final Set<String> dataProcessingIds = new HashSet<>();
   private int lastPrintedDate = 0;
   private int lastPrintedTime = 0;

   PrintUser25(ReportEngine reportEngine, ReportEngine.Feedback feedback) {
      this(25, reportEngine, feedback);
   }

   PrintUser25(int type, ReportEngine reportEngine, ReportEngine.Feedback feedback) {
      super(type, reportEngine);
      this.feedback = feedback;
   }

   @Override
   void printHeaderXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException {
      xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
      xmlStreamWriter.writeStartElement("Acoustic");
      // For testing: xmlStreamWriter.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
      // For testing: xmlStreamWriter.writeAttribute("xsi:noNamespaceSchemaLocation", "Acoustic.xsd");
      hasPrintedMetadata = false;
      instrumentIds.clear();
      dataProcessingIds.clear();
   }

   @Override
   void printMetadataXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException {
      hasPrintedMetadata = true;

      checkMissingIcesValues(aPrintData.getSurvey(), aPrintData.getIcesAcousticMetadata());

      // Instrument start
      int n = -1;
      Map<String, String> instrumentTransducerLocationIdToValue = new LinkedHashMap<>();
      Map<String, String> instrumentTransducerBeamTypeIdToValue = new LinkedHashMap<>();
      for (IcesInstrument instrument : aPrintData.getIcesAcousticMetadata().instruments) {
         n++;
         String instrumentID = "ID_F" + instrument.frequency.getStringValue();// + "_" + n;
         instrumentIds.add(instrumentID);
         xmlStreamWriter.writeStartElement("Instrument");
         xmlStreamWriter.writeAttribute("ID", instrumentID);
         writeSimpleElement("Frequency", instrument.frequency.getStringValue());
         String transducerLocationValue = instrument.transducerLocation.getStringValue();
         String transducerLocationId = "AC_TransducerLocation_" + transducerLocationValue;
         instrumentTransducerLocationIdToValue.put(transducerLocationId, transducerLocationValue);
         writeEmptyElementWithAttribute("TransducerLocation", "IDREF", transducerLocationId);
         writeSimpleElement("TransducerManufacturer", instrument.transducerManufacturer.getStringValue());
         writeSimpleElement("TransducerModel", instrument.transducerModel.getStringValue());
         writeSimpleElement("TransducerSerial", instrument.transducerSerial.getStringValue());
         String transducerBeamTypeValue = instrument.transducerBeamType.getStringValue();
         String transducerBeamTypeId = "AC_TransducerBeamType_" + transducerBeamTypeValue;
         instrumentTransducerBeamTypeIdToValue.put(transducerBeamTypeId, transducerBeamTypeValue);
         writeEmptyElementWithAttribute("TransducerBeamType", "IDREF", transducerBeamTypeId);
         writeSimpleElement("TransducerDepth", instrument.transducerDepth.getStringValue());
         writeSimpleElement("TransducerOrientation", instrument.transducerOrientation.getStringValue());
         writeSimpleElement("TransducerPSI", instrument.transducerPSI.getStringValue());
         writeSimpleElement("TransducerBeamAngleMajor", instrument.transducerBeamAngleMajor.getStringValue());
         writeSimpleElement("TransducerBeamAngleMinor", instrument.transducerBeamAngleMinor.getStringValue());
         writeSimpleElement("TransceiverManufacturer", instrument.transceiverManufacturer.getStringValue());
         writeSimpleElement("TransceiverModel", instrument.transceiverModel.getStringValue());
         writeSimpleElement("TransceiverSerial", instrument.transceiverSerial.getStringValue());
         writeSimpleElement("TransceiverFirmware", instrument.transceiverFirmware.getStringValue());
         xmlStreamWriter.writeEndElement(); // Instrument end
      }

      // Calibration start
      n = -1;
      Map<String, String> calibrationAcquisitionMethodIdToValue = new LinkedHashMap<>();
      Map<String, String> calibrationProcessingMethodIdToValue = new LinkedHashMap<>();
      for (IcesCalibration calibration : aPrintData.getIcesAcousticMetadata().calibrations) {
         n++;
         xmlStreamWriter.writeStartElement("Calibration");
         xmlStreamWriter.writeAttribute("ID", "C" + n);                 //todo
         writeSimpleElement("Date", calibration.date.getStringValue());
         String acquisitionMethodValue = calibration.acquisitionMethod.getStringValue();
         String acquisitionMethodId = "AC_AcquisitionMethod_" + acquisitionMethodValue;
         calibrationAcquisitionMethodIdToValue.put(acquisitionMethodId, acquisitionMethodValue);
         writeEmptyElementWithAttribute("AcquisitionMethod", "IDREF", acquisitionMethodId);
         String processingMethodValue = calibration.processingMethod.getStringValue();
         String processingMethodId = "AC_ProcessingMethod_" + processingMethodValue;
         calibrationProcessingMethodIdToValue.put(processingMethodId, processingMethodValue);
         writeEmptyElementWithAttribute("ProcessingMethod", "IDREF", processingMethodId);
         String str = "AC_AccuracyEstimate_" + calibration.accuracyEstimate.getStringValue();
         writeSimpleElement("AccuracyEstimate", str);
         xmlStreamWriter.writeEndElement(); // Calibration end
      }

      // Data acquisition start
      n = -1;
      Map<String, String> dataAcquisitionSoftwareNameIdToValue = new LinkedHashMap<>();
      Map<String, String> dataAcquisitionStoredDataFormatIdToValue = new LinkedHashMap<>();
      for (IcesDataAcquisition dataAcquisition : aPrintData.getIcesAcousticMetadata().dataAcquisitions) {
         n++;
         xmlStreamWriter.writeStartElement("DataAcquisition");
         xmlStreamWriter.writeAttribute("ID", "DA" + n);
         String dataAcquisitionSoftwareNameValue = dataAcquisition.softwareName.getStringValue();
         String dataAcquisitionSoftwareNameId = "AC_DataAcquisitionSoftwareName_" + dataAcquisitionSoftwareNameValue;
         dataAcquisitionSoftwareNameIdToValue.put(dataAcquisitionSoftwareNameId, dataAcquisitionSoftwareNameValue);
         writeEmptyElementWithAttribute("SoftwareName", "IDREF", dataAcquisitionSoftwareNameId);
         writeSimpleElement("SoftwareVersion", dataAcquisition.softwareVersion.getStringValue());
         String storedDataFormatValue = dataAcquisition.storedDataFormat.getStringValue();
         String storedDataFormatId = "AC_StoredDataFormat_" + storedDataFormatValue;
         dataAcquisitionStoredDataFormatIdToValue.put(storedDataFormatId, storedDataFormatValue);
         writeEmptyElementWithAttribute("StoredDataFormat", "IDREF", storedDataFormatId);
         writeSimpleElement("PingDutyCycle", dataAcquisition.pingDutyCycle.getStringValue());
         xmlStreamWriter.writeEndElement(); // DataAcquisition end
      }

      // DataProcessing start
      n = -1;
      Map<String, String> dataProcessingSoftwareNameIdToValue = new LinkedHashMap<>();
      Map<String, String> dataProcessingTriwaveCorrectionIdToValue = new LinkedHashMap<>();
      Map<String, String> dataProcessingOnAxisGainUnitIdToValue = new LinkedHashMap<>();
      for (IcesDataProcessing dataProcessing : aPrintData.getIcesAcousticMetadata().dataProcessings) {
         n++;
         String dataProcessingID = "DP" + dataProcessing.frequency.getStringValue();
         dataProcessingIds.add(dataProcessingID);
         xmlStreamWriter.writeStartElement("DataProcessing");
         xmlStreamWriter.writeAttribute("ID", dataProcessingID);
         String dataProcessingSoftwareNameValue = dataProcessing.softwareName.getStringValue();
         String dataProcessingSoftwareNameId = "AC_DataProcessingSoftwareName_" + dataProcessingSoftwareNameValue;
         dataProcessingSoftwareNameIdToValue.put(dataProcessingSoftwareNameId, dataProcessingSoftwareNameValue);
         writeEmptyElementWithAttribute("SoftwareName", "IDREF", dataProcessingSoftwareNameId);
         writeSimpleElement("SoftwareVersion", dataProcessing.softwareVersion.getStringValue());
         String triwaveCorrectionValue = dataProcessing.triwaveCorrection.getStringValue();
         String triwaveCorrectionId = "AC_TriwaveCorrection_" + triwaveCorrectionValue;
         dataProcessingTriwaveCorrectionIdToValue.put(triwaveCorrectionId, triwaveCorrectionValue);
         writeEmptyElementWithAttribute("TriwaveCorrection", "IDREF", triwaveCorrectionId);
         writeSimpleElement("ChannelID", dataProcessing.channelID.getStringValue());
         writeSimpleElement("Bandwidth", dataProcessing.bandwidth.getStringValue());
         writeSimpleElement("Frequency", dataProcessing.frequency.getStringValue());
         writeSimpleElement("TransceiverPower", dataProcessing.transceiverPower.getStringValue());
         writeSimpleElement("TransmitPulseLength", dataProcessing.transmitPulseLength.getStringValue());
         writeSimpleElement("OnAxisGain", dataProcessing.onAxisGain.getStringValue());
         String onAxisGainUnitValue = dataProcessing.onAxisGainUnit.getStringValue();
         String onAxisGainUnitId = "AC_OnAxisGainUnit_" + onAxisGainUnitValue;
         dataProcessingOnAxisGainUnitIdToValue.put(onAxisGainUnitId, onAxisGainUnitValue);
         writeEmptyElementWithAttribute("OnAxisGainUnit", "IDREF", onAxisGainUnitId);
         writeSimpleElement("SaCorrection", dataProcessing.saCorrection.getStringValue());
         writeSimpleElement("Absorption", dataProcessing.absorption.getStringValue());
         writeSimpleElement("AbsorptionDescription", dataProcessing.absorptionDescription.getStringValue());
         writeSimpleElement("SoundSpeed", dataProcessing.soundSpeed.getStringValue());
         writeSimpleElement("SoundSpeedDescription", dataProcessing.soundSpeedDescription.getStringValue());
         writeSimpleElement("TransducerPSI", dataProcessing.transducerPSI.getStringValue());
         xmlStreamWriter.writeEndElement(); // DataProcessing end
      }

      // Vocabulary
      xmlStreamWriter.writeStartElement("Vocabulary");

      String surveyCode = aPrintData.getIcesAcousticMetadata().survey.getStringValue();
      if (surveyCode.isEmpty()) {
         surveyCode = aPrintData.getSurvey().getSurveyTitle();
      }
      Map<String, String> surveyIdToValue = new LinkedHashMap<>();
      surveyIdToValue.put("AC_Survey_" + surveyCode, surveyCode);
      writeVocabularyCodes(xmlStreamWriter, "Survey", "AC_Survey.xml", surveyIdToValue);

      String nationCode = GetIces.nation(aPrintData.getNation());
      writeVocabularyCode(xmlStreamWriter, "Country", "ISO_3166.xml", "ISO_3166_" + nationCode, nationCode);

      String platformCode = aPrintData.getIcesAcousticMetadata().platform.getStringValue();
      if (platformCode.isEmpty()) {
         platformCode = aPrintData.getPlatformIocCode();
      }
      writeVocabularyCode(xmlStreamWriter, "Platform", "SHIPC.xml", "SHIPC_" + platformCode, platformCode);

      String icesOrganisation = aPrintData.getIcesAcousticMetadata().organisation.getStringValue();
      writeVocabularyCode(xmlStreamWriter, "Organisation", "EDMO.xml", "EDMO_" + icesOrganisation, icesOrganisation);

      String intervalTypeValue;
      String intervalUnitValue;
      String intervalOriginValue = "start";
      String logValidityValue;
      if (aPrintData.getOneDistanceInterval() <= 0.00001f) { // Very short distance: probably no log-distance: guess units of time
         intervalTypeValue = "time";
         intervalUnitValue = "sec";
         logValidityValue = "I"; // Invalid log
      } else {
         intervalTypeValue = "distance";  // Default
         intervalUnitValue = "nmi";       // Default
         logValidityValue = "V";             // Default: valid log
      }
      intervalTypeId = "AC_PingAxisIntervalType_" + intervalTypeValue;
      intervalUnitId = "AC_PingAxisIntervalUnit_" + intervalUnitValue;
      intervalOriginId = "AC_PingAxisIntervalOrigin_" + intervalOriginValue;
      String logValidityId = "AC_LogValidity_" + logValidityValue;
      writeVocabularyCode(xmlStreamWriter, "PingAxisIntervalType", "AC_PingAxisIntervalType.xml", intervalTypeId, intervalTypeValue); // LSSS: distance/ping/time
      writeVocabularyCode(xmlStreamWriter, "PingAxisIntervalUnit", "AC_PingAxisIntervalUnit.xml", intervalUnitId, intervalUnitValue); // LSSS: nmi/ping/sec
      writeVocabularyCode(xmlStreamWriter, "PingAxisIntervalOrigin", "AC_PingAxisIntervalOrigin.xml", intervalOriginId, intervalOriginValue); // end/middle/start

      Map<String, String> logOriginIdToValue = new LinkedHashMap<>();
      logOriginIdToValue.put("AC_LogOrigin_start", "start");
      logOriginIdToValue.put("AC_LogOrigin_end", "end");
      writeVocabularyCodes(xmlStreamWriter, "Origin", "AC_LogOrigin.xml", logOriginIdToValue);
      writeVocabularyCode(xmlStreamWriter, "LogValidity", "AC_LogValidity.xml", logValidityId, logValidityValue); // "V" if log is valid ("I" invalid)

      // Species used in report
      int printCount = aPrintData.getPrintCount();
      Map<String, String> saCategoryIdToValue = new LinkedHashMap<>();
      for (int i = 0; i < printCount; i++) {
         AcousticCategory acousticCategory = aPrintData.getAcousticCategory(i);
         String category = getCategory(acousticCategory);
         if (getType() == 25 && category.equals(GetIces.UNKNOWN_ACOUSTIC_CATEGORY)) {
            feedback.addMissingIcesCategory(acousticCategory);
         }
         saCategoryIdToValue.put("AC_SaCategory_" + category, category);
      }
      writeVocabularyCodes(xmlStreamWriter, "SaCategory", "AC_SaCategory.xml", saCategoryIdToValue);

      // Acoustic metadata
      writeVocabularyCode(xmlStreamWriter, "Type", "AC_AcousticDataType.xml", "AC_AcousticDataType_C", "C");  // Always: C=sA
      writeVocabularyCode(xmlStreamWriter, "Unit", "AC_DataUnit.xml", "AC_DataUnit_m2nmi-2", "m2nmi-2");      // Always m2nm-2 for sA
      writeVocabularyCodes(xmlStreamWriter, "TransducerLocation", "AC_TransducerLocation.xml", instrumentTransducerLocationIdToValue);
      writeVocabularyCodes(xmlStreamWriter, "TransducerBeamType", "AC_TransducerBeamType.xml", instrumentTransducerBeamTypeIdToValue);

      // Acoustic metadata: calibration
      writeVocabularyCodes(xmlStreamWriter, "AcquisitionMethod", "AC_AcquisitionMethod.xml", calibrationAcquisitionMethodIdToValue);
      writeVocabularyCodes(xmlStreamWriter, "ProcessingMethod", "AC_ProcessingMethod.xml", calibrationProcessingMethodIdToValue);

      // Acoustic metadata: acquisition
      writeVocabularyCodes(xmlStreamWriter, "DataAcquisitionSoftwareName", "AC_DataAcquisitionSoftwareName.xml", dataAcquisitionSoftwareNameIdToValue);
      writeVocabularyCodes(xmlStreamWriter, "StoredDataFormat", "AC_StoredDataFormat.xml", dataAcquisitionStoredDataFormatIdToValue);

      // Acoustic metadata: processing
      writeVocabularyCodes(xmlStreamWriter, "DataProcessingSoftwareName", "AC_DataProcessingSoftwareName.xml", dataProcessingSoftwareNameIdToValue);
      writeVocabularyCodes(xmlStreamWriter, "TriwaveCorrection", "AC_TriwaveCorrection.xml", dataProcessingTriwaveCorrectionIdToValue);
      writeVocabularyCodes(xmlStreamWriter, "OnAxisGainUnit", "AC_OnAxisGainUnit.xml", dataProcessingOnAxisGainUnitIdToValue);

      xmlStreamWriter.writeEndElement(); // Vocabulary
      xmlStreamWriter.writeStartElement("Cruise");

      xmlStreamWriter.writeStartElement("Survey");
      for (String surveyId : surveyIdToValue.keySet()) {
         writeEmptyElementWithAttribute("Code", "IDREF", surveyId);
      }
      xmlStreamWriter.writeEndElement();
      writeEmptyElementWithAttribute("Country", "IDREF", "ISO_3166_" + nationCode);
      writeEmptyElementWithAttribute("Platform", "IDREF", "SHIPC_" + platformCode);
      writeSimpleElement("StartDate", DateTimeMillis.toLocalDate(aPrintData.getSurvey().getStartDate()).map(LocalDate::toString).orElse(""));
      writeSimpleElement("EndDate", DateTimeMillis.toLocalDate(aPrintData.getSurvey().getStopDate()).map(LocalDate::toString).orElse(""));
      writeEmptyElementWithAttribute("Organisation", "IDREF", "EDMO_" + icesOrganisation);

      // Local survey identifier?
      String localID = Utils.format("%d-%d-%d",
            aPrintData.getSurvey().getCompId().getNation(),
            aPrintData.getSurvey().getCompId().getPlatform(),
            aPrintData.getSurvey().getCompId().getSurvey());
      writeSimpleElement("LocalID", localID);
   }

   private void checkMissingIcesValues(Survey survey, IcesAcousticMetadata icesAcousticMetadata) {
      checkMissingIcesValues(survey, icesAcousticMetadata.instruments);
      checkMissingIcesValues(survey, icesAcousticMetadata.calibrations);
      checkMissingIcesValues(survey, icesAcousticMetadata.dataAcquisitions);
      checkMissingIcesValues(survey, icesAcousticMetadata.dataProcessings);
   }

   private void checkMissingIcesValues(Survey survey, List<? extends IcesGroup> groups) {
      if (groups.isEmpty()) {
         feedback.addMissingIcesValue(survey);
      }
      for (IcesGroup group : groups) {
         boolean missingValue = group.getMandatoryParameters().stream()
               .anyMatch(parameter -> parameter.getStringValue().isEmpty());
         if (missingValue) {
            feedback.addMissingIcesValue(survey);
         }
      }
   }

   String getCategory(AcousticCategory acousticCategory) {
      return GetIces.acousticCategory(acousticCategory);
   }

   private static void writeVocabularyCode(XMLStreamWriter xmlStreamWriter, String aElementName, String aCodeTypeFileName, String aID, String aValue) throws XMLStreamException {
      writeVocabularyCodes(xmlStreamWriter, aElementName, aCodeTypeFileName, Map.of(aID, aValue));
   }

   private static void writeVocabularyCodes(XMLStreamWriter xmlStreamWriter, String aElementName, String aCodeTypeFileName, Map<String, String> aIdToValue) throws XMLStreamException {
      xmlStreamWriter.writeStartElement(aElementName);
      for (Map.Entry<String, String> entry : aIdToValue.entrySet()) {
         xmlStreamWriter.writeStartElement("Code");
         xmlStreamWriter.writeAttribute("ID", entry.getKey());
         xmlStreamWriter.writeAttribute("CodeType", "https://acoustic.ices.dk/Services/Schema/XML/" + aCodeTypeFileName);
         xmlStreamWriter.writeCharacters(entry.getValue());
         xmlStreamWriter.writeEndElement(); // Code
      }
      xmlStreamWriter.writeEndElement(); // aName
   }

   // Not very elegant, and may also be wrong
   private static int getInstrumentFrequency(int f) {
      int[] instrumentFrequency = {18, 38, 70, 120, 200, 333, 555, 926};
      int[] low = {16, 36, 55, 91, 161, 261, 451, 700};
      int[] high = {20, 40, 90, 160, 260, 450, 650, 1050};
      int instFreq = f;

      for (int i = 0; i < instrumentFrequency.length; i++) {
         if (f == instrumentFrequency[i]) {
            break;
         } else if (f > instrumentFrequency[i] && f < high[i] && f > low[i]) {
            instFreq = instrumentFrequency[i];
            break;
         }
      }

      return instFreq;
   }

   @Override
   void printXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) throws XMLStreamException {
      Observation observation = aPrintData.getObservation(aMode);
      Scatter scatter = aPrintData.getScatter(aMode);
      ScatterPK scatterPK = scatter.getCompId();
      int date = scatterPK.getObservationDate();
      int time = scatterPK.getObservationTime();

      // ***** Log / Distance block (start) ***
      if (date != lastPrintedDate || time != lastPrintedTime) {
         if (lastPrintedDate != 0) {
            xmlStreamWriter.writeEndElement(); // Log
         }
         xmlStreamWriter.writeStartElement("Log");
         writeSimpleElement("Distance", observation.getDistance());
         writeSimpleElement("Time", ReportUtils.DATE_TIME.format(DatabaseTime.toInstant(date, time)));
         writeSimpleElement("Latitude", observation.getLatitude());
         writeSimpleElement("Longitude", observation.getLongitude());

         writeEmptyElementWithAttribute("Origin", "IDREF", "AC_LogOrigin_start");

         //End of element
         Observation observationStop = aPrintData.getObservationStop(aMode);
         if (observationStop != null) {
            writeSimpleElement("Latitude2", observationStop.getLatitude());
            writeSimpleElement("Longitude2", observationStop.getLongitude());
            writeEmptyElementWithAttribute("Origin2", "IDREF", "AC_LogOrigin_end");
         }

         String logValidityId;
         if (scatter.getDistanceInterval() <= 0.00001f) { // Very short distance: probably no log-distance: guess units of time
            logValidityId = "AC_LogValidity_I";
         } else {
            logValidityId = "AC_LogValidity_V"; // Default: valid log
         }
         writeEmptyElementWithAttribute("Validity", "IDREF", logValidityId);

         if (observation.getBottomDepth() > 0) {
            writeSimpleElement("BottomDepth", observation.getBottomDepth());
         }

         lastPrintedDate = date;
         lastPrintedTime = time;
      }  //***** Log / Distance block (stop) ***


      float[][] dataPrint = aPrintData.getSa(aMode);
      float channelThickness = scatter.getChannelThickness();
      for (int j = 1; j <= aPrintData.getMaxChannel(aMode); j++) { // Sum channel (j=0) not included

         Map<String, Float> categoryToSa = new LinkedHashMap<>();
         for (int i = 0; i < aPrintData.getPrintCount(); i++) {
            float sa = dataPrint[i][j];
            if (sa > 0) {
               String category = getCategory(aPrintData.getAcousticCategory(i));
               categoryToSa.merge(category, sa, Float::sum);
            }
         }
         if (categoryToSa.isEmpty()) {
            continue;
         }

         xmlStreamWriter.writeStartElement("Sample");

         writeSimpleElement("ChannelDepthUpper", (j - 1) * channelThickness);
         writeSimpleElement("ChannelDepthLower", j * channelThickness);
         writeSimpleElement("PingAxisInterval", scatter.getDistanceInterval());
         writeEmptyElementWithAttribute("PingAxisIntervalType", "IDREF", intervalTypeId);
         writeEmptyElementWithAttribute("PingAxisIntervalUnit", "IDREF", intervalUnitId);
         writeSimpleElement("SvThreshold", scatter.getThreshold());
         int f = (int) Math.round(scatter.getCompId().getFrequency() / 1000.0);
         int ff = getInstrumentFrequency(f);
         String instrumentId = "ID_F" + ff;
         if (!instrumentIds.contains(instrumentId)) {
            feedback.addMissingIcesValue(aPrintData.getSurvey());
         }
         writeEmptyElementWithAttribute("Instrument", "IDREF", instrumentId);

         // One only?
         writeEmptyElementWithAttribute("Calibration", "IDREF", "C0"); //todo
         writeEmptyElementWithAttribute("DataAcquisition", "IDREF", "DA0"); //todo

         // At least as many as number of transducers, but may be more if channels are split
         String dataProcessingId = "DP" + f;
         if (!dataProcessingIds.contains(dataProcessingId)) {
            feedback.addMissingIcesValue(aPrintData.getSurvey());
         }
         writeEmptyElementWithAttribute("DataProcessing", "IDREF", dataProcessingId);

         writeEmptyElementWithAttribute("PingAxisIntervalOrigin", "IDREF", intervalOriginId);

         for (Map.Entry<String, Float> entry : categoryToSa.entrySet()) {
            String category = entry.getKey();
            float sa = entry.getValue();
            xmlStreamWriter.writeStartElement("Data");
            writeEmptyElementWithAttribute("SaCategory", "IDREF", "AC_SaCategory_" + category);
            writeEmptyElementWithAttribute("Type", "IDREF", "AC_AcousticDataType_C");
            writeEmptyElementWithAttribute("Unit", "IDREF", "AC_DataUnit_m2nmi-2");
            writeSimpleElement("Value", sa);
            xmlStreamWriter.writeEndElement(); // Data
         }
         xmlStreamWriter.writeEndElement(); // Sample
      }

      if (date != lastPrintedDate || time != lastPrintedTime) {
         xmlStreamWriter.writeEndElement(); // Log
         lastPrintedDate = date;
         lastPrintedTime = time;
      }
   }

   @Override
   void finaliseReportXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException {
      // Reset static variables LastPrintedDate and LastPrintedDate in case report generator is called again without stopping LSSS
      lastPrintedDate = 0;
      lastPrintedTime = 0;
      if (hasPrintedMetadata) {
         xmlStreamWriter.writeEndElement(); // Log
         xmlStreamWriter.writeEndElement(); // Cruise
      }
      xmlStreamWriter.writeEndElement(); // Acoustic
      xmlStreamWriter.writeEndDocument();
   }
}
