package no.imr.lsss.framework.config.survey;

import org.dom4j.Document;
import org.dom4j.Node;
import org.jspecify.annotations.Nullable;

/**
 * For getting values from the XML inside an lsss file instead of loading it into the ConfigurationManager.
 */
public final class SurveyConfigurationXml {
   private static final String XPATH_SURVEY_CONF = "//survey/configuration/unit[@name='SurveyConfiguration']/unit[@name='SurveyConf']";

   private final Document document;

   public SurveyConfigurationXml(Document document) {
      this.document = document;
   }

   public @Nullable String getUseLocalDatabase() {
      return getSurveyParameter("UseLocalDatabase");
   }

   public @Nullable String getNation() {
      return getSurveyParameter("Nation");
   }

   public @Nullable String getPlatformId() {
      return getSurveyParameter("PlatformId");
   }

   public @Nullable String getPlatform() {
      return getSurveyParameter("Platform");
   }

   public @Nullable String getSurveyId() {
      return getSurveyParameter("SurveyId");
   }

   public @Nullable String getSurveyTitle() {
      return getSurveyParameter("Survey");
   }

   public @Nullable String getStartDate() {
      return getSurveyParameter("StartDate");
   }

   public @Nullable String getStartTime() {
      return getSurveyParameter("StartTime");
   }

   public @Nullable String getStopDate() {
      return getSurveyParameter("StopDate");
   }

   public @Nullable String getStopTime() {
      return getSurveyParameter("StopTime");
   }

   public @Nullable String getBoundaryNorth() {
      return getSurveyParameter("BoundaryNorth");
   }

   public @Nullable String getBoundarySouth() {
      return getSurveyParameter("BoundarySouth");
   }

   public @Nullable String getBoundaryWest() {
      return getSurveyParameter("BoundaryWest");
   }

   public @Nullable String getBoundaryEast() {
      return getSurveyParameter("BoundaryEast");
   }

   public @Nullable String getSurveyDescription() {
      return getSurveyParameter("Comment");
   }

   private @Nullable String getSurveyParameter(String name) {
      Node node = document.selectSingleNode(XPATH_SURVEY_CONF + "/configuration/parameters/parameter[@name='" + name + "']");
      return node != null ? node.getStringValue() : null;
   }
}
