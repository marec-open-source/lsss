package no.imr.lsss.database.util;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.framework.config.ConfigurationManager;

import java.util.Comparator;

/**
 * Class for methods with functionality depending on language.
 */
public final class LanguageUtils {
   private final ConfigurationManager configurationManager;

   public LanguageUtils(ConfigurationManager configurationManager) {
      this.configurationManager = configurationManager;
   }

   private boolean useEnglish() {
      return configurationManager.getAppMiscConf().useEnglish.getBooleanValue();
   }

   public String getAcCatInitials(AcousticCategory category) {
      if (useEnglish()) {
         return category.getEnglishInitials();
      } else {
         return category.getInitials();
      }
   }

   public String getAcCatName(AcousticCategory category) {
      if (useEnglish()) {
         return category.getEnglishName();
      } else {
         return category.getCommonName();
      }
   }

   public Comparator<AcousticCategory> acousticCategoryComparator() {
      if (useEnglish()) {
         return AcousticCategory::compareToEnglish;
      } else {
         return Comparator.naturalOrder();
      }
   }
}
