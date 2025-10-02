package no.imr.tools.parameter;

import org.dom4j.Element;

import java.util.List;
import java.util.function.Supplier;

public final class ListConfigurable<T extends Configurable> extends Configurable {
   private final Supplier<T> valueSupplier;
   private final List<T> values;

   public ListConfigurable(Name name, Supplier<T> valueSupplier, List<T> values) {
      super(name);

      this.valueSupplier = valueSupplier;
      this.values = values;
   }

   public List<T> getValues() {
      return values;
   }

   @Override
   public Element toXml() {
      Element element = createElement();
      values.stream()
            .map(Configurable::toXml)
            .forEach(element::add);
      return element;
   }

   @Override
   public void fromXml(Element element) {
      values.clear();
      element.elements().forEach(itemElement -> {
         T value = valueSupplier.get();
         value.fromXml(itemElement);
         values.add(value);
      });
   }
}
