package no.imr.lsss.framework.packages;

import no.imr.tools.swing.GuiUtils;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.util.Map;

public record ActionArgument(
      int modifiers,
      Map<String, Object> input
) {
   public ActionArgument() {
      this(0, Map.of());
   }

   public ActionArgument(Map<String, Object> input) {
      this(0, input);
   }

   public ActionArgument(InputEvent inputEvent) {
      this(GuiUtils.getActionEventModifiers(inputEvent), Map.of());
   }

   public ActionArgument(ActionEvent actionEvent) {
      this(actionEvent, Map.of());
   }

   public ActionArgument(ActionEvent actionEvent, Map<String, Object> input) {
      this(actionEvent.getModifiers(), input);
   }
}
