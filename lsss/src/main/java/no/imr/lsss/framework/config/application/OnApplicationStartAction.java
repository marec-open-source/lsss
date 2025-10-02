package no.imr.lsss.framework.config.application;

import no.marec.lsss.api.util.parameters.ObjectParameterValue;

public enum OnApplicationStartAction implements ObjectParameterValue {
   DO_NOTHING("Do nothing"),
   OPEN_LAST_SURVEY("Open last survey"),
   SHOW_OPEN_SURVEY_DIALOG("Show open survey dialog");

   private final String label;

   OnApplicationStartAction(String label) {
      this.label = label;
   }

   @Override
   public String getDisplayLabel() {
      return label;
   }
}
