package no.imr.lsss.framework.config.survey.preprocessing;

import no.imr.korona.config.ConfigFileSettings;
import no.imr.lsss.LSSS;
import no.imr.lsss.framework.config.survey.SurveyFileParameter;
import no.imr.tools.parameter.Name;
import no.imr.tools.swing.SuffixFileFilter;

import javax.swing.filechooser.FileFilter;
import java.nio.file.Path;
import java.util.List;

abstract class ConfigFileSettingsParameter extends SurveyFileParameter {
   ConfigFileSettingsParameter(LSSS lsss, Name name) {
      super(lsss, name, Mode.FILE);

      subscribe(optFile -> {
         Path file = optFile.orElse(null);
         if (file != null && !exists()) {
            setFile(ConfigFileSettings.FILE_TYPE.ensureSuffix(file));
         }
      });
   }

   @Override
   public List<FileFilter> getFileFilters() {
      return List.of(new SuffixFileFilter(ConfigFileSettings.FILE_TYPE));
   }

   @Override
   public Copier getCopier() {
      return new DefaultCopier(this);
   }
}
