package no.imr.tools.parameter;

import com.google.common.collect.ImmutableList;
import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Element;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.logging.Level;

/**
 * A list of values with separate input fields.
 *
 * @param <T> the value type
 */
public abstract non-sealed class DynamicListParameter<T> extends BaseValueParameter<List<T>> {
   private final ValueConverter<T> converter;

   protected DynamicListParameter(Name name, List<T> initialValue, Unit unit, ValueConstraint<T> constraint, ValueConverter<T> converter) {
      this(name, initialValue, unit, constraint, converter, "");
   }

   protected DynamicListParameter(Name name, List<T> initialValue, Unit unit, ValueConstraint<T> constraint, ValueConverter<T> converter, String description) {
      super(name, initialValue, unit, description, ValueConstraints.listItem(constraint, converter));

      this.converter = converter;
   }

   protected DynamicListParameter(Name name, List<T> initialValue, Unit unit, ValueConverter<T> converter) {
      this(name, initialValue, unit, converter, "");
   }

   protected DynamicListParameter(Name name, List<T> initialValue, Unit unit, ValueConverter<T> converter, String description) {
      super(name, initialValue, unit, description, ValueConstraints.none());

      this.converter = converter;
   }

   public ValueConverter<T> getConverter() {
      return converter;
   }

   public abstract ValueParameter<Optional<T>> newOptionalParameter(int index, String persistentName);

   @Override
   public Element toXml() {
      Element element = createElement();
      for (T item : getValue()) {
         element.addElement("value")
               .addText(converter.stringify(item));
      }
      return element;
   }

   @Override
   public String toString() {
      return getPersistentName() + " = " + getStringValues();
   }

   public List<String> getStringValues() {
      return getValue().stream()
            .map(converter::stringify)
            .toList();
   }

   public void setStringValues(List<String> stringValues) {
      List<T> value = stringsToValues(stringValues);
      setValue(value);
   }

   private List<T> stringsToValues(List<String> stringValues) {
      return stringValues.stream()
            .filter(Predicate.not(String::isEmpty))
            .map(stringValue -> {
               try {
                  return converter.parse(stringValue);
               } catch (Exception e) {
                  throw new ParameterException(this, e);
               }
            })
            .toList();
   }

   @Override
   public List<T> xmlToValue(Element element) {
      ImmutableList.Builder<T> builder = ImmutableList.builder();
      for (Element itemElement : element.elements()) {
         String text = itemElement.getText();
         if (text.isEmpty()) {
            continue;
         }
         try {
            builder.add(converter.parse(text));
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Ignoring invalid value for " + getPersistentName() + " at " + XmlUtils.getPath(itemElement) + ": " + text, e);
         }
      }
      return builder.build();
   }
}
