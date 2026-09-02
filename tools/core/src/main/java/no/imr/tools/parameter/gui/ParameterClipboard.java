package no.imr.tools.parameter.gui;

import com.google.common.util.concurrent.Runnables;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BaseValueParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.VoidParameter;
import no.imr.tools.parameter.gui.input.MultiParameterGUI;
import no.imr.tools.parameter.gui.input.ParameterGUI;
import no.imr.tools.swing.ColorUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.awt.Color;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * For pasting XML from clipboard into parameters.
 */
final class ParameterClipboard {
   private final List<ParameterWrapper> parameterWrappers;

   private ParameterClipboard(List<ParameterWrapper> parameterWrappers) {
      this.parameterWrappers = parameterWrappers;
   }

   static @Nullable ParameterClipboard make(List<? extends BaseParameter<?>> parameters, Map<BaseParameter<?>, ParameterGUI<?>> parameterGUIs) {
      try {
         String xmlString = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
         Element root = XmlUtils.readDocument(xmlString).getRootElement();
         List<ParameterWrapper> parameterWrappers = findParametersWrappers(root, parameters, parameterGUIs);
         return new ParameterClipboard(parameterWrappers);
      } catch (Exception _) {
         return null;
      }
   }

   static void doCopy(List<? extends BaseParameter<?>> parameters) {
      Element element = new ParameterCollection(parameters).toXml();
      String xmlString = XmlUtils.toPrettyString(element);
      StringSelection stringSelection = new StringSelection(xmlString);
      Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, stringSelection);
   }

   private static List<ParameterWrapper> findParametersWrappers(Element containerElement, Collection<? extends BaseParameter<?>> parameters,
                                                                Map<? extends BaseParameter<?>, ParameterGUI<?>> parameterGUIs) {
      Map<String, BaseParameter<?>> nameToParameter = parameters.stream()
            .filter(parameter -> !(parameter instanceof VoidParameter))
            .collect(Collectors.toMap(parameter -> parameter.getName().persistentName(), Function.identity()));
      List<ParameterWrapper> parameterWrappers = new ArrayList<>();
      for (Element element : containerElement.elements()) {
         String name = Configurable.getName(element);
         BaseParameter<?> parameter = nameToParameter.get(name);
         switch (parameter) {
            case null -> {
            }
            case BaseValueParameter<?> baseValueParameter -> {
               parameterWrappers.add(wrap(baseValueParameter, parameterGUIs.get(baseValueParameter), element));
            }
            case MultiParameter<?> multiParameter -> {
               MultiParameterGUI multiParameterGUI = (MultiParameterGUI) parameterGUIs.get(multiParameter);
               List<ParameterWrapper> subParametersWrappers = findParametersWrappers(element, multiParameter.getParameters(), multiParameterGUI.getParameterGUIs());
               parameterWrappers.addAll(subParametersWrappers);
               // Add main parameter last, to avoid the last subparameter to override the highlight.
               ParameterWrapperState worstState = subParametersWrappers.stream()
                     .map(ParameterWrapper::state)
                     .reduce(ParameterWrapperState.UNCHANGED, ParameterWrapperState::worstOf);
               parameterWrappers.add(wrap(multiParameterGUI, worstState));
            }
            case VoidParameter _ -> {
               // Cannot happen, filtered above.
            }
         }
      }
      return parameterWrappers;
   }

   void highlight() {
      parameterWrappers.forEach(ParameterWrapper::highlight);
   }

   void removeHighlight() {
      parameterWrappers.forEach(ParameterWrapper::removeHighlight);
   }

   void doPaste() {
      parameterWrappers.forEach(ParameterWrapper::doPaste);
      removeHighlight();
   }

   private static ParameterWrapper wrap(ParameterGUI<?> parameterGUI, ParameterWrapperState state) {
      return new ParameterWrapper(parameterGUI, state, Runnables.doNothing());
   }

   private static <V> ParameterWrapper wrap(BaseValueParameter<V> parameter, ParameterGUI<?> parameterGUI, Element element) {
      V newValue;
      try {
         newValue = parameter.xmlToValue(element);
         if (!parameter.getConstraint().isValid(newValue)) {
            return wrap(parameterGUI, ParameterWrapperState.ERROR);
         }
      } catch (Exception _) {
         return wrap(parameterGUI, ParameterWrapperState.ERROR);
      }
      ParameterWrapperState state = newValue.equals(parameter.getValue())
            ? ParameterWrapperState.UNCHANGED
            : ParameterWrapperState.CHANGED;
      return new ParameterWrapper(parameterGUI, state, () -> parameter.setValue(newValue));
   }

   private enum ParameterWrapperState {
      UNCHANGED, CHANGED, ERROR;

      private static ParameterWrapperState worstOf(ParameterWrapperState a, ParameterWrapperState b) {
         return a.ordinal() > b.ordinal() ? a : b;
      }
   }

   private record ParameterWrapper(ParameterGUI<?> parameterGUI, ParameterWrapperState state, Runnable paste) {

      private void highlight() {
         parameterGUI.setHighlight(switch (state) {
            case UNCHANGED -> Color.LIGHT_GRAY;
            case CHANGED -> ColorUtils.PALEGREEN;
            case ERROR -> ColorUtils.TOMATO;
         });
      }

      private void removeHighlight() {
         parameterGUI.setHighlight(null);
      }

      private void doPaste() {
         paste.run();
      }
   }
}
