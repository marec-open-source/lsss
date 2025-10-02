package no.imr.tools.parameter.gui;

import com.google.common.util.concurrent.Runnables;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.BaseValueParameter;
import no.imr.tools.parameter.Configurable;
import no.imr.tools.parameter.MultiParameter;
import no.imr.tools.parameter.ParameterCollection;
import no.imr.tools.parameter.ParameterException;
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
 * For pasting xml from clipboard into parameters.
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
      } catch (Exception e) {
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
         if (parameter == null) {
            continue;
         }
         switch (parameter) {
            case BaseValueParameter<?> baseValueParameter -> {
               try {
                  parameterWrappers.add(wrap(baseValueParameter, parameterGUIs.get(parameter), element));
               } catch (ParameterException ignore) {
                  // Ignore this parameter
               }
            }
            case MultiParameter<?> multiParameter -> {
               MultiParameterGUI multiParameterGUI = (MultiParameterGUI) parameterGUIs.get(parameter);
               List<ParameterWrapper> subParametersWrappers = findParametersWrappers(element, multiParameter.getParameters(), multiParameterGUI.getParameterGUIs());
               parameterWrappers.add(wrap(multiParameterGUI, subParametersWrappers.stream().anyMatch(ParameterWrapper::changed)));
               parameterWrappers.addAll(subParametersWrappers);
            }
            case VoidParameter __ -> {
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

   private static ParameterWrapper wrap(MultiParameterGUI parameterGUI, boolean changed) {
      return new ParameterWrapper(parameterGUI, changed, Runnables.doNothing());
   }

   private static <V> ParameterWrapper wrap(BaseValueParameter<V> parameter, ParameterGUI<?> parameterGUI, Element element) {
      V newValue = parameter.xmlToValue(element);
      return new ParameterWrapper(parameterGUI, !newValue.equals(parameter.getValue()), () -> parameter.setValue(newValue));
   }

   private record ParameterWrapper(ParameterGUI<?> parameterGUI, boolean changed, Runnable paste) {

      private void highlight() {
         parameterGUI.setHighlight(changed ? ColorUtils.PALEGREEN : Color.LIGHT_GRAY);
      }

      private void removeHighlight() {
         parameterGUI.setHighlight(null);
      }

      private void doPaste() {
         paste.run();
      }
   }
}
