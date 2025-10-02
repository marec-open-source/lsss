package no.imr.korona.data.ping.items.channel;

import no.imr.korona.data.datagrams.MruDatagram;
import no.imr.korona.data.datagrams.Raw3Datagram;
import no.imr.korona.data.ping.items.PingConversion;
import no.imr.korona.data.ping.items.configuration.PulseForm;
import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.tools.logging.Log;
import no.imr.tools.logging.LogOnce;
import no.imr.tools.xml.XmlParse;
import no.imr.tools.xml.XmlParseException;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.Set;

public final class Raw3ToChannelData {
   private Raw3ToChannelData() {
   }

   public static @Nullable ChannelData toChannelData(Raw3Datagram raw3Datagram, PingConversion pingConversion) {
      Integer channel = pingConversion.getIdToChannel().get(raw3Datagram.channelId);
      if (channel == null) {
         Set<String> ignoredChannelIds = pingConversion.getPingConfiguration().getRawFileConfiguration().getIgnoredChannelIds();
         boolean isIgnored = ignoredChannelIds.contains(raw3Datagram.channelId)
               || ignoredChannelIds.stream().anyMatch(channelId -> raw3Datagram.channelId.startsWith(channelId + '#'));
         if (!isIgnored) {
            Log.global.log(Log.SILENT_WARNING, "No channel for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString());
         }
         return null;
      }

      Element channelParameter = pingConversion.getIdToChannelParameter().get(raw3Datagram.channelId);
      if (channelParameter == null) {
         Log.global.log(Log.SILENT_WARNING, "No XML0/Parameter/Channel for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString());
         return null;
      }

      RawFileConfiguration rawFileConfiguration = pingConversion.getPingConfiguration().getRawFileConfiguration();
      RawFileConfiguration.Xml0Info xml0Info = rawFileConfiguration.getXml0Info();
      if (xml0Info == null) {
         return null;
      }
      RawFileTransducer rawFileTransducer = rawFileConfiguration.getTransducers().get(channel - 1);

      ChannelData channelData;
      int pulseForm;
      float slope;
      try {
         pulseForm = XmlParse.intAttribute(channelParameter, "PulseForm");
         slope = XmlParse.floatAttribute(channelParameter, "Slope");

         switch (pulseForm) {
            case PulseForm.NARROWBAND -> {
               if (raw3Datagram.power != null) {
                  channelData = new PowerData(raw3Datagram.getNTDate());
               } else if (raw3Datagram.real != null && raw3Datagram.imag != null) {
                  NarrowbandData narrowbandData = new NarrowbandData(raw3Datagram.getNTDate());
                  channelData = narrowbandData;
                  narrowbandData.setData(raw3Datagram.real, raw3Datagram.imag, slope);
               } else {
                  Log.global.log(Log.SILENT_WARNING, "No valid data in RAW3 datagram for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString());
                  return null;
               }
            }
            case PulseForm.BROADBAND_LINEAR_UP,
                 PulseForm.BROADBAND_LINEAR_DOWN -> {
               if (raw3Datagram.power != null) {
                  LogOnce.warning("PulseForm " + pulseForm + " with dataType " + raw3Datagram.dataType
                        + " for channel " + raw3Datagram.channelId + " in file " + rawFileConfiguration.getDataFile()
                        + " is not supported yet", pingConversion.getPingConfiguration());
                  return null;
               } else if (raw3Datagram.real != null && raw3Datagram.imag != null) {
                  BroadbandData broadbandData = new BroadbandData(raw3Datagram.getNTDate(), pulseForm);
                  channelData = broadbandData;
                  broadbandData.setData(raw3Datagram.real, raw3Datagram.imag, slope);
               } else {
                  Log.global.log(Log.SILENT_WARNING, "No valid data in RAW3 datagram for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString());
                  return null;
               }
            }
            default -> {
               Log.global.log(Log.SILENT_WARNING, "Unrecognized PulseForm = " + pulseForm + ", XML0/Parameter/Channel for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString());
               return null;
            }
         }
      } catch (XmlParseException e) {
         Log.global.log(Log.SILENT_WARNING, "Error with XML0/Parameter/Channel for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString() + ": " + e);
         return null;
      }

      channelData.setChannel(channel);
      channelData.setOffset(raw3Datagram.offset);

      float transducerDepth = rawFileTransducer.getPos().z() - xml0Info.getWaterLevelDraft();
      RawFileTransducer.Xml0Info transducerXml0Info = rawFileTransducer.getXml0Info();
      if (transducerXml0Info != null && transducerXml0Info.isDropKeel()) {
         transducerDepth += xml0Info.getDropKeelOffset();
      }

      try {
         channelData.setPulseDuration(XmlParse.parseFloat(XmlParse.attribute(channelParameter, "PulseDuration", "PulseLength")));
         channelData.setSampleInterval(XmlParse.floatAttribute(channelParameter, "SampleInterval"));
         channelData.setTransmitPower(XmlParse.floatAttribute(channelParameter, "TransmitPower"));
         // unsure
         channelData.setTransmitMode(XmlParse.shortAttribute(channelParameter, "ChannelMode"));

         channelData.setTransducerDepth(XmlParse.floatAttribute(channelParameter, "TransducerDepth", transducerDepth));

         if (channelData instanceof BroadbandData broadbandData) {
            float frequencyStart = XmlParse.parseFloat(XmlParse.attribute(channelParameter, "FrequencyStart"));
            float frequencyEnd = XmlParse.parseFloat(XmlParse.attribute(channelParameter, "FrequencyEnd"));
            broadbandData.setFrequency(frequencyStart);
            broadbandData.setEndFrequency(frequencyEnd);
            broadbandData.setBandWidth(XmlParse.floatAttribute(channelParameter, "BandWidth", frequencyEnd - frequencyStart));
         } else {
            channelData.setFrequency(XmlParse.parseFloat(XmlParse.attribute(channelParameter, "Frequency", "FrequencyStart")));
            // In email to Rolf 2016-11-08 Lars Nonboe Andersen suggested: "1/PulseDuration for CW og FStop-FStart for FM"
            // In email 2018-02-09 Gavin says 1/PulseDuration gives an incorrect value. The actual bandwidth for CW data can be computed,
            // but it may not be worth the time to implement. Instead, we set it to NaN so that it shows up as unavailable in the numerical view.
            channelData.setBandWidth(XmlParse.floatAttribute(channelParameter, "BandWidth", Float.NaN));
         }
      } catch (XmlParseException e) {
         Log.global.log(Log.SILENT_WARNING, "Error with XML0/Parameter/Channel for " + raw3Datagram.channelId + ", " + pingConversion.getDateAndFileString() + ": " + e);
         return null;
      }

      channelData.setSoundVelocity(rawFileConfiguration.getSoundVelocityAverage());
      channelData.setAbsorptionCoefficient((float) xml0Info.getAbsorption().getAbsorption(channelData.getCenterFrequency()));

      MruDatagram mru = pingConversion.getMruDatagram();
      if (mru != null) {
         channelData.setHeave(mru.getHeave());
         channelData.setRoll(mru.getRoll());
         channelData.setPitch(mru.getPitch());
         channelData.setHeading(mru.getHeading());
      }

      if (channelData instanceof PowerData powerData) {
         if (rawFileTransducer.isWBT()) {
            powerData.setEffectivePulseDuration((float) NarrowbandData.calculateEffectiveTau(powerData, rawFileTransducer, slope));
         }
         powerData.setPingConfiguration(pingConversion.getPingConfiguration());
         powerData.setSvFromShortPower(raw3Datagram.power);
         if (raw3Datagram.angles != null) {
            float[] angles = AngleData.bytesToElectricalAngles(raw3Datagram.angles, rawFileTransducer);
            powerData.setElectricAngles(angles);
         }
      }

      return channelData;
   }
}
