package no.imr.korona.computation.categorization.apriori;

import no.imr.korona.computation.categorization.Category;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.data.ping.Ping;
import no.marec.lsss.api.util.GeoPoint;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PerPingAPriori {
   private final Map<Category, Float> categoryToAPriori = new HashMap<>();

   public PerPingAPriori(Configurator configurator, Ping ping) {
      GeoPoint geoPoint = ping.getPingIndex().getGeographicalPosition();
      if (geoPoint == null) {
         return;
      }
      List<Category> categories = configurator.getCategories();
      for (Category category : categories) {
         GeoAPriori geoAPriori = category.getCategoryAPriori().getGeoAPriori();
         if (geoAPriori != null) {
            categoryToAPriori.put(category, geoAPriori.getAPrioriValue(geoPoint));
         }
      }
   }

   public @Nullable Float getAPriori(Category category) {
      return categoryToAPriori.get(category);
   }
}
