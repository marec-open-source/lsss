package no.imr.lsss.database.reports;

import no.imr.lsss.LSSS;
import no.imr.lsss.database.tables.hibernate.Observation;
import no.imr.lsss.database.tables.hibernate.Scatter;
import no.imr.lsss.database.tables.hibernate.SurveyPK;
import no.imr.lsss.database.util.DatabaseTime;
import no.imr.tools.Utils;
import no.imr.tools.time.TimeUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

final class PrintUser20 extends BaseMultiFrequencyXmlReport {
   private int lastDate = -1;
   private int lastTime = -1;

   PrintUser20(ReportEngine reportEngine) {
      super(20, reportEngine);
   }

   @Override
   void printHeaderXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException {
      xmlStreamWriter.writeStartDocument("UTF-8", "1.0");
      //xmlStreamWriter.writeDTD("<!DOCTYPE echosounder_dataset SYSTEM \"echosounder_dataset.dtd\">");
      xmlStreamWriter.writeStartElement("echosounder_dataset");

      SurveyPK surveyId = aPrintData.getSurvey().getCompId();

      DateTimeFormatter utcDateFormat = TimeUtils.createUTCDateTimeFormatter("yyyy-MM-dd HH:mm:ss");
      String time = utcDateFormat.format(Instant.now());

      writeSimpleElement("report_time", time);
      writeSimpleElement("lsss_version", LSSS.VERSION);
      writeSimpleElement("nation", surveyId.getNation());
      writeSimpleElement("platform", surveyId.getPlatform());
      writeSimpleElement("cruise", surveyId.getSurvey());

      xmlStreamWriter.writeStartElement("distance_list");
   }

