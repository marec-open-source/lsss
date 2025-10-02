package no.imr.tools.parameter;

import no.imr.tools.logging.Log;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ConfigParameter;
import no.marec.lsss.api.util.parameters.ValueConstraint;
import no.marec.lsss.api.util.parameters.ValueConstraints;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * Parameters that have a value.
 *
 * @param <T> the value type
 */
public abstract sealed class BaseValueParameter<T> extends BaseParameter<T> implements Consumer<T>, ConfigParameter<T>
      permits DynamicListParameter, RangeParameter, ValueParameter {

   private ValueConstraint<T> constraint;
   private T value;

   BaseValueParameter(Name name, T initialValue, Unit unit, String description, ValueConstraint<T> constraint) {
      super(name, unit, description);

      this.constraint = constraint;
      value = validate(initialValue);
   }

   @Override
   public void accept(T value) {
      setValue(value);
   }

   public void addListenerAndNotify(Consumer<? super T> listener) {
      subscribe(listener);
      listener.accept(value);
   }

   @Override
   public T getValue() {
      return value;
   }

   @Override
   public void setValue(T value) {
      if (this.value.equals(value)) {
         return;
      }
      this.value = validate(value);
      notifyListeners();
   }

   private T validate(T value) {
      String error = constraint.validate(value);
      if (error != null) {
         throw new ParameterException(this, error);
      }
      return value;
   }

   @Override
   public abstract Element toXml();

   @Override
   public void fromXml(Element element) {
      try {
         T value = xmlToValue(element);
         setValue(value);
      } catch (ParameterException e) {
         Log.global.log(Level.WARNING, "Ignoring invalid value for " + getPersistentName() + " at " + XmlUtils.getPath(element) + ": " + element.getText(), e);
      }
   }

   public abstract T xmlToValue(Element element);

   public ValueConstraint<T> getConstraint() {
      return constraint;
   }

   @Override
   public ConfigParameter<T> setConstraint(ValueConstraint<T> constraint) {
      String error = constraint.validate(value);
      if (error != null) {
         throw new IllegalArgumentException(error);
      }
      this.constraint = constraint;
      notifyListeners();
      return this;
   }

   public void setConstraintAndValue(ValueConstraint<T> constraint, T value) {
      String error = constraint.validate(value);
      if (error != null) {
         throw new IllegalArgumentException(error);
      }
      this.constraint = constraint;
      this.value = value;
      notifyListeners();
   }

   public void setConstraintAndPossiblyValue(ValueConstraint<T> constraint, T value) {
      if (constraint.isValid(this.value)) {
         this.constraint = constraint;
         notifyListeners();
      } else {
         setConstraintAndValue(constraint, value);
      }
   }

   public void setAllowedValuesAndValue(List<T> allowedValues, T value) {
      if (Objects.equals(constraint.getAllowedValues(), allowedValues) && this.value.equals(value)) {
         return;
      }
      setConstraintAndValue(ValueConstraints.ofValues(allowedValues), value);
   }

   public void setAllowedValuesAndPossiblyValue(List<T> allowedValues, T value) {
      if (Objects.equals(constraint.getAllowedValues(), allowedValues)) {
         return;
      }
      setConstraintAndPossiblyValue(ValueConstraints.ofValues(allowedValues), value);
   }

   public @Nullable List<T> getAllowedValues() {
      return constraint.getAllowedValues();
   }
}
