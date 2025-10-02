package no.imr.korona.util.schoolparameters;

import no.imr.tools.parameter.Name;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.List;

public abstract class ListSchoolParameter<T extends BaseSchoolParameter> extends BaseSchoolParameter {
   private final List<T> parameters = new ArrayList<>();
   private final String format;

   protected ListSchoolParameter(Name name, String unit, String format) {
      super(name, unit);

      this.format = format;
   }

   public String getFormat() {
      return format;
   }

   protected abstract T createNewParameter(Name name, String unit, String format);

   @Override
   public List<String> getExportNames() {
      return List.of(getName().persistentName());
   }

   @Override
   public List<String> getExportValues() {
      return List.of(getDisplayValue());
   }

   @Override
   public String getDisplayValue() {
      char textQualifier = '"';
      char itemDelimiter = ';';
      StringBuilder sb = new StringBuilder();
      int counter = 0;
      for (T parameter : parameters) {
         if (counter == 0) {
            sb.append(parameter.getDisplayValue());
         } else {
            sb.append(itemDelimiter).append(parameter.getDisplayValue());
         }
         counter++;
      }
      // Remove any non-enclosing text qualifiers created at lower levels
      for (int index = 1; index < sb.length() - 1; ) {
         if (sb.charAt(index) == textQualifier) {
            sb.deleteCharAt(index);
         } else {
            index++;
         }
      }
      if (sb.isEmpty()) {
         sb.append(textQualifier).append(textQualifier);
      } else {
         if (sb.charAt(0) != textQualifier) {
            sb.insert(0, textQualifier);
         }
         if (sb.charAt(sb.length() - 1) != textQualifier) {
            sb.append(textQualifier);
         }
      }
      return sb.toString();
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      for (T parameter : parameters) {
         element.add(parameter.toXml());
      }

      return element;
   }

   @Override
   public void doRestoreFromXml(Element element) {
      for (Element parameterElement : element.elements()) {
         T parameter = createNewParameter(new Name(parameterElement.getName()), "", format);
         parameter.fromXml(parameterElement);
         parameters.add(parameter);
      }
   }

   public void addValue(T value) {
      parameters.add(value);
   }

   public void clear() {
      parameters.clear();
   }

   public List<T> getList() {
      return parameters;
   }
}
