package no.imr.lsss.framework.config.survey;

import no.imr.lsss.LSSS;
import no.imr.lsss.framework.SurveyManager;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;

/**
 * A file parameter that uses {@link SurveyManager#getReferenceDirectoryManager()}.
 */
public class SurveyFileParameter extends FileParameter {
   private final LSSS lsss;

   public SurveyFileParameter(LSSS lsss, Name name, Mode mode) {
      super(name, null, mode);

      this.lsss = lsss;
      setReferenceDirectoryManager(lsss.getSurveyManager().getReferenceDirectoryManager());
   }

   public LSSS getLSSS() {
      return lsss;
   }

   @Override
   public @Nullable Path getDefaultBrowseDirectory() {
      return lsss.getSurveyManager().getLsssReferenceDirectory().getFile();
   }
}
