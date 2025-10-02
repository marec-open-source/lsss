package no.imr.korona.apps.relay;

import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BooleanParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.IntParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.Unit;
import no.imr.tools.parameter.gui.ConfigurableGUIDialog;
import no.imr.tools.parameter.gui.ParameterEditor;
import no.marec.lsss.api.util.parameters.ValueConstraints;

import java.awt.Component;
import java.util.Collection;
import java.util.List;

/**
 * Settings for {@link KoronaRelay}.
 */
public final class KoronaRelaySettings extends Configurable implements ParameterContainer {
   public final BooleanParameter automaticMultiprocessing = new BooleanParameter(
         new Name("AutomaticMultiprocessing", "Automatic multiprocessing"),
         true,
         "Using available number of processors");

   public final IntParameter processCount = new IntParameter(
         new Name("ProcessCount", "Process count"),
         Runtime.getRuntime().availableProcessors(), Unit.COUNT, ValueConstraints.gte(1),
         "Number of files to process simultaneously");

   public final BooleanParameter lowPriority = new BooleanParameter(
         new Name("LowPriority", "Low priority"),
         true,
         "Assign low priority to processing threads");

   KoronaRelaySettings() {
      super(new Name("KoronaRelaySettings", "KORONA relay settings"));

      automaticMultiprocessing.addListenerAndNotify(auto -> processCount.setEnabled(!auto));
   }

   void showEditor(Component referenceComponent, boolean editable) {
      ParameterEditor parameterEditor = new ParameterEditor(getParameters());
      parameterEditor.getGUIConfig().setParameterEnabledDecider(__ -> editable);
      new ConfigurableGUIDialog(referenceComponent, getName().displayName(), this)
            .setCloseOnOk(parameterEditor::commitEdits)
            .setGUI(parameterEditor.getEditorComponent())
            .show();
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            automaticMultiprocessing,
            processCount,
            lowPriority
      );
   }

   @Override
   public Collection<? extends Configurable> getSubConfigurables() {
      return getParameters();
   }

   public int getProcessCount() {
      if (automaticMultiprocessing.getBooleanValue()) {
         return Runtime.getRuntime().availableProcessors();
      } else {
         return processCount.getIntValue();
      }
   }
}
