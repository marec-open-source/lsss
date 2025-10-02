package no.imr.korona.data.formats.ek60.calibration;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import no.imr.korona.data.ping.items.configuration.RawFileTransducer;
import no.imr.korona.data.ping.items.configuration.TransducerNameAndSerialNumber;
import no.imr.tools.ValueOrError;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class CalibrationEntry {
   public static final CalibrationEntry EMPTY = new CalibrationEntry();

   private final ChannelCalibration defaultChannelCalibration;
   private final ImmutableMap<String, ChannelCalibration> byId;
   private final ImmutableMap<Integer, ChannelCalibration> byChannel;
   private final ImmutableMap<TransducerNameAndSerialNumber, ChannelCalibration> byNameAndSerialNumber;

   private CalibrationEntry() {
      defaultChannelCalibration = ChannelCalibration.EMPTY;
      byId = ImmutableMap.of();
      byChannel = ImmutableMap.of();
      byNameAndSerialNumber = ImmutableMap.of();
   }

   CalibrationEntry(ChannelCalibration defaultChannelCalibration, List<ChannelCalibration> channelCalibrations) {
      this.defaultChannelCalibration = defaultChannelCalibration;

      ImmutableMap.Builder<String, ChannelCalibration> byIdBuilder = ImmutableMap.builder();
      ImmutableMap.Builder<Integer, ChannelCalibration> byChannelBuilder = ImmutableMap.builder();
      ImmutableMap.Builder<TransducerNameAndSerialNumber, ChannelCalibration> byNameAndSerialNumberBuilder = ImmutableMap.builder();
      for (ChannelCalibration channelCalibration : channelCalibrations) {
         channelCalibration.id.ifPresent(id -> byIdBuilder.put(id, channelCalibration));
         channelCalibration.channel.ifPresent(channel -> byChannelBuilder.put(channel, channelCalibration));
         channelCalibration.nameAndSerialNumber.ifPresent(nameAndSerialNumber -> byNameAndSerialNumberBuilder.put(nameAndSerialNumber, channelCalibration));
      }
      byId = byIdBuilder.build();
      byChannel = byChannelBuilder.build();
      byNameAndSerialNumber = byNameAndSerialNumberBuilder.build();
   }

   public CalibrationEntry(ChannelCalibration defaultChannelCalibration,
                           ImmutableMap<String, ChannelCalibration> byId,
                           ImmutableMap<Integer, ChannelCalibration> byChannel,
                           ImmutableMap<TransducerNameAndSerialNumber, ChannelCalibration> byNameAndSerialNumber) {
      this.defaultChannelCalibration = defaultChannelCalibration;
      this.byId = byId;
      this.byChannel = byChannel;
      this.byNameAndSerialNumber = byNameAndSerialNumber;
   }

   public ChannelCalibration getDefaultChannelCalibration() {
      return defaultChannelCalibration;
   }

   public ValueOrError<ChannelCalibration> getChannelCalibration(RawFileTransducer transducer, int channel) {
      RawFileTransducer.Xml0Info xml0Info = transducer.getXml0Info();
      TransducerNameAndSerialNumber nameAndSerialNumber = xml0Info != null
            ? new TransducerNameAndSerialNumber(xml0Info.getName(), xml0Info.getTransducerSerialNumber()) : null;
      return getChannelCalibration(channel, transducer.getChannelId(), transducer.getKHz(), nameAndSerialNumber);
   }

   public ValueOrError<ChannelCalibration> getChannelCalibration(int channel, String id, int kHz, @Nullable TransducerNameAndSerialNumber nameAndSerialNumber) {
      ChannelCalibration calibration = getChannelCalibrationForIdOrChannelOrNameAndSerialNumber(id, channel, nameAndSerialNumber);

      if (calibration.id.isPresent() && !calibration.id.get().equals(id)) {
         return ValueOrError.error("Calibration for " + toIdInfo(calibration) + " does not match channel id " + id);
      }
      if (calibration.channel.isPresent() && calibration.channel.get() != channel) {
         return ValueOrError.error("Calibration for " + toIdInfo(calibration) + " does not match channel number " + channel);
      }
      if (calibration.nameAndSerialNumber.isPresent() && nameAndSerialNumber != null && !calibration.nameAndSerialNumber.get().equals(nameAndSerialNumber)) {
         return ValueOrError.error("Calibration for " + toIdInfo(calibration) + " does not match " + toNameAndSerialNumberString(nameAndSerialNumber));
      }
      if (calibration.kHz.isPresent() && calibration.kHz.get() != kHz) {
         return ValueOrError.error("Calibration for " + toIdInfo(calibration) + " specifies " + calibration.kHz.get() + " kHz, but is really " + kHz + " kHz");
      }

      return ValueOrError.of(calibration);
   }

   private static String toIdInfo(ChannelCalibration channelCalibration) {
      List<String> items = new ArrayList<>();
      channelCalibration.id.ifPresent(id -> items.add("id " + id));
      channelCalibration.channel.ifPresent(channel -> items.add("channel " + channel));
      channelCalibration.nameAndSerialNumber.ifPresent(nameAndSerialNumber -> items.add(toNameAndSerialNumberString(nameAndSerialNumber)));
      return Joiner.on(", ").join(items);
   }

   private static String toNameAndSerialNumberString(TransducerNameAndSerialNumber nameAndSerialNumber) {
      return "transducer name " + nameAndSerialNumber.transducerName() + ", serial number " + nameAndSerialNumber.serialNumber();
   }

   private ChannelCalibration getChannelCalibrationForIdOrChannelOrNameAndSerialNumber(String id, int channel, @Nullable TransducerNameAndSerialNumber nameAndSerialNumber) {
      ChannelCalibration channelCalibration = byId.get(id);
      if (channelCalibration != null) {
         return channelCalibration;
      }
      channelCalibration = byChannel.get(channel);
      if (channelCalibration != null) {
         return channelCalibration;
      }
      if (nameAndSerialNumber != null) {
         channelCalibration = byNameAndSerialNumber.get(nameAndSerialNumber);
         if (channelCalibration != null) {
            return channelCalibration;
         }
      }
      return defaultChannelCalibration;
   }

   void addXml(Element element) {
      if (defaultChannelCalibration != ChannelCalibration.EMPTY) {
         defaultChannelCalibration.addXml(element.addElement(CalibrationXml.DEFAULT));
      }
      Stream.of(byId, byChannel, byNameAndSerialNumber)
            .flatMap(map -> map.values().stream())
            .distinct()
            .sorted(Comparator.<ChannelCalibration>comparingInt(cal -> cal.channel.orElse(0))
                  .thenComparingInt(cal -> cal.kHz.orElse(0))
                  .thenComparing(cal -> cal.id.orElse("")))
            .forEach(c -> c.addXml(element.addElement(CalibrationXml.CASE)));
   }

   @Override
   public boolean equals(@Nullable Object obj) {
      if (this == obj) {
         return true;
      }
      return obj instanceof CalibrationEntry that
            && defaultChannelCalibration.equals(that.defaultChannelCalibration)
            && byId.equals(that.byId)
            && byChannel.equals(that.byChannel)
            && byNameAndSerialNumber.equals(that.byNameAndSerialNumber);
   }

   @Override
   public int hashCode() {
      int result = defaultChannelCalibration.hashCode();
      result = 31 * result + byId.hashCode();
      result = 31 * result + byChannel.hashCode();
      result = 31 * result + byNameAndSerialNumber.hashCode();
      return result;
   }
}
