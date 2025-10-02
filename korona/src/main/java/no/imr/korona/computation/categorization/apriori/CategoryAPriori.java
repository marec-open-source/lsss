package no.imr.korona.computation.categorization.apriori;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.jspecify.annotations.Nullable;

public final class CategoryAPriori {
   public static final String XML_A_PRIORI = "apriori";

   private @Nullable GeoAPriori geoAPriori;

   public CategoryAPriori() {
   }

   public CategoryAPriori(Element element) {
      Element geoElement = element.element(GeoAPriori.XML_GEO);
      if (geoElement != null) {
         geoAPriori = new GeoAPriori(geoElement);
      }
   }

   public @Nullable Element toXml() {
      Element aprioriElement = DocumentHelper.createElement(XML_A_PRIORI);
      if (geoAPriori != null) {
         aprioriElement.add(geoAPriori.toXml());
      }
      return aprioriElement.elements().isEmpty() ? null : aprioriElement;
   }

   public @Nullable GeoAPriori getGeoAPriori() {
      return geoAPriori;
   }

   public void setGeoAPriori(@Nullable GeoAPriori geoAPriori) {
      this.geoAPriori = geoAPriori;
   }
}
