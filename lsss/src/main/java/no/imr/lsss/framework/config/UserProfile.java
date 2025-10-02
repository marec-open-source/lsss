package no.imr.lsss.framework.config;

/**
 * Defines different levels of user access.
 */
public enum UserProfile {
   /**
    * Can only edit configurations necessary for normal use.
    */
   NORMAL_USE("Normal use"),

   /**
    * Can edit most configurations.
    */
   SURVEY_SETUP("Survey setup"),

   /**
    * Can edit all configurations.
    */
   ADMINISTRATOR_MODE("Administrator mode");

   private final String description;

   UserProfile(String description) {
      this.description = description;
   }

   @Override
   public String toString() {
      return description;
   }
}
