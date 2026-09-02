package no.imr.korona.util;

import no.imr.korona.data.ping.items.configuration.RawFileConfiguration;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.ValueConverter;
import no.imr.tools.parameter.ValueConverters;
import no.imr.tools.parameter.ValueParameter;

public final class SingleChannelParameter extends ValueParameter<SingleChannelParameter.SingleChannelValue> {
   private static final ValueConverter<SingleChannelValue> CONVERTER = ValueConverters.of(
         SingleChannelParameter::parseSingleChannelValue, SingleChannelValue::toString
   );

   public SingleChannelParameter(Name name, String initialValue) {
      super(name, parseSingleChannelValue(initialValue), Unit.NONE, CONVERTER,
            "Use C1, C2, ..., or CE1, CE2, ..., or F18, F38, ...");
   }

   public int selectedChannel(RawFileConfiguration rawFileConfiguration) {
      return getValue().selectedChannel(rawFileConfiguration);
   }

   private static SingleChannelValue parseSingleChannelValue(String string) {
      if (string.startsWith("C")) {
         if (string.startsWith("CE")) {
            return new ChannelFromEndValue(Integer.parseInt(string.substring(2)));
         }
         return new ChannelValue(Integer.parseInt(string.substring(1)));
      }
      if (string.startsWith("F")) {
         return new FrequencyValue(Integer.parseInt(string.substring(1)));
      }
      throw new IllegalArgumentException(string);
   }

   sealed interface SingleChannelValue {
      int selectedChannel(RawFileConfiguration rawFileConfiguration);
   }

   private record ChannelValue(int channel) implements SingleChannelValue {
      private ChannelValue {
         if (channel <= 0) {
            throw new IllegalArgumentException("C <= 0");
         }
      }

      @Override
      public int selectedChannel(RawFileConfiguration rawFileConfiguration) {
         int transducerCount = rawFileConfiguration.getTransducerCount();
         return channel <= transducerCount ? channel : -1;
      }

      @Override
      public String toString() {
         return "C" + channel;
      }
   }

   private record ChannelFromEndValue(int channelFromEnd) implements SingleChannelValue {
      private ChannelFromEndValue {
         if (channelFromEnd <= 0) {
            throw new IllegalArgumentException("CE <= 0");
         }
      }

      @Override
      public int selectedChannel(RawFileConfiguration rawFileConfiguration) {
         int transducerCount = rawFileConfiguration.getTransducerCount();
         return channelFromEnd <= transducerCount ? transducerCount - channelFromEnd + 1 : -1;
      }

      @Override
      public String toString() {
         return "CE" + channelFromEnd;
      }
   }

   private record FrequencyValue(int kHz) implements SingleChannelValue {
      private FrequencyValue {
         if (kHz <= 0) {
            throw new IllegalArgumentException(kHz + " <= 0");
         }
      }

      @Override
      public int selectedChannel(RawFileConfiguration rawFileConfiguration) {
         return rawFileConfiguration.lastChannelWithKHz(kHz);
      }

      @Override
      public String toString() {
         return "F" + kHz;
      }
   }
}
