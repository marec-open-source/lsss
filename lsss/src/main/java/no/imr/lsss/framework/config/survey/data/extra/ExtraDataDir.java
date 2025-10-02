package no.imr.lsss.framework.config.survey.data.extra;

import no.imr.lsss.LSSS;
import no.imr.tools.parameter.BaseParameter;
import no.imr.tools.parameter.FileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.parameter.ParameterContainer;
import no.imr.tools.parameter.StringParameter;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.List;

public final class ExtraDataDir implements ParameterContainer {
   private final LSSS lsss;
   public final FileParameter dataDir = new FileParameter(
         new Name("Dir", "Directory (source)"),
         null, FileParameter.Mode.FILE_OR_DIRECTORY) {
      @Override
      public @Nullable Path getDefaultBrowseDirectory() {
         return lsss.getSurveyManager().getSurveyReferenceDirectory().getFile();
      }
   };
   public final StringParameter relativePath = new StringParameter(
         new Name("RelativePath", "Relative path (destination)"));

   ExtraDataDir(LSSS lsss) {
      this.lsss = lsss;
      dataDir.setReferenceDirectoryManager(lsss.getSurveyManager().getReferenceDirectoryManager());
   }

   @Override
   public List<? extends BaseParameter<?>> getParameters() {
      return List.of(
            dataDir,
            relativePath
      );
   }

   boolean isBlank() {
      return dataDir.getFile() == null && relativePath.getValue().isBlank();
   }
}