   @Override
   void printXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData, ReportMode aMode, PrintData.Bottom aPrintDataBottom) throws XMLStreamException {
      float[][] dataPrint = aPrintData.getSa(aMode);
      float[][] dataPrintBottom = aPrintDataBottom.getSa(aMode);

      if (aPrintData.getScatter(aMode).getCompId().getObservationDate() != lastDate ||
            aPrintData.getScatter(aMode).getCompId().getObservationTime() != lastTime) {
         if (lastTime != -1) {
            xmlStreamWriter.writeEndElement(); // distance
         }
         lastDate = aPrintData.getScatter(aMode).getCompId().getObservationDate();
         lastTime = aPrintData.getScatter(aMode).getCompId().getObservationTime();

         xmlStreamWriter.writeStartElement("distance");
         writeAttribute("log_start", aPrintData.getObservation(aMode).getDistance());

         Instant startTime = DatabaseTime.toInstant(aPrintData.getScatter(aMode));
         xmlStreamWriter.writeAttribute("start_time", ReportUtils.DATE_TIME.format(startTime));

         {
            int duration = aPrintData.getScatter(aMode).getDuration();
            Instant stopTime = startTime.plusMillis(duration * 10L);
            writeSimpleElement("stop_time", ReportUtils.DATE_TIME.format(stopTime));
         }
         writeSimpleElement("integrator_dist", aPrintData.getScatter(aMode).getDistanceInterval());
         writeSimpleElement("pel_ch_thickness", aPrintData.getScatter(aMode).getChannelThickness());
         if (aPrintData.getScatter(aMode).getBottomActive() == 1) {
            Scatter bottomScatter = aPrintDataBottom.getScatter(aMode);
            if (bottomScatter != null) { // may not be bottom-channels for school-data
               writeSimpleElement("bot_ch_thickness", bottomScatter.getChannelThickness());
            } else {
               writeSimpleElement("bot_ch_thickness", "");
            }
         } else {
            writeSimpleElement("bot_ch_thickness", "");
         }
         writeSimpleElement("include_estimate", 1);  //include_estimate = 1 (always included)
         writeSimpleElement("lat_start", aPrintData.getObservation(aMode).getLatitude());
         Observation observationStop = aPrintData.getObservationStop(aMode);
         if (observationStop != null) {
            writeSimpleElement("lat_stop", observationStop.getLatitude());
         } else {
            writeSimpleElement("lat_stop", "");
         }
         writeSimpleElement("lon_start", aPrintData.getObservation(aMode).getLongitude());
         if (observationStop != null) {
            writeSimpleElement("lon_stop", observationStop.getLongitude());
         } else {
            writeSimpleElement("lon_stop", "");
         }
      }

      xmlStreamWriter.writeStartElement("frequency");
      writeAttribute("freq", aPrintData.getScatter(aMode).getCompId().getFrequency());
      writeAttribute("transceiver", aPrintData.getScatter(aMode).getCompId().getTransceiver());

      writeSimpleElement("threshold", aPrintData.getScatter(aMode).getThreshold());
      writeSimpleElement("num_pel_ch", aPrintData.getMaxChannel(aMode));
      if (aPrintData.getScatter(aMode).getBottomActive() == 1) {
         if (aPrintDataBottom.getScatter(aMode) != null) { // may not be bottom-channels for school-data
            writeSimpleElement("num_bot_ch", aPrintDataBottom.getMaxChannel(aMode));
         } else {
            writeSimpleElement("num_bot_ch", 0);
         }
      } else {
         writeSimpleElement("num_bot_ch", 0);
      }
      writeSimpleElement("min_bot_depth", aPrintData.getScatter(aMode).getMinBottomDepth());
      writeSimpleElement("max_bot_depth", aPrintData.getScatter(aMode).getMaxBottomDepth());
      writeSimpleElement("upper_interpret_depth", aPrintData.getScatter(aMode).getUpperInterpretationDepth());
      writeSimpleElement("lower_interpret_depth", aPrintData.getScatter(aMode).getLowerInterpretationDepth());
      writeSimpleElement("upper_integrator_depth", aPrintData.getScatter(aMode).getUpperDepth());
      writeSimpleElement("lower_integrator_depth", aPrintData.getScatter(aMode).getLowerDepth());
      writeSimpleElement("quality", Math.round(aPrintData.getScatter(aMode).getQuality())); // Scatter.quality is defined as "float", but should have been "short"
      writeSimpleElement("bubble_corr", aPrintData.getScatter(aMode).getBubbleCorrection());

      // Pelagic channels
      xmlStreamWriter.writeStartElement("ch_type");
      xmlStreamWriter.writeAttribute("type", "P");    //Pelagic data follows

      for (int i = 0; i < aPrintData.getPrintCount(); i++) {
         if (dataPrint[i][0] > 0.0) { // Print only species with values
            xmlStreamWriter.writeStartElement("sa_by_acocat");
            writeAttribute("acocat", aPrintData.getAcousticCategory(i).getCompId().getAcousticCategory());  //Species code

            for (int j = 1; j <= aPrintData.getMaxChannel(aMode); j++) { // Sum channel (j=0) not included
               if (dataPrint[i][j] > 0.0) {
                  xmlStreamWriter.writeStartElement("sa");
                  writeAttribute("ch", j);
                  xmlStreamWriter.writeCharacters(Utils.toString(dataPrint[i][j]));
                  xmlStreamWriter.writeEndElement();
               }
            }
            xmlStreamWriter.writeEndElement(); // sa_by_acocat
         }
      }

      xmlStreamWriter.writeEndElement(); // ch_type Pelagic

      if (aPrintData.getScatter(aMode).getBottomActive() == 1) {
         if (aPrintDataBottom.getScatter(aMode) != null) { // may not be bottom-channels for school-data
            // Bottom channels
            xmlStreamWriter.writeStartElement("ch_type");
            xmlStreamWriter.writeAttribute("type", "B");    //Bottom data follows

            for (int i = 0; i < aPrintDataBottom.getPrintCount(); i++) {
               if (dataPrintBottom[i][0] > 0.0) { //Print only species with values: species "i", channel 0 is sum of sa
                  xmlStreamWriter.writeStartElement("sa_by_acocat");
                  writeAttribute("acocat", aPrintData.getAcousticCategory(i).getCompId().getAcousticCategory());  //Species code

                  for (int j = 1; j <= aPrintDataBottom.getMaxChannel(aMode); j++) { // Sum channel (j=0) not included
                     if (dataPrintBottom[i][j] > 0.0) { // Only positive sa are included
                        xmlStreamWriter.writeStartElement("sa");
                        writeAttribute("ch", j);
                        xmlStreamWriter.writeCharacters(Utils.toString(dataPrintBottom[i][j]));
                        xmlStreamWriter.writeEndElement(); //sa
                     }
                  }
                  xmlStreamWriter.writeEndElement(); // sa_by_acocat
               }
            }

            xmlStreamWriter.writeEndElement(); // ch_type Bottom
         }
      }

      xmlStreamWriter.writeEndElement(); // frequency
   }

   @Override
   void finaliseReportXml(XMLStreamWriter xmlStreamWriter, PrintData.Pelagic aPrintData) throws XMLStreamException {
      if (lastTime != -1) {
         xmlStreamWriter.writeEndElement(); // distance - end of last
      }
      xmlStreamWriter.writeEndElement(); // distance_list - end of last

      xmlStreamWriter.writeStartElement("acocat_list");

      for (int i = 0; i < aPrintData.getPrintCount(); i++) {
         xmlStreamWriter.writeStartElement("acocat");
         int acousticCategory = aPrintData.getAcousticCategory(i).getCompId().getAcousticCategory();
         writeAttribute("acocat", acousticCategory);  //Species code
         writeSimpleElement("purpose", aPrintData.getPurposePrint(acousticCategory));
         xmlStreamWriter.writeEndElement(); //acocat
      }
      xmlStreamWriter.writeEndElement(); // acocat_list

      xmlStreamWriter.writeEndElement(); // echosounder_dataset

      xmlStreamWriter.writeEndDocument();
   }
}  //PrintUser20
