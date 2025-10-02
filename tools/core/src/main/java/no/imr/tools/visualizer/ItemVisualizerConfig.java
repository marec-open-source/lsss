package no.imr.tools.visualizer;

import no.imr.tools.listening.Listener;
import no.imr.tools.logging.Log;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.imr.tools.swing.GuiUtils;
import no.imr.tools.xml.XmlUtils;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.util.List;
import java.util.logging.Level;
import java.util.prefs.Preferences;

final class ItemVisualizerConfig implements ParameterContainer {
   final IntParameter selectedDotSize = new IntParameter(
         new Name("selectedDotSize", "Selected dot size"),
         3, Unit.PT, ValueConstraints.gt(0),
         "Size of selected dots in the scatter plot");

   final IntParameter unselectedDotSize = new IntParameter(
         new Name("unselectedDotSize", "Unselected dot size"),
         3, Unit.PT, ValueConstraints.gt(0),
         "Size of unselected dots in the scatter plot");

   private final Preferences preferences;
   private final JScrollPane scrollPane;

   ItemVisualizerConfig(Preferences preferences) {
      this.preferences = preferences;
      ParameterCollection parameterCollection = new ParameterCollection(this);
      String config = preferences.get("config", null);
      if (config != null) {
         try {
            parameterCollection.fromXml(XmlUtils.readDocument(config).getRootElement());
         } catch (Exception e) {
            Log.global.log(Level.WARNING, "Error parsing xml", e);
         }
      }
      Listener.of(() -> {
         preferences.put("config", XmlUtils.toDefaultString(parameterCollection.toXml()));
      }).addTo(getParameters());
      scrollPane = GuiUtils.createScrollPane(
            new JLabel("<html><h1>Settings</h1>"),
            new ParameterEditor(getParameters()).getEditorComponent()
      );
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            selectedDotSize,
            unselectedDotSize
      );
   }

   Preferences getPreferences() {
      return preferences;
   }

   JComponent getComponent() {
      return scrollPane;
   }
}
