package no.imr.lsss.modules.echogramplot.functions;

import no.imr.korona.data.datamanager.DataFileSet;
import no.imr.korona.data.ping.Ping;
import no.imr.korona.data.ping.items.NmeaPingItem;
import no.imr.korona.data.util.Nmea;
import no.imr.lsss.framework.InterpretationSettings;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.OptionalStringParameter;
import no.imr.tools.parameter.Unit;
import no.imr.tools.plot.ExportTransform;
import no.marec.lsss.api.util.parameters.ValueConstraint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NmeaFunction extends PingFunction {
   private final String nmeaType;
   private final int fieldIndex;

   private NmeaFunction(String nmeaType, int fieldIndex) {
      super(new Name("NMEA: " + nmeaType + "," + fieldIndex), Unit.NONE, ExportTransform.identity());

      if (fieldIndex < 0) {
         throw new IllegalArgumentException(String.valueOf(fieldIndex));
      }

      this.nmeaType = nmeaType;
      this.fieldIndex = fieldIndex;

      selected.setBooleanValue(true);
   }

   @Override
   public double compute(DataFileSet dataFileSet, Ping ping, int channel) {
      return ping.getPingItems(NmeaPingItem.class)
            .map(NmeaPingItem::getNmeaString)
            .map(Nmea::toFields)
            .filter(fields -> fields.size() > fieldIndex && fields.getFirst().equals(nmeaType))
            .mapToDouble(fields -> {
               try {
                  return Double.parseDouble(fields.get(fieldIndex));
               } catch (NumberFormatException _) {
                  return Double.NaN;
               }
            })
            .findFirst()
            .orElse(Double.NaN);
   }

   public static final class NmeaParameter extends OptionalStringParameter {
      private static final Pattern PATTERN = Pattern.compile("(\\w+),(\\d+)");
      public static final ValueConstraint<String> CONSTRAINT = value -> {
         return PATTERN.matcher(value).matches() ? null : "Must be of the form \"nmeaType,fieldIndex\", i.e., match the regular expression " + PATTERN;
      };

      private final InterpretationSettings interpretationSettings;

      public NmeaParameter(InterpretationSettings interpretationSettings, String persistentName) {
         super(new Name(persistentName, "NMEA"), Optional.empty(), CONSTRAINT, "Format: nmeaType,fieldIndex. Example: GPGGA,2");

         this.interpretationSettings = interpretationSettings;
      }

      public static NmeaFunction stringToFunction(String string) {
         Matcher matcher = PATTERN.matcher(string);
         if (!matcher.matches()) {
            throw new IllegalArgumentException(string);
         }
         String nmeaType = matcher.group(1);
         int fieldIndex = Integer.parseInt(matcher.group(2));
         return new NmeaFunction(nmeaType, fieldIndex);
      }

      @Override
      public List<Optional<String>> getSuggestedValues() {
         Map<String, List<String>> typeToFields = new TreeMap<>();
         interpretationSettings.getPingSampler().getAvailablePings().stream()
               .map(Ping::getAvailablePingData)
               .filter(Objects::nonNull)
               .flatMap(pingData -> pingData.getPingItems(NmeaPingItem.class))
               .forEach(nmeaPingItem -> {
                  List<String> fields = Nmea.toFields(nmeaPingItem.getNmeaString());
                  typeToFields.put(fields.getFirst(), fields);
               });
         List<Optional<String>> suggestedValues = new ArrayList<>();
         for (List<String> fields : typeToFields.values()) {
            for (int i = 1; i < fields.size(); i++) {
               try {
                  Double.parseDouble(fields.get(i));
               } catch (NumberFormatException _) {
                  continue;
               }
               suggestedValues.add(Optional.of(fields.getFirst() + "," + i));
            }
         }
         return suggestedValues;
      }
   }
}
