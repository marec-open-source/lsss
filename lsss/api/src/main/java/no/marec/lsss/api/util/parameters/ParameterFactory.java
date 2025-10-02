package no.marec.lsss.api.util.parameters;

import no.marec.lsss.api.LsssAccess;
import no.marec.lsss.api.internal.InternalLsss;
import no.marec.lsss.api.internal.InternalParameterFactory;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * Factory for creating instances of {@link ConfigParameter}.
 */
public final class ParameterFactory {
   private static final InternalParameterFactory FACTORY = InternalLsss.INSTANCE.parameterFactory();

   private ParameterFactory() {
   }

   /**
    * {@return a new parameter for boolean values}
    *
    * @param id           the parameter ID
    * @param label        the display label
    * @param initialValue the initial value
    * @param description  an optional description
    */
   public static ConfigParameter<Boolean> booleanParameter(
         String id, String label,
         boolean initialValue,
         String description) {
      return FACTORY.booleanParameter(id, label, initialValue, description);
   }

   /**
    * {@return a new parameter for int values}
    *
    * @param id           the parameter ID
    * @param label        the display label
    * @param initialValue the initial value
    * @param unit         the unit (can be empty)
    * @param description  a description (can be empty)
    */
   public static ConfigParameter<Integer> intParameter(
         String id, String label,
         int initialValue, String unit,
         String description) {
      return FACTORY.intParameter(id, label, initialValue, unit, description);
   }

   /**
    * {@return a new parameter for optional int values}
    *
    * @param id           the parameter ID
    * @param label        the display label
    * @param initialValue the initial value
    * @param unit         the unit (can be empty)
    * @param description  a description (can be empty)
    */
   public static ConfigParameter<Optional<Integer>> optionalIntParameter(
         String id, String label,
         Optional<Integer> initialValue, String unit,
         String description) {
      return FACTORY.optionalIntParameter(id, label, initialValue, unit, description);
   }

   /**
    * {@return a new parameter for float values}
    *
    * @param id           the parameter ID
    * @param label        the display label
    * @param initialValue the initial value
    * @param unit         the unit (can be empty)
    * @param description  a description (can be empty)
    */
   public static ConfigParameter<Float> floatParameter(
         String id, String label,
         float initialValue, String unit,
         String description) {
      return FACTORY.floatParameter(id, label, initialValue, unit, description);
   }

   /**
    * {@return a new parameter for optional float values}
    *
    * @param id           the parameter ID
    * @param label        the display label
    * @param initialValue the initial value
    * @param unit         the unit (can be empty)
    * @param description  a description (can be empty)
    */
   public static ConfigParameter<Optional<Float>> optionalFloatParameter(
         String id, String label,
         Optional<Float> initialValue, String unit,
         String description) {
      return FACTORY.optionalFloatParameter(id, label, initialValue, unit, description);
   }

   /**
    * {@return a new parameter for string values}
    *
    * @param id           the parameter ID
    * @param label        the display label
    * @param initialValue the initial value
    * @param description  a description (can be empty)
    */
   public static ConfigParameter<String> stringParameter(
         String id, String label,
         String initialValue,
         String description) {
      return FACTORY.stringParameter(id, label, initialValue, description);
   }

   /**
    * {@return a new parameter for selection among a list of values}
    * <p>
    * If the value type implements {@link ObjectParameterValue},
    * then the selection display is modified accordingly.
    *
    * @param id            the parameter ID
    * @param label         the display label
    * @param initialValue  the initial value
    * @param allowedValues a list of possible values
    * @param description   a description (can be empty)
    * @param <T>           the value type
    */
   public static <T> ConfigParameter<T> objectParameter(
         String id, String label,
         T initialValue, List<T> allowedValues,
         String description) {
      return FACTORY.selectionParameter(id, label, initialValue, allowedValues, description);
   }

   /**
    * {@return a parameter rendered as a clickable button}
    *
    * @param id          the parameter ID
    * @param label       the display label
    * @param description a description (can be empty)
    */
   public static VoidConfigParameter button(String id, String label, String description) {
      return FACTORY.button(id, label, description);
   }

   /**
    * {@return a parameter rendered as a header}
    *
    * @param text the header text
    */
   public static VoidConfigParameter header(String text) {
      return FACTORY.header(text);
   }

   /**
    * {@return a parameter rendered as a horizontal line}
    */
   public static VoidConfigParameter lineSeparator() {
      return FACTORY.lineSeparator();
   }

   /**
    * {@return a parameter rendered as a vertical space}
    */
   public static VoidConfigParameter spaceSeparator() {
      return FACTORY.spaceSeparator();
   }

   /**
    * {@return a new survey directory parameter}
    *
    * @param lsssAccess an LSSS instance
    * @param dirId      the ID
    * @param path       the path relative to the survey directory
    */
   public static ConfigParameter<Optional<Path>> surveyDirectoryParameter(LsssAccess lsssAccess, String dirId, String path) {
      return FACTORY.surveyDirectoryParameter(lsssAccess, dirId, path);
   }
}
