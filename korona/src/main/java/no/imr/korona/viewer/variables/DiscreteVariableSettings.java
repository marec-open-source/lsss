package no.imr.korona.viewer.variables;

import no.imr.korona.color.DiscreteColorMapping;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.DiscreteCategory;
import no.imr.tools.listening.ChangeManager;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Settings for a {@link DiscreteVariable}.
 */
public final class DiscreteVariableSettings {
   private List<DiscreteCategory> categories = List.of();
   private DiscreteColorMapping discreteColorMapping = new DiscreteColorMapping(List.of());
   private final boolean[] specialCategories = new boolean[Configurator.MAX_CATEGORY_NUMBER + 1];
   private final boolean[] categoryNumberPlottable = new boolean[Configurator.MAX_CATEGORY_NUMBER + 1];

   private CategoryRemappingCache categoryRemappingCache = new CategoryRemappingCache(List.of());

   private final ChangeManager changeManager = new ChangeManager();

   public DiscreteVariableSettings() {
   }

   public ChangeManager getChangeManager() {
      return changeManager;
   }

   public DiscreteColorMapping getDiscreteColorMapping() {
      return discreteColorMapping;
   }

   public boolean isCategorySpecial(byte categoryNumber) {
      return specialCategories[categoryNumber];
   }

   public boolean[] getCategoryNumberPlottable() {
      return categoryNumberPlottable;
   }

   public boolean isPlottable(DiscreteCategory category) {
      return categoryNumberPlottable[category.getNumber()];
   }

   public void setPlottable(DiscreteCategory category, boolean plottable) {
      categoryNumberPlottable[category.getNumber()] = plottable;
      changeManager.notifyListeners();
   }

   public List<DiscreteCategory> getCategories() {
      return categories;
   }

   public CategoryRemapping getCategoryRemapping(@Nullable Cac0Datagram cac0Datagram) {
      if (cac0Datagram == null) {
         return CategoryRemapping.identity();
      }
      return categoryRemappingCache.get(cac0Datagram);
   }

   public void update(Collection<? extends DiscreteCategory> newCategories) {
      if (categories.isEmpty()) {
         Arrays.fill(categoryNumberPlottable, false);
         newCategories.forEach(category -> categoryNumberPlottable[category.getNumber()] = true);
      }

      Arrays.fill(specialCategories, false);
      newCategories.forEach(category -> {
         if (Configurator.SPECIAL_CATEGORIES.contains(category.getName())) {
            specialCategories[category.getNumber()] = true;
         }
      });

      categories = List.copyOf(newCategories);
      discreteColorMapping = DiscreteColorMapping.makeDiscreteColors(newCategories);
      categoryRemappingCache = new CategoryRemappingCache(categories);
      changeManager.notifyListeners();
   }
}
