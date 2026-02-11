package no.imr.lsss.framework.config;

import com.google.common.collect.ImmutableMap;
import no.imr.lsss.LSSS;
import no.imr.lsss.plugins.FeaturePlugin;
import no.imr.tools.Utils;
import no.imr.tools.help.HelpID;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.swing.svg.SvgIcon;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTextPane;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for objects representing a part of the configuration.
 */
public abstract class ConfigurationUnit implements ParameterContainer {
   public static final String XML_CONFIGURATION = "configuration";
   public static final String XML_UNIT = "unit";
   public static final String XML_NAME = "name";

   private final FeaturePlugin plugin;
   private final Name name;
   private final String description;
   private final List<ConfigurationUnit> subUnits = new ArrayList<>();
   private Map<Integer, Element> configurationOfUnknownSubUnits = Map.of();

   protected ConfigurationUnit(FeaturePlugin plugin, Name name, String description) {
      this.plugin = plugin;
      this.name = name;
      this.description = description;
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of();
   }

   public LSSS getLSSS() {
      return plugin.getLSSS();
   }

   public ConfigurationManager getConfigurationManager() {
      return getLSSS().getConfigurationManager();
   }

   public void setFromSurvey(Path surveyDir) {
   }

   public void prepareForSaveDefault() {
   }

   /**
    * Called after everything has been created.
    */
   public void setup() {
      subUnits.forEach(ConfigurationUnit::setup);
   }

   public ParameterCollection getParameterCollection() {
      return new ParameterCollection(this);
   }

   public FeaturePlugin getPlugin() {
      return plugin;
   }

   public @Nullable SvgIcon getIcon() {
      return plugin.getIcon();
   }

   /**
    * Returns the name of this configuration unit used for presentation to the user.
    *
    * @return the name presented to the user
    */
   public String getDisplayName() {
      return name.displayName();
   }

   /**
    * Returns the persistent name of this configuration unit.
    * The persistent name is used for identification in serializations.
    *
    * @return the persistent name used for serialization
    */
   public String getPersistentName() {
      return name.persistentName();
   }

   public String getDescription() {
      return description;
   }

   public HelpID getHelpID() {
      return plugin.getHelpSet().createHelpID(getPersistentName());
   }

   public void showInConfigurationDialog() {
      getConfigurationManager().showDialog(this);
   }

   /**
    * Tests if this configuration unit has a configuration component.
    *
    * @return {@code true} if this configuration unit has a configuration component
    */
   public boolean hasConfigurationComponent() {
      return true;
   }

   /**
    * Returns the gui for editing this configuration unit.
    *
    * @return a swing component
    */
   public JComponent getComponent() {
      return GuiUtils.createScrollPane(createParameterEditor());
   }

   public boolean stopEditing() {
      return true;
   }

   public void removeView() {
   }

   public static JTextPane createInfoComponent(String text) {
      JTextPane textPane = GuiUtils.labelLikeHtmlTextPane(text);
      textPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 30, 0));
      return textPane;
   }

   public ParameterEditor createParameterEditor() {
      return createParameterEditor(getParameters());
   }

   public ParameterEditor createParameterEditor(List<? extends BaseParameter<?>> parameters) {
      ParameterEditor parameterEditor = new ParameterEditor(parameters);
      adaptParameterEditor(parameterEditor);
      return parameterEditor;
   }

   private void adaptParameterEditor(ParameterEditor parameterEditor) {
      parameterEditor.getGUIConfig().setHorizontalFill(true);
      parameterEditor.getGUIConfig().setParameterEnabledDecider(this::isParameterEnabled);
   }

   public boolean isParameterEnabled(BaseParameter<?> parameter) {
      return getConfigurationManager().canEdit(getMinimumUserProfileForEditing(parameter));
   }

   protected UserProfile getMinimumUserProfileForEditing(BaseParameter<?> parameter) {
      return UserProfile.NORMAL_USE;
   }

   /**
    * Serializes this configuration unit to XML.
    * Subunits should be recursively serialized.
    *
    * @return an XML element, or {@code null} if nothing to serialize
    */
   public Element toXml() {
      Element unitElement = DocumentHelper.createElement(XML_UNIT)
            .addAttribute(XML_NAME, getPersistentName());

      unitElement.add(toConfigurationXml());

      for (ConfigurationUnit subUnit : subUnits) {
         Element subXml = subUnit.toXml();
         if (!subXml.elements().isEmpty()) {
            unitElement.add(subXml);
         }
      }

      configurationOfUnknownSubUnits.forEach((index, element) -> {
         List<Element> elements = unitElement.elements();
         elements.add(Math.min(index, elements.size()), (Element) element.clone());
      });

      return unitElement;
   }

   public Element toConfigurationXml() {
      Element configuration = DocumentHelper.createElement(XML_CONFIGURATION);
      addToConfigurationXml(configuration);
      return configuration;
   }

   public void addToConfigurationXml(Element configurationElement) {
      configurationElement.add(getParameterCollection().toXml());
   }

   /**
    * Deserializes this configuration unit from XML.
    * Subunits should be recursively deserialized.
    *
    * @param element an XML element
    */
   public void fromXml(Element element) {
      Element configurationElement = element.element(XML_CONFIGURATION);
      if (configurationElement != null) {
         fromConfigurationXml(configurationElement);
      }

      Map<String, ConfigurationUnit> nameToSubUnit = subUnits.stream()
            .collect(Collectors.toMap(ConfigurationUnit::getPersistentName, Function.identity()));

      ImmutableMap.Builder<Integer, Element> unknowns = ImmutableMap.builder();
      for (Element subUnitElement : element.elements(XML_UNIT)) {
         String name = subUnitElement.attributeValue(XML_NAME);
         ConfigurationUnit subUnit = nameToSubUnit.get(name);
         if (subUnit == null) {
            Element copy = (Element) subUnitElement.clone();
            XmlUtils.removeBlankMixedContentText(copy);
            unknowns.put(element.elements().indexOf(subUnitElement), copy);
            continue;
         }
         subUnit.fromXml(subUnitElement);
      }
      configurationOfUnknownSubUnits = unknowns.build();
   }

   public void fromConfigurationXml(Element configurationElement) {
      Element parametersElement = configurationElement.element(ParameterCollection.XML_PARAMETERS);
      if (parametersElement != null) {
         getParameterCollection().fromXml(parametersElement);
      }
   }

   public <T extends ConfigurationUnit> T addSubConfigurationUnit(T unit) {
      subUnits.add(unit);
      return unit;
   }

   public List<ConfigurationUnit> getSubUnits() {
      return subUnits;
   }

   public Stream<ConfigurationUnit> getAllUnitsRecursively() {
      return Utils.recursiveStream(this, ConfigurationUnit::getSubUnits);
   }

   public <T extends ConfigurationUnit> Stream<T> getAllUnitsRecursively(Class<T> clazz) {
      return getAllUnitsRecursively()
            .gather(Utils.allOfType(clazz));
   }

   public void applyRecursively(Consumer<ConfigurationUnit> action) {
      action.accept(this);
      subUnits.forEach(subUnit -> subUnit.applyRecursively(action));
   }

   public void opened() {
   }

   public void cancelled() {
   }

   /**
    * Prepare for {@link #apply()}.
    *
    * @return {@code true} if ok or {@code false} if cancelled
    */
   public boolean prepareApply() {
      return true;
   }

   /**
    * Apply any edits not already applied.
    *
    * @return {@code true} if applied or {@code false} if cancelled
    */
   public boolean apply() {
      return true;
   }
}
