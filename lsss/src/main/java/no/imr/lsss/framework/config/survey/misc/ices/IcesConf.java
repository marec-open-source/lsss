package no.imr.lsss.framework.config.survey.misc.ices;

import no.imr.korona.data.datamanager.DataFile;
import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.formats.ek60.EK60DataFormatPlugin;
import no.imr.korona.data.ping.items.channel.ChannelData;
import no.imr.korona.data.ping.items.configuration.BeamType;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.ping.items.configuration.TransducerMounting;
import no.imr.lsss.LSSS;
import no.imr.lsss.database.ices.IcesAcousticMetadata;
import no.imr.lsss.database.ices.IcesCalibration;
import no.imr.lsss.database.ices.IcesDataAcquisition;
import no.imr.lsss.database.ices.IcesDataProcessing;
import no.imr.lsss.database.ices.IcesInstrument;
import no.imr.lsss.database.ices.IdRefParameter;
import no.imr.lsss.database.reports.GetIocCode;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.lsss.framework.BaseSystemFeaturePlugin;
import no.imr.lsss.framework.config.ConfigurationUnit;
import no.imr.lsss.framework.config.survey.data.DataSetManager;
import no.imr.lsss.framework.config.survey.data.DataType;
import no.imr.tools.Utils;
import no.imr.tools.database.DatabaseConnection;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.ViewHolder;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class IcesConf extends ConfigurationUnit {
   private final ViewHolder<IcesConfView> viewHolder = new ViewHolder<>(() -> new IcesConfView(this));
   private final IcesAcousticMetadata icesAcousticMetadata = new IcesAcousticMetadata();

   public IcesConf(BaseSystemFeaturePlugin plugin) {
      super(plugin, new Name("SurveyIcesConf", "ICES acoustic metadata"),
            "ICES acoustic metadata used when generating database reports");
   }

   IcesAcousticMetadata getIcesAcousticMetadata() {
      return icesAcousticMetadata;
   }

   @Override
   public void addToConfigurationXml(Element configurationElement) {
      configurationElement.add(icesAcousticMetadata.toXml());
   }

   @Override
   public void fromConfigurationXml(Element configurationElement) {
      Element element = configurationElement.element(icesAcousticMetadata.getName().persistentName());
      if (element != null) {
         icesAcousticMetadata.fromXml(element);
      }
   }

   @Override
   public JComponent getComponent() {
      if (viewHolder.hasView()) {
         viewHolder.getView().updateContent();
      }
      return viewHolder.getComponent();
   }

   @Override
   public boolean stopEditing() {
      return !viewHolder.hasView() || viewHolder.getView().stopEditing();
   }

   @Override
   public void removeView() {
      viewHolder.removeView();
   }

   @Override
   public boolean prepareApply() {
      return stopEditing();
   }

   @Override
   public boolean apply() {
      saveToDatabase();
      return true;
   }

   public void saveToDatabase() {
      DatabaseConnection databaseConnection = getLSSS().getDatabaseManager().getDatabaseConnection();
      if (!databaseConnection.isConnected()) {
         return;
      }
      Survey survey = getConfigurationManager().getSurveyConf().getSurvey();
      if (survey == null) {
         return;
      }
      IcesUtils.acousticMetadataToDatabase(databaseConnection, survey, icesAcousticMetadata);
   }

   void resetValuesFromSurvey() {
      Survey survey = getConfigurationManager().getSurveyConf().getSurvey();
      if (survey != null) {
         String platformIocCode = getLSSS().getDatabaseManager().getDatabaseConnection().executeStatelessValuedQuery(session -> {
            return GetIocCode.getIocCode(session, survey);
         });
         if (readIcesCodes(IcesUtils.PLATFORM_SCHEMA).stream().anyMatch(code -> code.key().equals(platformIocCode))) {
            icesAcousticMetadata.platform.setStringValue(platformIocCode);
         }

         readIcesCodes(IcesUtils.SURVEY_SCHEMA).stream()
               .filter(code -> survey.getSurveyTitle().contains(code.key()))
               .findFirst()
               .ifPresent(code -> {
                  icesAcousticMetadata.survey.setStringValue(code.key());
               });
      }
   }

   public void resetValuesFromData() {
      resetValuesFromSurvey();

      DataSetManager dataSetManager = getConfigurationManager().getDataConf().getDataSetManager();
      DataFileSet currentDataFileSet = dataSetManager.getDataFileSet();
      if (currentDataFileSet.isEmpty()) {
         return;
      }

      DataFileSet originalDataFileSet = dataSetManager.getDataFileSet(DataType.RAW);
      if (originalDataFileSet.isEmpty()) {
         originalDataFileSet = currentDataFileSet;
      }

      DataFile originalDataFile = originalDataFileSet.getDataFiles().getFirst();
      RawFileConfiguration rawFileConfiguration = originalDataFile.getRawFileConfiguration();

      if (icesAcousticMetadata.calibrations.isEmpty()) {
         icesAcousticMetadata.calibrations.add(new IcesCalibration());
      }

      icesAcousticMetadata.dataAcquisitions.clear();
      IcesDataAcquisition dataAcquisition = new IcesDataAcquisition();
      dataAcquisition.softwareName.setValue(rawFileConfiguration.getSounderName());
      dataAcquisition.softwareVersion.setValue(rawFileConfiguration.getVersion());
      if (Utils.endsWithIgnoringCase(originalDataFile.getSegmentHandle().getMainFile().toString(), EK60DataFormatPlugin.RAW_SUFFIX)) {
         dataAcquisition.storedDataFormat.setValue("RAW");
      }
      dataAcquisition.pingDutyCycle.setValue("Varying");
      dataAcquisition.comments.setValue(dataFileToComment(originalDataFile));
      icesAcousticMetadata.dataAcquisitions.add(dataAcquisition);

      resetInstruments(originalDataFile);
      resetDataProcessings(currentDataFileSet.getDataFiles().getFirst());

      viewHolder.ifView(IcesConfView::updateContent);
   }

   private void resetInstruments(DataFile dataFile) {
      String comment = dataFileToComment(dataFile);

      icesAcousticMetadata.instruments.clear();
      List<RawFileTransducer> transducers = dataFile.getRawFileConfiguration().getTransducers();
      for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
         RawFileTransducer transducer = transducers.get(channelIndex);
         RawFileTransducer.Xml0Info xml0Info = transducer.getXml0Info();
         int channel = channelIndex + 1;
         ChannelData channelData = findChannelData(dataFile, channel);

         IcesInstrument instrument = new IcesInstrument();
         icesAcousticMetadata.instruments.add(instrument);
         instrument.frequency.setFloatValue(transducer.getKHz());
         instrument.transducerManufacturer.setValue("Simrad");
         Pattern transducerModelPattern = Pattern.compile(".*\\b(\\w+" + transducer.getKHz() + "\\S*).*");
         Matcher transducerModelMatcher = transducerModelPattern.matcher(transducer.getChannelId());
         if (transducerModelMatcher.matches()) {
            instrument.transducerModel.setValue(transducerModelMatcher.group(1));
         }
         instrument.transducerBeamType.setValue(BeamType.isSplit(transducer.getBeamType()) ? "S2" : "S1");
         instrument.transducerOrientation.setValue(getConfigurationManager().getSurveyMiscConf().seabedMounted.getBooleanValue() ? "upward looking" : "downwards-looking");
         instrument.transducerPSI.setFloatValue(transducer.getEquivalentBeamAngle());
         instrument.transducerBeamAngleMajor.setFloatValue(transducer.getBeamWidthAthwartship());
         instrument.transducerBeamAngleMinor.setFloatValue(transducer.getBeamWidthAlongship());
         instrument.transceiverManufacturer.setValue("Simrad");
         if (xml0Info != null) {
            switch (xml0Info.getTransducerMounting()) {
               case TransducerMounting.DROP_KEEL -> {
                  instrument.transducerLocation.setValue("AB");
               }
               default -> {
               }
            }
            instrument.transducerSerial.setValue(xml0Info.getTransducerSerialNumber());
            instrument.transceiverModel.setValue(xml0Info.getTransceiverType());
            instrument.transceiverSerial.setValue(xml0Info.getTransceiverSerialNumber());
            instrument.transceiverFirmware.setValue(xml0Info.getTransceiverSoftwareVersion());
         }
         if (channelData != null) {
            instrument.transducerDepth.setFloatValue(channelData.getTransducerDepth());
         }
         instrument.comments.setValue(comment);
      }
   }

   private void resetDataProcessings(DataFile dataFile) {
      String comment = dataFileToComment(dataFile);

      icesAcousticMetadata.dataProcessings.clear();
      List<RawFileTransducer> transducers = dataFile.getRawFileConfiguration().getTransducers();
      for (int channelIndex = 0; channelIndex < transducers.size(); channelIndex++) {
         RawFileTransducer transducer = transducers.get(channelIndex);
         int channel = channelIndex + 1;
         ChannelData channelData = findChannelData(dataFile, channel);

         IcesDataProcessing dataProcessing = new IcesDataProcessing();
         icesAcousticMetadata.dataProcessings.add(dataProcessing);
         dataProcessing.softwareName.setValue("LSSS");
         dataProcessing.softwareVersion.setValue(LSSS.VERSION);
         dataProcessing.triwaveCorrection.setValue("NA");
         dataProcessing.channelID.setValue(transducer.getChannelId());
         dataProcessing.frequency.setFloatValue(transducer.getKHz());
         dataProcessing.transducerPSI.setFloatValue(transducer.getEquivalentBeamAngle());
         if (channelData != null) {
            dataProcessing.bandwidth.setFloatValue(channelData.getBandWidth() / 1000);
            dataProcessing.transceiverPower.setFloatValue(channelData.getTransmitPower());
            dataProcessing.transmitPulseLength.setFloatValue(channelData.getPulseDuration() * 1000);
            dataProcessing.onAxisGain.setFloatValue(transducer.getGainForPulseDuration(channelData.getPulseDuration()));
            dataProcessing.saCorrection.setFloatValue(transducer.getSaCorrection(channelData.getPulseDuration()));
            dataProcessing.absorption.setFloatValue(channelData.getAbsorptionCoefficient());
            dataProcessing.soundSpeed.setFloatValue(channelData.getSoundVelocity());
         } else {
            dataProcessing.onAxisGain.setFloatValue(transducer.getGain());
         }
         dataProcessing.onAxisGainUnit.setValue("dB");
         dataProcessing.comments.setValue(comment);
      }
   }

   private static @Nullable ChannelData findChannelData(DataFile dataFile, int channel) {
      return dataFile.getPingIndices().stream()
            .limit(dataFile.getRawFileConfiguration().getTransducerCount())
            .map(dataFile::getPing)
            .map(ping -> ping.getChannelData(channel))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
   }

   private static String dataFileToComment(DataFile dataFile) {
      return "Values from file " + dataFile.getSegmentHandle().getMainFile().getFileName();
   }

   static String schemaNameToFileName(String schemaName) {
      return schemaName + ".xml";
   }

   static Stream<String> findSchemaNames() {
      Stream<String> idRef = Stream.of(new IcesInstrument(), new IcesCalibration(), new IcesDataAcquisition(), new IcesDataProcessing())
            .flatMap(icesGroup -> icesGroup.getParameters().stream())
            .map(parameter -> parameter instanceof IdRefParameter p ? p : null)
            .filter(Objects::nonNull)
            .map(IdRefParameter::getSchemaName);
      Stream<String> other = Stream.of(
            IcesUtils.SURVEY_SCHEMA,
            IcesUtils.PLATFORM_SCHEMA,
            IcesUtils.ORGANIZATION_SCHEMA
      );
      return Stream.concat(idRef, other);
   }

   @Nullable Path icesSchemaDir() {
      Path mainDir = getConfigurationManager().getApplicationConfiguration().getDirectoryConf().mainDir.getFile();
      if (mainDir == null) {
         return null;
      }
      return mainDir.resolve("config").resolve("ICES-acoustic-metadata");
   }

   public List<IcesCode> readIcesCodes(String schemaName) {
      Path dir = icesSchemaDir();
      if (dir == null) {
         return List.of();
      }
      Path file = dir.resolve(schemaNameToFileName(schemaName));
      try {
         Document document = XmlUtils.readDocumentIfExists(file);
         if (document == null) {
            return List.of();
         }
         return document.getRootElement().elements().stream()
               .map(IcesCode::new)
               .toList();
      } catch (Exception e) {
         Log.global.log(Level.WARNING, "Error reading " + file, e);
         return List.of();
      }
   }
}
