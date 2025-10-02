package no.imr.lsss.framework.config.application;

import no.imr.tools.parameter.Name;

/**
 * Represents a subdirectory in the survey directory.
 */
public record SubDir(Name mainDirParameterName, Name parameterName, String defaultRelativePath) {
   @Override
   public String toString() {
      return parameterName.persistentName() + ": " + defaultRelativePath;
   }
}
