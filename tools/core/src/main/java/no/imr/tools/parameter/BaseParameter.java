package no.imr.tools.parameter;

import com.google.common.collect.ImmutableMap;
import no.imr.tools.ImmutableUtils;
import no.imr.tools.listening.ArgChangeManager;
import no.imr.tools.swing.svg.SvgIcon;
import no.marec.lsss.api.util.observing.Subscription;
import no.marec.lsss.api.util.parameters.BaseConfigParameter;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Base class for parameters.
 */
public abstract sealed class BaseParameter<T> extends Configurable implements BaseConfigParameter<T>
      permits BaseValueParameter, MultiParameter, VoidParameter {

   public static final String XML_PARAMETER = "parameter";

   public static final PropertyKey<Boolean> KEY_COMBINE_INPUT_AND_DESCRIPTION = new PropertyKey<>(false);
   public static final PropertyKey<Boolean> KEY_HORIZONTAL_FILL = new PropertyKey<>(false);
   public static final PropertyKey<Optional<SvgIcon>> KEY_ICON = new PropertyKey<>(Optional.empty());
   public static final PropertyKey<Boolean> KEY_LEFT_ALIGNED = new PropertyKey<>(false);
   public static final PropertyKey<Boolean> KEY_MONOSPACED = new PropertyKey<>(false);
   public static final PropertyKey<Boolean> KEY_PERSISTABLE = new PropertyKey<>(true);
   public static final PropertyKey<Integer> KEY_ROWS = new PropertyKey<>(5);
   public static final PropertyKey<Boolean> KEY_VERTICAL_FILL = new PropertyKey<>(false);
   public static final PropertyKey<List<?>> KEY_SUGGESTED_VALUES = new PropertyKey<>(List.of());

   private Unit unit;
   private String description;
   private boolean enabled = true;
   private boolean visible = true;
   private ImmutableMap<PropertyKey<?>, Object> properties = ImmutableMap.of();

   private final ArgChangeManager<T> changeManager = new ArgChangeManager<>();

   protected BaseParameter(Name name, Unit unit, String description) {
      super(name);

      this.unit = unit;
      this.description = description;
   }

   public boolean isEnabled() {
      return enabled;
   }

   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   public boolean isVisible() {
      return visible;
   }

   public void setVisible(boolean visible) {
      this.visible = visible;
   }

   @Override
   public boolean isPersistable() {
      return getProperty(KEY_PERSISTABLE);
   }

   public void setPersistable(boolean persistable) {
      setProperty(KEY_PERSISTABLE, persistable);
   }

   public <P> P getProperty(PropertyKey<P> key) {
      return key.get(properties);
   }

   public <P> void setProperty(PropertyKey<P> key, P value) {
      properties = key.defaultValue.equals(value)
            ? ImmutableUtils.remove(properties, key)
            : ImmutableUtils.put(properties, key, value);
   }

   @Override
   public Subscription subscribe(Consumer<? super T> observer) {
      return changeManager.subscribe(observer);
   }

   public ArgChangeManager<T> getChangeManager() {
      return changeManager;
   }

   abstract T getValue();

   public void notifyListeners() {
      changeManager.notifyListeners(getValue());
   }

   public String getDisplayName() {
      return getName().displayName();
   }

   public String getPersistentName() {
      return getName().persistentName();
   }

   public Unit getUnit() {
      return unit;
   }

   public void setUnit(Unit unit) {
      this.unit = unit;
   }

   public String getDescription() {
      return description;
   }

   public void setDescription(String description) {
      this.description = description;
   }

   @Override
   protected Element createElement() {
      return createElementWithNameAttribute(XML_PARAMETER);
   }

   @Override
   public String toString() {
      return getPersistentName() + " (" + description + ")";
   }

   public @Nullable String getAllowedValuesDescription() {
      return null;
   }

   public static final class PropertyKey<P> {
      private final P defaultValue;

      private PropertyKey(P defaultValue) {
         this.defaultValue = defaultValue;
      }

      @SuppressWarnings("unchecked")
      private P get(Map<PropertyKey<?>, Object> map) {
         return (P) map.getOrDefault(this, defaultValue);
      }
   }
}
