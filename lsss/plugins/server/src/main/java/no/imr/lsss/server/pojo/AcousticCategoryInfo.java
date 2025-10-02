package no.imr.lsss.server.pojo;

import no.imr.lsss.database.tables.hibernate.AcousticCategory;
import no.imr.lsss.database.util.LanguageUtils;

public final class AcousticCategoryInfo {
   public int id;
   public String initials;
   public String name;
   public boolean composite;

   public AcousticCategoryInfo(AcousticCategory acousticCategory, LanguageUtils languageUtils) {
      id = acousticCategory.getCompId().getAcousticCategory();
      initials = languageUtils.getAcCatInitials(acousticCategory);
      name = languageUtils.getAcCatName(acousticCategory);
      composite = acousticCategory.getComposite() != 0;
   }
}
