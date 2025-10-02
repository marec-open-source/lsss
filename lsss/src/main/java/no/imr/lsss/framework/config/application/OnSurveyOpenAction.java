package no.imr.lsss.framework.config.application;

import no.marec.lsss.api.util.parameters.ObjectParameterValue;

public enum OnSurveyOpenAction implements ObjectParameterValue {
   DO_NOTHING("Do nothing"),
   OPEN_FILES("Open selected files"),
   OPEN_FILES_AND_ZOOM("Open selected files and zoom to last location"),
   SHOW_CONFIG_DIALOG("Show configuration dialog");

   private final String label;

   OnSurveyOpenAction(String label) {
      this.label = label;
   }

   @Override
   public String getDisplayLabel() {
      return label;
   }
}
