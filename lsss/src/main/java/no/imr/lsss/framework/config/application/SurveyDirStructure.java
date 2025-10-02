package no.imr.lsss.framework.config.application;

import no.imr.lsss.database.tables.hibernate.Platform;
import no.imr.lsss.database.tables.hibernate.Survey;
import no.imr.tools.Utils;
import no.imr.tools.io.FileUtils;
import no.imr.tools.xml.XmlUtils;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class SurveyDirStructure {
   private String format = "S%d_P%s[%d]";
   private @Nullable String removeRegex;
   private @Nullable String letterCase;
   private final Map<String, String> relativePaths = new HashMap<>();

   SurveyDirStructure(Collection<SubDir> subDirs) {
      for (SubDir subDir : subDirs) {
         String id = subDir.parameterName().persistentName();
         String path = subDir.defaultRelativePath();
         relativePaths.put(id, path);
      }
   }

   void read(Path file) throws IOException {
      Element element = XmlUtils.readDocument(file).getRootElement();

      String formatAttribute = element.attributeValue("format");
      if (formatAttribute != null) {
         format = formatAttribute;
      }
      removeRegex = element.attributeValue("removeRegex");
      letterCase = element.attributeValue("case");

      add(element);
   }

   private void add(Element element) {
      for (Element dirElement : element.elements()) {
         String id = dirElement.attributeValue("id");
         String path = FileUtils.toNativeSeparatorChar(dirElement.attributeValue("path"));
         relativePaths.put(id, path);
      }
   }

   public String getRelativePath(SubDir subDir) {
      return relativePaths.get(subDir.parameterName().persistentName());
   }

   public String getSurveyDirName(Survey survey) {
      int surveyId = survey.getCompId().getSurvey();
      Platform platform = survey.getPlatform();
      String platformName = platform.findPlatformName(survey);
      short platformId = platform.getCompId().getPlatform();

      String dirName = Utils.format(format, surveyId, platformName, platformId);

      if (removeRegex != null) {
         dirName = dirName.replaceAll(removeRegex, "");
      }
      if ("upper".equalsIgnoreCase(letterCase)) {
         dirName = dirName.toUpperCase(Locale.ENGLISH);
      }
      if ("lower".equalsIgnoreCase(letterCase)) {
         dirName = dirName.toLowerCase(Locale.ENGLISH);
      }

      return dirName;
   }
}
