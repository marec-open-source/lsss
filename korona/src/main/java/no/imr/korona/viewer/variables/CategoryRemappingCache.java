package no.imr.korona.viewer.variables;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import no.imr.korona.computation.categorization.Configurator;
import no.imr.korona.data.datagrams.Cac0Datagram;
import no.imr.korona.data.datagrams.DiscreteCategory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

final class CategoryRemappingCache {
   private final LoadingCache<Cac0Datagram, CategoryRemapping> cache = CacheBuilder.newBuilder()
         .build(CacheLoader.from(this::computeCategoryRemapping));

   private final Map<String, DiscreteCategory> nameToCategory;

   CategoryRemappingCache(List<DiscreteCategory> categories) {
      nameToCategory = categories.stream().collect(Collectors.toUnmodifiableMap(DiscreteCategory::getName, Function.identity()));
   }

   CategoryRemapping get(Cac0Datagram cac0Datagram) {
      return cache.getUnchecked(cac0Datagram);
   }

   private CategoryRemapping computeCategoryRemapping(Cac0Datagram cac0Datagram) {
      if (nameToCategory.isEmpty()) {
         return CategoryRemapping.identity();
      }
      List<Cac0Datagram.Category> cac0Categories = cac0Datagram.getCategories();
      return isCompatible(cac0Categories) ? CategoryRemapping.identity() : computeMapping(cac0Categories);
   }

   private boolean isCompatible(List<Cac0Datagram.Category> cac0Categories) {
      return cac0Categories.stream().allMatch(cac0Category -> {
         DiscreteCategory category = nameToCategory.get(cac0Category.getName());
         return category != null && cac0Category.getNumber() == category.getNumber();
      });
   }

   private CategoryRemapping computeMapping(List<Cac0Datagram.Category> cac0Categories) {
      int maxCategoryNumber = cac0Categories.stream().mapToInt(Cac0Datagram.Category::getNumber).max().orElse(0);
      byte[] map = new byte[maxCategoryNumber + 1];
      Arrays.fill(map, nameToCategory.get(Configurator.UNKNOWN_CATEGORY_NAME).getNumber());
      cac0Categories.forEach(cac0Category -> {
         DiscreteCategory category = nameToCategory.get(cac0Category.getName());
         if (category != null) {
            map[cac0Category.getNumber()] = category.getNumber();
         }
      });
      return category -> map[category];
   }
}
